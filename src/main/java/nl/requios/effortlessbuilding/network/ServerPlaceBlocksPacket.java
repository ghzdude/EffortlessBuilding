package nl.requios.effortlessbuilding.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.utilities.BlockSet;

import java.util.function.Supplier;

/**
 * Sends a message to the server to place multiple blocks
 */
public class ServerPlaceBlocksPacket {

	private BlockSet blocks;
	private long placeTime;

	public ServerPlaceBlocksPacket() {}

	public ServerPlaceBlocksPacket(BlockSet blocks, long placeTime) {
		this.blocks = blocks;
		this.placeTime = placeTime;
	}

	public static void encode(ServerPlaceBlocksPacket message, FriendlyByteBuf buf) {
		BlockSet.encode(buf, message.blocks);
		buf.writeLong(message.placeTime);
	}

	public static ServerPlaceBlocksPacket decode(FriendlyByteBuf buf) {
		ServerPlaceBlocksPacket message = new ServerPlaceBlocksPacket();
		message.blocks = BlockSet.decode(buf);
		message.placeTime = buf.readLong();
		return message;
	}

	public static class Handler {
		public static void handle(ServerPlaceBlocksPacket message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);
				
				EffortlessBuilding.SERVER_BLOCK_PLACER.placeBlocksDelayed(player, message.blocks, message.placeTime);
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
