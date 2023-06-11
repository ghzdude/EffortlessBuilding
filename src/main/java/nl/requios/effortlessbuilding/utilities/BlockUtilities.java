package nl.requios.effortlessbuilding.utilities;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Half;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.BlockSnapshot;

//Common
public class BlockUtilities {

    public static boolean isNullOrAir(BlockState blockState) {
        return blockState == null || blockState.isAir();
    }

    @Deprecated //Use BlockEntry.setItemStackAndFindNewBlockState instead
    public static BlockState getBlockState(Player player, InteractionHand hand, ItemStack blockItemStack, BlockEntry blockEntry, Vec3 relativeHitVec, Direction sideHit) {
        Block block = Block.byItem(blockItemStack.getItem());
        Vec3 hitVec = relativeHitVec.add(Vec3.atLowerCornerOf(blockEntry.blockPos));
        var blockHitResult = new BlockHitResult(hitVec, sideHit, blockEntry.blockPos, false);
        return block.getStateForPlacement(new BlockPlaceContext(player, hand, blockItemStack, blockHitResult));
    }

    public static boolean determineIfLookingAtInteractiveObject(Minecraft mc, Level level) {
        //Check if we are looking at an interactive object
        var result = false;
        if (mc.hitResult != null) {
            if (mc.hitResult.getType() == HitResult.Type.BLOCK) {
                var blockHitResult = (BlockHitResult) mc.hitResult;
                var blockState = level.getBlockState(blockHitResult.getBlockPos());
                if (blockState.hasBlockEntity()) {
                    result = true;
                }
            }
            if (mc.hitResult.getType() == HitResult.Type.ENTITY) {
                result = true;
            }
        }
        return result;
    }

    public static void playSoundIfFurtherThanNormal(Player player, BlockEntry blockEntry, boolean breaking) {

        if (Minecraft.getInstance().hitResult != null && Minecraft.getInstance().hitResult.getType() == HitResult.Type.BLOCK)
            return;

        if (blockEntry == null || blockEntry.newBlockState == null)
            return;

        SoundType soundType = blockEntry.newBlockState.getBlock().getSoundType(blockEntry.newBlockState, player.level, blockEntry.blockPos, player);
        SoundEvent soundEvent = breaking ? soundType.getBreakSound() : soundType.getPlaceSound();
        player.level.playSound(player, player.blockPosition(), soundEvent, SoundSource.BLOCKS, 0.6f, soundType.getPitch());
    }

    public static BlockState getVerticalMirror(BlockState blockState) {
        //Stairs
        if (blockState.getBlock() instanceof StairBlock) {
            if (blockState.getValue(StairBlock.HALF) == Half.BOTTOM) {
                return blockState.setValue(StairBlock.HALF, Half.TOP);
            } else {
                return blockState.setValue(StairBlock.HALF, Half.BOTTOM);
            }
        }

        //Slabs
        if (blockState.getBlock() instanceof SlabBlock) {
            if (blockState.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
                return blockState;
            } else if (blockState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
                return blockState.setValue(SlabBlock.TYPE, SlabType.TOP);
            } else {
                return blockState.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
            }
        }

        //Buttons, endrod, observer, piston
        if (blockState.getBlock() instanceof DirectionalBlock) {
            if (blockState.getValue(DirectionalBlock.FACING) == Direction.DOWN) {
                return blockState.setValue(DirectionalBlock.FACING, Direction.UP);
            } else if (blockState.getValue(DirectionalBlock.FACING) == Direction.UP) {
                return blockState.setValue(DirectionalBlock.FACING, Direction.DOWN);
            }
        }

        //Dispenser, dropper
        if (blockState.getBlock() instanceof DispenserBlock) {
            if (blockState.getValue(DispenserBlock.FACING) == Direction.DOWN) {
                return blockState.setValue(DispenserBlock.FACING, Direction.UP);
            } else if (blockState.getValue(DispenserBlock.FACING) == Direction.UP) {
                return blockState.setValue(DispenserBlock.FACING, Direction.DOWN);
            }
        }

        return blockState;
    }

    public static BlockHitResult getLookingAtFar(Player player) {
        Level world = player.level;

        //base distance off of player ability (config)
        float raytraceRange = ReachHelper.getPlacementReach(player);

        Vec3 look = player.getLookAngle();
        Vec3 start = new Vec3(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());
        Vec3 end = new Vec3(player.getX() + look.x * raytraceRange, player.getY() + player.getEyeHeight() + look.y * raytraceRange, player.getZ() + look.z * raytraceRange);

        return world.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
    }
}
