package nl.requios.effortlessbuilding.systems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.create.foundation.utility.BlockHelper;
import nl.requios.effortlessbuilding.utilities.BlockEntry;

import java.util.List;

// Receives block placement requests from the client and places them
public class ServerBlockPlacer {
    private boolean isPlacingOrBreakingBlocks = false;

    public void placeBlocks(Player player, List<BlockEntry> blocks) {
        EffortlessBuilding.log(player, "Placing " + blocks.size() + " blocks");

        for (BlockEntry block : blocks) {
            placeBlock(player, block);
        }
    }

    public void placeBlock(Player player, BlockEntry block) {
        Level world = player.level;
        if (!world.isLoaded(block.blockPos)) return;

        isPlacingOrBreakingBlocks = true;
        BlockHelper.placeSchematicBlock(world, block.blockState, block.blockPos, block.itemStack, null);
        isPlacingOrBreakingBlocks = false;
    }

    public void breakBlocks(Player player, List<BlockEntry> blocks) {
        EffortlessBuilding.log(player, "Breaking " + blocks.size() + " blocks");

        for (BlockEntry block : blocks) {
            breakBlock(player, block);
        }
    }

    public void breakBlock(Player player, BlockEntry block) {
        ServerLevel world = (ServerLevel) player.level;
        if (!world.isLoaded(block.blockPos) || world.isEmptyBlock(block.blockPos)) return;

        isPlacingOrBreakingBlocks = true;
        BlockHelper.destroyBlockAs(world, block.blockPos, player, player.getMainHandItem(), 0f, stack -> {
            if (!player.isCreative()) {
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        });
        isPlacingOrBreakingBlocks = false;
    }

    public boolean isPlacingOrBreakingBlocks() {
        return isPlacingOrBreakingBlocks;
    }
}
