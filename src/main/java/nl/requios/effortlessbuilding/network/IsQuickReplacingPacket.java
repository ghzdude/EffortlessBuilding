package nl.requios.effortlessbuilding.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.create.foundation.networking.SimplePacketBase;
import nl.requios.effortlessbuilding.systems.ServerBuildState;

import java.util.function.Supplier;

/**
 * Shares mode settings (see ModeSettingsManager) between server and client
 */
public class IsQuickReplacingPacket {
	private boolean isQuickReplacing;

	public IsQuickReplacingPacket() {
	}

	public IsQuickReplacingPacket(boolean isQuickReplacing) {
		this.isQuickReplacing = isQuickReplacing;
	}

	public static void encode(IsQuickReplacingPacket message, FriendlyByteBuf buf) {
		buf.writeBoolean(message.isQuickReplacing);
	}

	public static IsQuickReplacingPacket decode(FriendlyByteBuf buf) {
		return new IsQuickReplacingPacket(buf.readBoolean());
	}

	public static class Handler {
		public static void handle(IsQuickReplacingPacket message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				ServerBuildState.setIsQuickReplacing(ctx.get().getSender(), message.isQuickReplacing);
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
