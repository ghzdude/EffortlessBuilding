package nl.requios.effortlessbuilding.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;

import java.util.function.Supplier;

/***
 * Sends a message to the client indicating that a block has been placed.
 * Necessary because Forge's onBlockPlaced event is only called on the server.
 */
public class OnBlockPlacedMessage {

	public OnBlockPlacedMessage() {
	}

	public static void encode(OnBlockPlacedMessage message, FriendlyByteBuf buf) {

	}

	public static OnBlockPlacedMessage decode(FriendlyByteBuf buf) {
		return new OnBlockPlacedMessage();
	}

	public static class Handler {
		public static void handle(OnBlockPlacedMessage message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
					//Received clientside
					DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHandler.handle(message, ctx));
				}
			});
			ctx.get().setPacketHandled(true);
		}
	}

	@OnlyIn(Dist.CLIENT)
	public static class ClientHandler {
		public static void handle(OnBlockPlacedMessage message, Supplier<NetworkEvent.Context> ctx) {

			EffortlessBuildingClient.BUILDER_CHAIN.onRightClick();
		}
	}
}
