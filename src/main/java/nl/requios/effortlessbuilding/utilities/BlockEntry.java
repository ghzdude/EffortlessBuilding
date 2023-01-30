package nl.requios.effortlessbuilding.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

//Common
public class BlockEntry {
    public final BlockPos blockPos;
    public boolean mirrorX;
    public boolean mirrorY;
    public boolean mirrorZ;
    //Horizontal rotation
    public Rotation rotation;
    //BlockState that is currently in the world
    public BlockState existingBlockState;
    public BlockState newBlockState;
    public ItemStack itemStack = ItemStack.EMPTY;

    public BlockEntry(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public void copyRotationSettingsFrom(BlockEntry blockEntry) {
        mirrorX = blockEntry.mirrorX;
        mirrorY = blockEntry.mirrorY;
        mirrorZ = blockEntry.mirrorZ;
        rotation = blockEntry.rotation;
    }

    public void setItemStackAndFindNewBlockState(ItemStack itemStack, Level world, Direction originalDirection, Direction clickedFace, Vec3 relativeHitVec) {
        this.itemStack = itemStack;

        Block block = Block.byItem(itemStack.getItem());
        var direction = originalDirection;
        if (rotation != null) direction = rotation.rotate(direction);
        direction = applyMirror(direction);
        //TODO mirror and rotate relativeHitVec?
        var blockPlaceContext = new MyPlaceContext(world, blockPos, direction, itemStack, clickedFace, relativeHitVec);
        newBlockState = block.getStateForPlacement(blockPlaceContext);
        applyMirrorToBlockState();
    }

    private Direction applyMirror(Direction direction) {
        if (mirrorX && direction.getAxis() == Direction.Axis.X) direction = direction.getOpposite();
        if (mirrorY && direction.getAxis() == Direction.Axis.Y) direction = direction.getOpposite();
        if (mirrorZ && direction.getAxis() == Direction.Axis.Z) direction = direction.getOpposite();
        return direction;
    }
    
    private void applyMirrorToBlockState() {
        if (mirrorY) newBlockState = BlockUtilities.getVerticalMirror(newBlockState);
    }

    public static void encode(FriendlyByteBuf buf, BlockEntry block) {
        buf.writeBlockPos(block.blockPos);
        buf.writeNullable(block.newBlockState, (buffer, blockState) -> buffer.writeNbt(NbtUtils.writeBlockState(blockState)));
        buf.writeItem(block.itemStack);
    }

    public static BlockEntry decode(FriendlyByteBuf buf) {
        BlockEntry block = new BlockEntry(buf.readBlockPos());
        block.newBlockState = buf.readNullable(buffer -> {
            var nbt = buf.readNbt();
            if (nbt == null) return null;
            return NbtUtils.readBlockState(nbt);
        });
        block.itemStack = buf.readItem();
        return block;
    }
}
