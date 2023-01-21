package nl.requios.effortlessbuilding.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.BitSet;

public class BlockEntry {
    public final BlockPos blockPos;
    public boolean mirrorX;
    public boolean mirrorY;
    public boolean mirrorZ;
    public Rotation rotation;
    public BlockState blockState;
    public ItemStack itemStack = ItemStack.EMPTY;

    public BlockEntry(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public boolean meansBreakBlock() {
        return blockState == null || blockState.isAir();
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

    public static void encode(FriendlyByteBuf buf, BlockEntry block) {
        buf.writeBlockPos(block.blockPos);
        buf.writeNullable(block.blockState, (buffer, blockState) -> buffer.writeNbt(NbtUtils.writeBlockState(blockState)));
        buf.writeItem(block.itemStack);
    }

    public static BlockEntry decode(FriendlyByteBuf buf) {
        BlockEntry block = new BlockEntry(buf.readBlockPos());
        block.blockState = buf.readNullable(buffer -> {
            var nbt = buf.readNbt();
            if (nbt == null) return null;
            return NbtUtils.readBlockState(nbt);
        });
        block.itemStack = buf.readItem();
        return block;
    }

}
