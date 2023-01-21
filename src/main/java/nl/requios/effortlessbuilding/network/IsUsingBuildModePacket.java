package nl.requios.effortlessbuilding.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

import nl.requios.effortlessbuilding.systems.ServerBuildState;

/**
 * Shares mode settings (see ModeSettingsManager) between server and client
 */
public class IsUsingBuildModePacket {
	private boolean isUsingBuildMode;

	public IsUsingBuildModePacket() {
	}

	public IsUsingBuildModePacket(boolean isUsingBuildMode) {
		this.isUsingBuildMode = isUsingBuildMode;
	}

	public static void encode(IsUsingBuildModePacket message, FriendlyByteBuf buf) {
		buf.writeBoolean(message.isUsingBuildMode);
	}

	public static IsUsingBuildModePacket decode(FriendlyByteBuf buf) {
		return new IsUsingBuildModePacket(buf.readBoolean());
	}

	public static class Handler {
		public static void handle(IsUsingBuildModePacket message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				ServerBuildState.setIsUsingBuildMode(ctx.get().getSender(), message.isUsingBuildMode);
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
