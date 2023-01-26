package nl.requios.effortlessbuilding;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import nl.requios.effortlessbuilding.systems.UndoRedo;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.systems.ServerBuildState;
import nl.requios.effortlessbuilding.utilities.ReachHelper;

@EventBusSubscriber
public class CommonEvents {

	//Mod Bus Events
	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class ModBusEvents {


	}

	@SubscribeEvent
	public static void onTick(TickEvent.LevelTickEvent event) {
		if (event.phase != TickEvent.Phase.START) return;

		EffortlessBuilding.DELAYED_BLOCK_PLACER.tick();
	}

	//Cancel event if necessary. Nothing more, rest is handled on mouseclick
	@SubscribeEvent
	public static void onBlockPlaced(BlockEvent.EntityPlaceEvent event) {
		if (event.getLevel().isClientSide()) return; //Never called clientside anyway, but just to be sure
		if (!(event.getEntity() instanceof Player player)) return;
		if (event.getEntity() instanceof FakePlayer) return;

		//Don't cancel event if our custom logic is breaking blocks
		if (EffortlessBuilding.SERVER_BLOCK_PLACER.isPlacingOrBreakingBlocks()) return;

		if (!ServerBuildState.isLikeVanilla(player)) {

			//Only cancel if itemblock in hand
			//Fixed issue with e.g. Create Wrench shift-rightclick disassembling being cancelled.
			if (isPlayerHoldingBlock(player)) {
				event.setCanceled(true);
			}
		}
	}

	//Cancel event if necessary. Nothing more, rest is handled on mouseclick
	@SubscribeEvent
	public static void onBlockBroken(BlockEvent.BreakEvent event) {
		if (event.getLevel().isClientSide()) return;
		Player player = event.getPlayer();
		if (player instanceof FakePlayer) return;

		//Don't cancel event if our custom logic is breaking blocks
		if (EffortlessBuilding.SERVER_BLOCK_PLACER.isPlacingOrBreakingBlocks()) return;

		if (!ServerBuildState.isLikeVanilla(player) && ReachHelper.canBreakFar(player)) {
			event.setCanceled(true);
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
		if (player.getCommandSenderWorld().isClientSide) {
			EffortlessBuilding.log("PlayerLoggedInEvent triggers on client side");
			return;
		}

		ServerBuildState.handleNewPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		if (player.getCommandSenderWorld().isClientSide) {
			EffortlessBuilding.log("PlayerLoggedOutEvent triggers on client side");
			return;
		}

		UndoRedo.clear(player);
	}

	@SubscribeEvent
	public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		if (player.getCommandSenderWorld().isClientSide) {
			EffortlessBuilding.log("PlayerRespawnEvent triggers on client side");
			return;
		}

		//TODO check if this is needed
		ServerBuildState.handleNewPlayer(player);
	}

	@SubscribeEvent
	public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
		if (event.getEntity() instanceof FakePlayer) return;
		Player player = event.getEntity();
		if (player.getCommandSenderWorld().isClientSide) {
			EffortlessBuilding.log("PlayerChangedDimensionEvent triggers on client side");
			return;
		}

		//Undo redo has no dimension data, so clear it
		UndoRedo.clear(player);

		//TODO disable build mode and modifiers?
	}
}