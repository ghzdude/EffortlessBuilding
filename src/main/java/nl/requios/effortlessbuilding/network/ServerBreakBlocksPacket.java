package nl.requios.effortlessbuilding.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.utilities.BlockSet;

import java.util.function.Supplier;

/**
 * Sends a message to the server to break multiple blocks
 */
public class ServerBreakBlocksPacket {

	private BlockSet blocks;

	public ServerBreakBlocksPacket() {}

	public ServerBreakBlocksPacket(BlockSet blocks) {
		this.blocks = blocks;
	}

	public static void encode(ServerBreakBlocksPacket message, FriendlyByteBuf buf) {
		BlockSet.encode(buf, message.blocks);
	}

	public static ServerBreakBlocksPacket decode(FriendlyByteBuf buf) {
		ServerBreakBlocksPacket message = new ServerBreakBlocksPacket();
		message.blocks = BlockSet.decode(buf);
		return message;
	}

	public static class Handler {
		public static void handle(ServerBreakBlocksPacket message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);

				EffortlessBuilding.SERVER_BLOCK_PLACER.breakBlocks(player, message.blocks);
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
