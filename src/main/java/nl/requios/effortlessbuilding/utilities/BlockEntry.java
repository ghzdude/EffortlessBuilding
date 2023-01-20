package nl.requios.effortlessbuilding.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.BitSet;

public class BlockEntry {
    public final BlockPos blockPos;
    public boolean mirrorX;
    public boolean mirrorY;
    public boolean mirrorZ;
    public BlockState blockState;

    public BlockEntry(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public BitSet getMirrorBitSet() {
        BitSet bitSet = new BitSet(3);
        bitSet.set(0, mirrorX);
        bitSet.set(1, mirrorY);
        bitSet.set(2, mirrorZ);
        return bitSet;
    }

    public void setMirrorBitSet(BitSet bitSet) {
        mirrorX = bitSet.get(0);
        mirrorY = bitSet.get(1);
        mirrorZ = bitSet.get(2);
    }

    public boolean meansBreakBlock() {
        return blockState == null || blockState.isAir();
    }

    public static void encode(FriendlyByteBuf buf, BlockEntry block) {
        buf.writeBlockPos(block.blockPos);
        buf.writeBitSet(block.getMirrorBitSet());
        buf.writeNbt(NbtUtils.writeBlockState(block.blockState));
    }

    public static BlockEntry decode(FriendlyByteBuf buf) {
        BlockEntry block = new BlockEntry(buf.readBlockPos());
        block.setMirrorBitSet(buf.readBitSet());
        block.blockState = NbtUtils.readBlockState(buf.readNbt());
        return block;
    }

}
