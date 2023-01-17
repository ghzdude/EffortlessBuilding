package nl.requios.effortlessbuilding.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedo;

import java.util.function.Supplier;

/***
 * Sends a message to the client asking to clear the undo and redo stacks.
 */
public class ClearUndoMessage {

	public ClearUndoMessage() {
	}

	public static void encode(ClearUndoMessage message, FriendlyByteBuf buf) {

	}

	public static ClearUndoMessage decode(FriendlyByteBuf buf) {
		return new ClearUndoMessage();
	}

	public static class Handler {
		public static void handle(ClearUndoMessage message, Supplier<NetworkEvent.Context> ctx) {
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
		public static void handle(ClearUndoMessage message, Supplier<NetworkEvent.Context> ctx) {
			Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);

			//Add to undo stack clientside
			UndoRedo.clear(player);
		}
	}
}
