package nl.requios.effortlessbuilding;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.network.PacketDistributor;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedo;
import nl.requios.effortlessbuilding.capability.ModeCapabilityManager;
import nl.requios.effortlessbuilding.capability.ModifierCapabilityManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.helper.ReachHelper;
import nl.requios.effortlessbuilding.network.AddUndoMessage;
import nl.requios.effortlessbuilding.network.ClearUndoMessage;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.RequestLookAtMessage;

@EventBusSubscriber
public class CommonEvents {

	//Mod Bus Events
	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class ModBusEvents {

		@SubscribeEvent
		public void registerCapabilities(RegisterCapabilitiesEvent event){
			event.register(ModifierCapabilityManager.IModifierCapability.class);
			event.register(ModeCapabilityManager.IModeCapability.class);
		}
	}

	@SubscribeEvent
	public static void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof FakePlayer) return;
		if (event.getObject() instanceof Player) {
			event.addCapability(new ResourceLocation(EffortlessBuilding.MODID, "build_modifier"), new ModifierCapabilityManager.Provider());
			event.addCapability(new ResourceLocation(EffortlessBuilding.MODID, "build_mode"), new ModeCapabilityManager.Provider());
		}
	}

	@SubscribeEvent
	public static void onTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;

		EffortlessBuilding.DELAYED_BLOCK_PLACER.tick();
	}

	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (event.getLevel().isClientSide()) return;

		if (!(event.getEntity() instanceof Player)) return;

		if (event.getEntity() instanceof FakePlayer) return;

		ServerPlayer player = ((ServerPlayer) event.getEntity());
		BuildModes.BuildModeEnum buildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();
		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);

		if (buildMode != BuildModes.BuildModeEnum.DISABLED) {

			//Only cancel if itemblock in hand
			//Fixed issue with e.g. Create Wrench shift-rightclick disassembling being cancelled.
			if (isPlayerHoldingBlock(player)) {
				event.setCanceled(true);
			}

		} else if (modifierSettings.doQuickReplace()) {
			//Cancel event and send message if QuickReplace
			if (isPlayerHoldingBlock(player)) {
				event.setCanceled(true);
			}
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new RequestLookAtMessage(true));
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new AddUndoMessage(event.getPos(), event.getBlockSnapshot().getReplacedBlock(), event.getState()));
		} else {
			//NORMAL mode, let vanilla handle block placing
			//But modifiers should still work

			//Send message to client, which sends message back with raytrace info
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new RequestLookAtMessage(false));
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new AddUndoMessage(event.getPos(), event.getBlockSnapshot().getReplacedBlock(), event.getState()));
		}
	}

	@SubscribeEvent
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (event.getLevel().isClientSide()) return;

		if (event.getPlayer() instanceof FakePlayer) return;

		//Cancel event if necessary
		//If cant break far then dont cancel event ever
		BuildModes.BuildModeEnum buildMode = ModeSettingsManager.getModeSettings(event.getPlayer()).getBuildMode();
		if (buildMode != BuildModes.BuildModeEnum.DISABLED && ReachHelper.canBreakFar(event.getPlayer())) {
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

	private static boolean isPlayerHoldingBlock(Player player) {
		ItemStack currentItemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
		return currentItemStack.getItem() instanceof BlockItem ||
				(CompatHelper.isItemBlockProxy(currentItemStack) && !player.isShiftKeyDown());
	}

	@SubscribeEvent
	public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		ModifierSettingsManager.handleNewPlayer(player);
		ModeSettingsManager.handleNewPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		if (player.getCommandSenderWorld().isClientSide) return;

		UndoRedo.clear(player);
		PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new ClearUndoMessage());
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		ModifierSettingsManager.handleNewPlayer(player);
		ModeSettingsManager.handleNewPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		if (player.getCommandSenderWorld().isClientSide) return;

		//Set build mode to normal
		ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
		modeSettings.setBuildMode(BuildModes.BuildModeEnum.DISABLED);
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
		if (event.getEntity() instanceof FakePlayer) return;
		//Attach capabilities on death, otherwise crash
		Player oldPlayer = event.getOriginal();
		oldPlayer.revive();

		Player newPlayer = event.getEntity();
		ModifierSettingsManager.setModifierSettings(newPlayer, ModifierSettingsManager.getModifierSettings(oldPlayer));
		ModeSettingsManager.setModeSettings(newPlayer, ModeSettingsManager.getModeSettings(oldPlayer));
	}


}
