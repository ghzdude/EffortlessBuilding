package nl.requios.effortlessbuilding.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import nl.requios.effortlessbuilding.buildmode.BuildModes;

import java.util.function.Supplier;

/***
 * Sends a message to the server indicating that a player wants to break a block
 */
public class BlockBrokenMessage {

	private final boolean blockHit;
	private final BlockPos blockPos;
	private final Direction sideHit;
	private final Vec3 hitVec;

	public BlockBrokenMessage() {
		this.blockHit = false;
		this.blockPos = BlockPos.ZERO;
		this.sideHit = Direction.UP;
		this.hitVec = new Vec3(0, 0, 0);
	}

	public BlockBrokenMessage(BlockHitResult result) {
		this.blockHit = result.getType() == HitResult.Type.BLOCK;
		this.blockPos = result.getBlockPos();
		this.sideHit = result.getDirection();
		this.hitVec = result.getLocation();
	}

	public BlockBrokenMessage(boolean blockHit, BlockPos blockPos, Direction sideHit, Vec3 hitVec) {
		this.blockHit = blockHit;
		this.blockPos = blockPos;
		this.sideHit = sideHit;
		this.hitVec = hitVec;
	}

	public static void encode(BlockBrokenMessage message, FriendlyByteBuf buf) {
		buf.writeBoolean(message.blockHit);
		buf.writeInt(message.blockPos.getX());
		buf.writeInt(message.blockPos.getY());
		buf.writeInt(message.blockPos.getZ());
		buf.writeInt(message.sideHit.get3DDataValue());
		buf.writeDouble(message.hitVec.x);
		buf.writeDouble(message.hitVec.y);
		buf.writeDouble(message.hitVec.z);
	}

	public static BlockBrokenMessage decode(FriendlyByteBuf buf) {
		boolean blockHit = buf.readBoolean();
		BlockPos blockPos = new BlockPos(buf.readInt(), buf.readInt(), buf.readInt());
		Direction sideHit = Direction.from3DDataValue(buf.readInt());
		Vec3 hitVec = new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
		return new BlockBrokenMessage(blockHit, blockPos, sideHit, hitVec);
	}

	public boolean isBlockHit() {
		return blockHit;
	}

	public BlockPos getBlockPos() {
		return blockPos;
	}

	public Direction getSideHit() {
		return sideHit;
	}

	public Vec3 getHitVec() {
		return hitVec;
	}

	public static class Handler {
		public static void handle(BlockBrokenMessage message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				if (ctx.get().getDirection().getReceptionSide() == LogicalSide.SERVER) {
					//Received serverside
					BuildModes.onBlockBrokenMessage(ctx.get().getSender(), message);
				}
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
