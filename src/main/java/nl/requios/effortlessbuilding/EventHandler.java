package nl.requios.effortlessbuilding;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.BuildModifiers;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedo;
import nl.requios.effortlessbuilding.capability.ModeCapabilityManager;
import nl.requios.effortlessbuilding.capability.ModifierCapabilityManager;
import nl.requios.effortlessbuilding.command.CommandReach;
import nl.requios.effortlessbuilding.helper.ReachHelper;
import nl.requios.effortlessbuilding.helper.SurvivalHelper;
import nl.requios.effortlessbuilding.network.AddUndoMessage;
import nl.requios.effortlessbuilding.network.ClearUndoMessage;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.RequestLookAtMessage;

import java.util.List;

@Mod.EventBusSubscriber(modid = EffortlessBuilding.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {

	@SubscribeEvent
	public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof FakePlayer) return;
		if (event.getObject() instanceof Player) {
			event.addCapability(new ResourceLocation(EffortlessBuilding.MODID, "build_modifier"), new ModifierCapabilityManager.Provider());
			event.addCapability(new ResourceLocation(EffortlessBuilding.MODID, "build_mode"), new ModeCapabilityManager.Provider());
		}
	}

	@SubscribeEvent
	public void onServerStarting(ServerStartingEvent event) {
		CommandReach.register(event.getServer().getCommands().getDispatcher());
	}

	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (event.getWorld().isClientSide()) return;

		if (!(event.getEntity() instanceof Player)) return;

		if (event.getEntity() instanceof FakePlayer) return;

		//Cancel event if necessary
		ServerPlayer player = ((ServerPlayer) event.getEntity());
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
			if (event.getPlayer() instanceof ServerPlayer && event.getState() != null && event.getPos() != null) {
				PacketDistributor.PacketTarget packetTarget = PacketDistributor.PLAYER.with(() -> (ServerPlayer) event.getPlayer());
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

		Player player = event.getPlayer();
		Level world = player.level;
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
		Player player = event.getPlayer();
		ModifierSettingsManager.handleNewPlayer(player);
		ModeSettingsManager.handleNewPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getPlayer() instanceof FakePlayer) return;
		Player player = event.getPlayer();
		if (player.getCommandSenderWorld().isClientSide) return;

		UndoRedo.clear(player);
		PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ClearUndoMessage());
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getPlayer() instanceof FakePlayer) return;
		Player player = event.getPlayer();
		ModifierSettingsManager.handleNewPlayer(player);
		ModeSettingsManager.handleNewPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getPlayer() instanceof FakePlayer) return;
		Player player = event.getPlayer();
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
		PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ClearUndoMessage());
	}

	@SubscribeEvent
	public static void onPlayerClone(PlayerEvent.Clone event) {
		if (event.getPlayer() instanceof FakePlayer) return;
		//Attach capabilities on death, otherwise crash
		Player oldPlayer = event.getOriginal();
		oldPlayer.revive();

		Player newPlayer = event.getPlayer();
		ModifierSettingsManager.setModifierSettings(newPlayer, ModifierSettingsManager.getModifierSettings(oldPlayer));
		ModeSettingsManager.setModeSettings(newPlayer, ModeSettingsManager.getModeSettings(oldPlayer));
	}
}
