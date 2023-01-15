package nl.requios.effortlessbuilding.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.ClientEvents;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.proxy.ClientProxy;
import nl.requios.effortlessbuilding.render.BlockPreviewRenderer;

import java.util.function.Supplier;

/***
 * Sends a message to the client asking for its lookat (objectmouseover) data.
 * This is then sent back with a BlockPlacedMessage.
 */
public class RequestLookAtMessage {
	private final boolean placeStartPos;

	public RequestLookAtMessage() {
		placeStartPos = false;
	}

	public RequestLookAtMessage(boolean placeStartPos) {
		this.placeStartPos = placeStartPos;
	}

	public static void encode(RequestLookAtMessage message, FriendlyByteBuf buf) {
		buf.writeBoolean(message.placeStartPos);
	}

	public static RequestLookAtMessage decode(FriendlyByteBuf buf) {
		boolean placeStartPos = buf.readBoolean();
		return new RequestLookAtMessage(placeStartPos);
	}

	public boolean getPlaceStartPos() {
		return placeStartPos;
	}

	public static class Handler {
		public static void handle(RequestLookAtMessage message, Supplier<NetworkEvent.Context> ctx) {
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
		public static void handle(RequestLookAtMessage message, Supplier<NetworkEvent.Context> ctx) {
			//Send back your info
			Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);

			//Prevent double placing in normal mode with placeStartPos false
			//Unless QuickReplace is on, then we do need to place start pos.
			if (ClientEvents.previousLookAt.getType() == HitResult.Type.BLOCK) {
				PacketHandler.INSTANCE.sendToServer(new BlockPlacedMessage((BlockHitResult) ClientEvents.previousLookAt, message.getPlaceStartPos()));
			} else {
				PacketHandler.INSTANCE.sendToServer(new BlockPlacedMessage());
			}
		}
	}
}
