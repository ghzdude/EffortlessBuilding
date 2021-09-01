package nl.requios.effortlessbuilding;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.network.PacketDistributor;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.BuildModifiers;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedo;
import nl.requios.effortlessbuilding.capability.ModeCapabilityManager;
import nl.requios.effortlessbuilding.capability.ModifierCapabilityManager;
import nl.requios.effortlessbuilding.helper.ReachHelper;
import nl.requios.effortlessbuilding.helper.SurvivalHelper;
import nl.requios.effortlessbuilding.network.AddUndoMessage;
import nl.requios.effortlessbuilding.network.ClearUndoMessage;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.RequestLookAtMessage;

import java.util.List;

@Mod.EventBusSubscriber
public class EventHandler {

	@SubscribeEvent
	public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof FakePlayer) return;
		if (event.getObject() instanceof PlayerEntity) {
			event.addCapability(new ResourceLocation(EffortlessBuilding.MODID, "build_modifier"), new ModifierCapabilityManager.Provider());
			event.addCapability(new ResourceLocation(EffortlessBuilding.MODID, "build_mode"), new ModeCapabilityManager.Provider());
		}
	}

	//TODO 1.13 config
//    @SubscribeEvent
//    public static void onConfigChangedEvent(ConfigChangedEvent.OnConfigChangedEvent event)
//    {
//        if (event.getModID().equals(EffortlessBuilding.MODID))
//        {
//            ConfigManager.sync(EffortlessBuilding.MODID, Config.Type.INSTANCE);
//        }
//    }

//    @SubscribeEvent
//    public static void onServerTick(TickEvent.ServerTickEvent event) {
//
//    }

	@SubscribeEvent
	//Only called serverside (except with lilypads...)
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (event.getWorld().isClientSide()) return;

		if (!(event.getEntity() instanceof PlayerEntity)) return;

		if (event.getEntity() instanceof FakePlayer) return;

		//Cancel event if necessary
		ServerPlayerEntity player = ((ServerPlayerEntity) event.getEntity());
		BuildModes.BuildModeEnum buildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();
		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);

		if (buildMode != BuildModes.BuildModeEnum.NORMAL) {
			event.setCanceled(true);
		} else if (modifierSettings.doQuickReplace()) {
			//Cancel event and send message if QuickReplace
			event.setCanceled(true);
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new RequestLookAtMessage(true));
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new AddUndoMessage(event.getPos(), event.getBlockSnapshot().getReplacedBlock(), event.getState()));
		} else {
			//NORMAL mode, let vanilla handle block placing
			//But modifiers should still work

			//Send message to client, which sends message back with raytrace info
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new RequestLookAtMessage(false));
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new AddUndoMessage(event.getPos(), event.getBlockSnapshot().getReplacedBlock(), event.getState()));
		}

//        Stat<ResourceLocation> blocksPlacedStat = StatList.CUSTOM.get(new ResourceLocation(EffortlessBuilding.MODID, "blocks_placed"));
//        player.getStats().increment(player, blocksPlacedStat, 1);
//
//        System.out.println(player.getStats().getValue(blocksPlacedStat));
	}

	@SubscribeEvent
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (event.getWorld().isClientSide()) return;

		if (event.getPlayer() instanceof FakePlayer) return;

		//Cancel event if necessary
		//If cant break far then dont cancel event ever
		BuildModes.BuildModeEnum buildMode = ModeSettingsManager.getModeSettings(event.getPlayer()).getBuildMode();
		if (buildMode != BuildModes.BuildModeEnum.NORMAL && ReachHelper.canBreakFar(event.getPlayer())) {
			event.setCanceled(true);
		} else {
			//NORMAL mode, let vanilla handle block breaking
			//But modifiers and QuickReplace should still work
			//Dont break the original block yourself, otherwise Tinkers Hammer and Veinminer won't work
			BuildModes.onBlockBroken(event.getPlayer(), event.getPos(), false);

			//Add to undo stack in client
			if (event.getPlayer() instanceof ServerPlayerEntity && event.getState() != null && event.getPos() != null) {
				PacketDistributor.PacketTarget packetTarget = PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) event.getPlayer());
				if (packetTarget != null)
					PacketHandler.INSTANCE.send(packetTarget, new AddUndoMessage(event.getPos(), event.getState(), Blocks.AIR.defaultBlockState()));
			}
		}
	}

	@SubscribeEvent
	public static void breakSpeed(PlayerEvent.BreakSpeed event) {
		//Disable if config says so
		if (!BuildConfig.survivalBalancers.increasedMiningTime.get()) return;

		if (event.getPlayer() instanceof FakePlayer) return;

		PlayerEntity player = event.getPlayer();
		World world = player.level;
		BlockPos pos = event.getPos();

		//EffortlessBuilding.log(player, String.valueOf(event.getNewSpeed()));

		float originalBlockHardness = event.getState().getDestroySpeed(world, pos);
		if (originalBlockHardness < 0) return; //Dont break bedrock
		float totalBlockHardness = 0;
		//get coordinates
		List<BlockPos> coordinates = BuildModifiers.findCoordinates(player, pos);
		for (int i = 1; i < coordinates.size(); i++) {
			BlockPos coordinate = coordinates.get(i);
			//get existing blockstates at those coordinates
			BlockState blockState = world.getBlockState(coordinate);
			//add hardness for each blockstate, if can break
			if (SurvivalHelper.canBreak(world, player, coordinate))
				totalBlockHardness += blockState.getDestroySpeed(world, coordinate);
		}

		//Grabbing percentage from config
		float percentage = (float) BuildConfig.survivalBalancers.miningTimePercentage.get() / 100;
		totalBlockHardness *= percentage;
		totalBlockHardness += originalBlockHardness;

		float newSpeed = event.getOriginalSpeed() / totalBlockHardness * originalBlockHardness;
		if (Float.isNaN(newSpeed) || newSpeed == 0f) newSpeed = 1f;
		event.setNewSpeed(newSpeed);

		//EffortlessBuilding.log(player, String.valueOf(event.getNewSpeed()));
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getPlayer() instanceof FakePlayer) return;
		PlayerEntity player = event.getPlayer();
		ModifierSettingsManager.handleNewPlayer(player);
		ModeSettingsManager.handleNewPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getPlayer() instanceof FakePlayer) return;
		PlayerEntity player = event.getPlayer();
		if (player.getCommandSenderWorld().isClientSide) return;

		UndoRedo.clear(player);
		PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new ClearUndoMessage());
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getPlayer() instanceof FakePlayer) return;
		PlayerEntity player = event.getPlayer();
		ModifierSettingsManager.handleNewPlayer(player);
		ModeSettingsManager.handleNewPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getPlayer() instanceof FakePlayer) return;
		PlayerEntity player = event.getPlayer();
		if (player.getCommandSenderWorld().isClientSide) return;

		//Set build mode to normal
		ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
		modeSettings.setBuildMode(BuildModes.BuildModeEnum.NORMAL);
		ModeSettingsManager.setModeSettings(player, modeSettings);

		//Disable modifiers
		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
		modifierSettings.getMirrorSettings().enabled = false;
		modifierSettings.getRadialMirrorSettings().enabled = false;
		modifierSettings.getArraySettings().enabled = false;
		ModifierSettingsManager.setModifierSettings(player, modifierSettings);

		ModifierSettingsManager.handleNewPlayer(player);
		ModeSettingsManager.handleNewPlayer(player);

		UndoRedo.clear(player);
		PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayerEntity) player), new ClearUndoMessage());
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (event.getPlayer() instanceof FakePlayer) return;
		//Attach capabilities on death, otherwise crash
		PlayerEntity oldPlayer = event.getOriginal();
		oldPlayer.revive();

		PlayerEntity newPlayer = event.getPlayer();
		ModifierSettingsManager.setModifierSettings(newPlayer, ModifierSettingsManager.getModifierSettings(oldPlayer));
		ModeSettingsManager.setModeSettings(newPlayer, ModeSettingsManager.getModeSettings(oldPlayer));
	}
}
