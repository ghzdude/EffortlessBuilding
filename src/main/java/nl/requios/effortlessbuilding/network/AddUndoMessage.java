package nl.requios.effortlessbuilding.network;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedoBlockSet;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedo;

import java.util.ArrayList;
import java.util.function.Supplier;

/***
 * Sends a message to the client asking to add a block to the undo stack.
 */
@Deprecated
public class AddUndoMessage {
	private final BlockPos coordinate;
	private final BlockState previousBlockState;
	private final BlockState newBlockState;

	public AddUndoMessage() {
		coordinate = BlockPos.ZERO;
		previousBlockState = null;
		newBlockState = null;
	}

	public AddUndoMessage(BlockPos coordinate, BlockState previousBlockState, BlockState newBlockState) {
		this.coordinate = coordinate;
		this.previousBlockState = previousBlockState;
		this.newBlockState = newBlockState;
	}

	public static void encode(AddUndoMessage message, FriendlyByteBuf buf) {
		buf.writeInt(message.coordinate.getX());
		buf.writeInt(message.coordinate.getY());
		buf.writeInt(message.coordinate.getZ());
		buf.writeInt(Block.getId(message.previousBlockState));
		buf.writeInt(Block.getId(message.newBlockState));
	}

	public static AddUndoMessage decode(FriendlyByteBuf buf) {
		BlockPos coordinate = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
		BlockState previousBlockState = Block.stateById(buf.readInt());
		BlockState newBlockState = Block.stateById(buf.readInt());
		return new AddUndoMessage(coordinate, previousBlockState, newBlockState);
	}

	public BlockPos getCoordinate() {
		return coordinate;
	}

	public BlockState getPreviousBlockState() {
		return previousBlockState;
	}

	public BlockState getNewBlockState() {
		return newBlockState;
	}

	public static class Handler {
		public static void handle(AddUndoMessage message, Supplier<NetworkEvent.Context> ctx) {
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
		public static void handle(AddUndoMessage message, Supplier<NetworkEvent.Context> ctx) {
			Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);
			//Add to undo stack clientside
			//Only the appropriate player that needs to add this to the undo stack gets this message
			UndoRedo.addUndo(player, new UndoRedoBlockSet(
					new ArrayList<BlockPos>() {{
						add(message.getCoordinate());
					}},
					new ArrayList<BlockState>() {{
						add(message.getPreviousBlockState());
					}},
					new ArrayList<BlockState>() {{
						add(message.getNewBlockState());
					}},
					message.getCoordinate(), message.getCoordinate()));
		}
	}
}
