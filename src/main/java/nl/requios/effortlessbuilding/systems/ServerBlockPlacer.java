package nl.requios.effortlessbuilding.systems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.utilities.BlockEntry;

import java.util.List;

// Receives block placement requests from the client and places them
public class ServerBlockPlacer {

    public void placeBlocks(Player player, List<BlockEntry> blocks) {
        EffortlessBuilding.log(player, "Placing " + blocks.size() + " blocks");

        for (BlockEntry block : blocks) {
            placeBlock(player, block);
        }
    }

    public void placeBlock(Player player, BlockEntry block) {
        Level world = player.level;
        if (!world.isLoaded(block.blockPos)) return;

        if (block.meansBreakBlock()) {
            breakBlock(player, block.blockPos);
            return;
        }

        boolean success = world.setBlock(block.blockPos, block.blockState, 3);
    }

    public void breakBlock(Player player, BlockPos pos) {
        ServerLevel world = (ServerLevel) player.level;
        if (!world.isLoaded(pos) || world.isEmptyBlock(pos)) return;

        //Held tool

        if (!player.getAbilities().instabuild) {

            //Drops
            BlockState blockState = world.getBlockState(pos);
            List<ItemStack> drops = Block.getDrops(blockState, world, pos, world.getBlockEntity(pos), player, player.getMainHandItem());
            for (ItemStack drop : drops) {
                player.addItem(drop);
            }
        }

        world.removeBlock(pos, false);
    }

}
