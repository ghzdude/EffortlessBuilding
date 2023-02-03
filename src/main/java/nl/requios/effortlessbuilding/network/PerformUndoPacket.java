package nl.requios.effortlessbuilding.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.systems.UndoRedo;

import java.util.function.Supplier;

public class PerformUndoPacket {

	public PerformUndoPacket() {}

	public static void encode(PerformUndoPacket message, FriendlyByteBuf buf) {}

	public static PerformUndoPacket decode(FriendlyByteBuf buf) {
		return new PerformUndoPacket();
	}

	public static class Handler {
		public static void handle(PerformUndoPacket message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				EffortlessBuilding.UNDO_REDO.undo(ctx.get().getSender());
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
