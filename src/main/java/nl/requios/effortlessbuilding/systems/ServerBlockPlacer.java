package nl.requios.effortlessbuilding.systems;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.ServerConfig;
import nl.requios.effortlessbuilding.create.foundation.utility.BlockHelper;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.BlockSet;
import nl.requios.effortlessbuilding.utilities.BlockUtilities;

import java.util.*;

// Receives block placement requests from the client and places them
public class ServerBlockPlacer {
    private final Set<DelayedEntry> delayedEntries = Collections.synchronizedSet(new HashSet<>());
    private final Set<DelayedEntry> delayedEntriesView = Collections.unmodifiableSet(delayedEntries);
    private boolean isPlacingOrBreakingBlocks = false;

    public void placeBlocksDelayed(Player player, BlockSet blocks, long placeTime) {
        if (!checkAndNotifyAllowedToUseMod(player)) return;

        delayedEntries.add(new DelayedEntry(player, blocks, placeTime));
    }
    
    public void tick() {
        //Iterator to prevent concurrent modification exception
        for (var iterator = delayedEntries.iterator(); iterator.hasNext(); ) {
            DelayedEntry entry = iterator.next();
            long gameTime = entry.player.level.getGameTime();
            if (gameTime >= entry.placeTime) {
                placeBlocks(entry.player, entry.blocks);
                iterator.remove();
            }
        }
    }
    
    public void placeBlocks(Player player, BlockSet blocks) {
        if (!checkAndNotifyAllowedToUseMod(player)) return;
//        EffortlessBuilding.log(player, "Placing " + blocks.size() + " blocks");

        var undoSet = new BlockSet();
        for (BlockEntry block : blocks) {
            if (blocks.skipFirst && block.blockPos == blocks.firstPos) continue;
            if (placeBlock(player, block)) {
                undoSet.add(block);
            }
        }
        EffortlessBuilding.UNDO_REDO.addUndo(player, undoSet);
    }
    
    private boolean placeBlock(Player player, BlockEntry block) {
        Level world = player.level;
        if (!world.isLoaded(block.blockPos)) return false;

        isPlacingOrBreakingBlocks = true;
        boolean placedBlock = BlockUtilities.placeBlockEntry(player, block) == InteractionResult.SUCCESS;
        isPlacingOrBreakingBlocks = false;
        return placedBlock;
    }
    
    public void breakBlocks(Player player, BlockSet blocks) {
        if (!checkAndNotifyAllowedToUseMod(player)) return;
//        EffortlessBuilding.log(player, "Breaking " + blocks.size() + " blocks");

        var undoSet = new BlockSet();
        for (BlockEntry block : blocks) {
            if (blocks.skipFirst && block.blockPos == blocks.firstPos) continue;
            if (breakBlock(player, block)) {
                undoSet.add(block);
            }
        }
        EffortlessBuilding.UNDO_REDO.addUndo(player, undoSet);
    }
    
    private boolean breakBlock(Player player, BlockEntry block) {
        ServerLevel world = (ServerLevel) player.level;
        if (!world.isLoaded(block.blockPos) || world.isEmptyBlock(block.blockPos)) return false;

        isPlacingOrBreakingBlocks = true;
        boolean brokeBlock = BlockHelper.destroyBlockAs(world, block.blockPos, player, player.getMainHandItem(), 0f, stack -> {
            if (!player.isCreative()) {
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        });
        isPlacingOrBreakingBlocks = false;
        return brokeBlock;
    }

    public boolean checkAndNotifyAllowedToUseMod(Player player) {
        //TODO TEMP
        if (!player.isCreative()) {
            EffortlessBuilding.log(player, ChatFormatting.RED + "Effortless Building is not yet supported in survival mode.");
            return false;
        }

        if (!isAllowedToUseMod(player)) {
            EffortlessBuilding.log(player, ChatFormatting.RED + "You are not allowed to use Effortless Building.");
            return false;
        }
        return true;
    }

    public boolean isAllowedToUseMod(Player player) {
        if (!ServerConfig.validation.allowInSurvival.get() && !player.isCreative()) return false;

        if (ServerConfig.validation.useWhitelist.get()) {
            return ServerConfig.validation.whitelist.get().contains(player.getGameProfile().getName());
        }

        return true;
    }
    
    public Set<DelayedEntry> getDelayedEntries() {
        return delayedEntriesView;
    }
    
    public boolean isPlacingOrBreakingBlocks() {
        return isPlacingOrBreakingBlocks;
    }
    
    public record DelayedEntry(Player player, BlockSet blocks, long placeTime) {}

}
