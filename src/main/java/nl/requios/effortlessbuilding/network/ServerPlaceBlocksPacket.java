package nl.requios.effortlessbuilding.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.utilities.BlockEntry;

import java.util.List;
import java.util.function.Supplier;

/**
 * Sends a message to the server to place multiple blocks
 */
public class ServerPlaceBlocksPacket {

	private List<BlockEntry> blocks;

	public ServerPlaceBlocksPacket() {}

	public ServerPlaceBlocksPacket(List<BlockEntry> blocks) {
		this.blocks = blocks;
	}

	public static void encode(ServerPlaceBlocksPacket message, FriendlyByteBuf buf) {
		buf.writeCollection(message.blocks, BlockEntry::encode);
	}

	public static ServerPlaceBlocksPacket decode(FriendlyByteBuf buf) {
		ServerPlaceBlocksPacket message = new ServerPlaceBlocksPacket();
		message.blocks = buf.readList(BlockEntry::decode);
		return message;
	}


	public static class Handler {
		public static void handle(ServerPlaceBlocksPacket message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);

				EffortlessBuilding.SERVER_BLOCK_PLACER.placeBlocks(player, message.blocks);
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
