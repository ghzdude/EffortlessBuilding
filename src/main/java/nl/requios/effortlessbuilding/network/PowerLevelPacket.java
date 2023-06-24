package nl.requios.effortlessbuilding.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;

import java.util.function.Supplier;

/**
 * Sync power level between server and client, for saving and loading.
 */
public class PowerLevelPacket {

	private int powerLevel;

	public PowerLevelPacket() {
	}

	public PowerLevelPacket(int powerLevel) {
		this.powerLevel = powerLevel;
	}

	public static void encode(PowerLevelPacket message, FriendlyByteBuf buf) {
		buf.writeInt(message.powerLevel);
	}

	public static PowerLevelPacket decode(FriendlyByteBuf buf) {
		return new PowerLevelPacket(buf.readInt());
	}

	public static class Handler {
		public static void handle(PowerLevelPacket message, Supplier<NetworkEvent.Context> ctx) {
			if (ctx.get().getDirection().getReceptionSide().isServer()) {
				ctx.get().enqueueWork(() -> {
					var player = ctx.get().getSender();
					//To server, save to persistent player data
					EffortlessBuilding.SERVER_POWER_LEVEL.setPowerLevel(player, message.powerLevel);
				});
			} else {
				ctx.get().enqueueWork(() -> {
					//To client, load into system
					EffortlessBuildingClient.POWER_LEVEL.setPowerLevel(message.powerLevel);
				});
			}
			ctx.get().setPacketHandled(true);
		}
	}
}
