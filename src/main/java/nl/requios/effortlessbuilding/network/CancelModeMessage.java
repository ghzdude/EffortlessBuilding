package nl.requios.effortlessbuilding.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.BuildModes;

import java.util.function.Supplier;

/**
 * Sends a message to the server indicating that a buildmode needs to be canceled for a player
 */
public class CancelModeMessage {

	public static void encode(CancelModeMessage message, FriendlyByteBuf buf) {
	}

	public static CancelModeMessage decode(FriendlyByteBuf buf) {
		return new CancelModeMessage();
	}

	public static class Handler {
		public static void handle(CancelModeMessage message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);

				BuildModes.initializeMode(player);
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
