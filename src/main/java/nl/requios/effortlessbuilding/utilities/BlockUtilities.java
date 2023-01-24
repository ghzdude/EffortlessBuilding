package nl.requios.effortlessbuilding.utilities;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

//Common
public class BlockUtilities {

    public static BlockState getBlockState(Player player, InteractionHand hand, ItemStack blockItemStack, BlockEntry blockEntry) {
        Block block = Block.byItem(blockItemStack.getItem());
        //TODO convert lookingAt hitvec to relative hitvec
        var blockHitResult = new BlockHitResult(Vec3.ZERO, Direction.UP, blockEntry.blockPos, false);
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
}
