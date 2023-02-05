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
import nl.requios.effortlessbuilding.utilities.BlockPlacerHelper;
import nl.requios.effortlessbuilding.utilities.BlockSet;
import nl.requios.effortlessbuilding.utilities.BlockUtilities;

import java.util.*;

// Receives block placement requests from the client and places them
public class ServerBlockPlacer {
    private boolean isPlacingOrBreakingBlocks = false;

//region Delays
    private final Set<DelayedEntry> delayedEntries = Collections.synchronizedSet(new HashSet<>());
    private final Set<DelayedEntry> delayedEntriesView = Collections.unmodifiableSet(delayedEntries);

    public void placeBlocksDelayed(Player player, BlockSet blocks, long placeTime) {
        if (!checkAndNotifyAllowedToUseMod(player)) return;
        if (!validateBlockSet(player, blocks)) return;

        delayedEntries.add(new DelayedEntry(player, blocks, placeTime));
    }
    
    public void tick() {
        //Iterator to prevent concurrent modification exception
        for (var iterator = delayedEntries.iterator(); iterator.hasNext(); ) {
            DelayedEntry entry = iterator.next();
            long gameTime = entry.player.level.getGameTime();
            if (gameTime >= entry.placeTime) {
                applyBlockSet(entry.player, entry.blocks);
                iterator.remove();
            }
        }
    }

    public Set<DelayedEntry> getDelayedEntries() {
        return delayedEntriesView;
    }

    public record DelayedEntry(Player player, BlockSet blocks, long placeTime) {}
//endregion

    public void breakBlocks(Player player, BlockSet blocks) {
        applyBlockSet(player, blocks);
    }

    public void applyBlockSet(Player player, BlockSet blocks) {
        if (!checkAndNotifyAllowedToUseMod(player)) return;
        if (!validateBlockSet(player, blocks)) return;

        var undoSet = new BlockSet();
        for (BlockEntry block : blocks) {
            if (blocks.skipFirst && block.blockPos == blocks.firstPos) continue;

            if (applyBlockEntry(player, block)) {
                undoSet.add(block);
            }
        }
        EffortlessBuilding.UNDO_REDO.addUndo(player, undoSet);
    }

    public void undoBlockSet(Player player, BlockSet blocks) {
        if (!isAllowedToUndo(player)) return;

        var redoSet = new BlockSet();
        for (BlockEntry block : blocks) {
            if (blocks.skipFirst && block.blockPos == blocks.firstPos) continue;

            if (undoBlockEntry(player, block)) {
                redoSet.add(block);
            }
        }
        EffortlessBuilding.UNDO_REDO.addRedo(player, redoSet);
    }

    private boolean applyBlockEntry(Player player, BlockEntry block) {
        block.existingBlockState = player.level.getBlockState(block.blockPos);
        boolean breaking = BlockUtilities.isNullOrAir(block.newBlockState);
        if (!validateBlockEntry(player, block, breaking)) return false;

        boolean success;
        isPlacingOrBreakingBlocks = true;
        if (breaking) {
            success = BlockPlacerHelper.breakBlock(player, block);
        } else {
            success = BlockPlacerHelper.placeBlock(player, block);
        }
        isPlacingOrBreakingBlocks = false;
        return success;
    }

    private boolean undoBlockEntry(Player player, BlockEntry block) {
        //Update newBlockState for future redo's
        block.newBlockState = player.level.getBlockState(block.blockPos);
        boolean breaking = BlockUtilities.isNullOrAir(block.existingBlockState);

        var tempBlockEntry = new BlockEntry(block.blockPos);
        var newBlockState = block.existingBlockState;
        tempBlockEntry.existingBlockState = block.newBlockState;
        tempBlockEntry.newBlockState = newBlockState;

        if (!validateBlockEntry(player, tempBlockEntry, breaking)) return false;

        boolean success;
        isPlacingOrBreakingBlocks = true;
        if (breaking) {
            success = BlockPlacerHelper.placeBlock(player, tempBlockEntry);
        } else {
            success = BlockPlacerHelper.breakBlock(player, tempBlockEntry);
        }
        isPlacingOrBreakingBlocks = false;
        return success;
    }

    private boolean checkAndNotifyAllowedToUseMod(Player player) {
        //TODO temp no survival allowed
        if (!player.isCreative()) {
            EffortlessBuilding.log(player, ChatFormatting.RED + "Effortless Building is not yet supported in survival mode.");
            return false;
        }

        if (!player.getAbilities().mayBuild) {
            EffortlessBuilding.log(player, ChatFormatting.RED + "You are not allowed to build.");
            return false;
        }

        if (!isAllowedToUseMod(player)) {
            EffortlessBuilding.log(player, ChatFormatting.RED + "You are not allowed to use Effortless Building.");
            return false;
        }
        return true;
    }

    private boolean isAllowedToUseMod(Player player) {
        if (!ServerConfig.validation.allowInSurvival.get() && !player.isCreative()) return false;

        if (ServerConfig.validation.useWhitelist.get()) {
            return ServerConfig.validation.whitelist.get().contains(player.getGameProfile().getName());
        }

        return true;
    }

    private boolean isAllowedToUndo(Player player) {
        if (!player.isCreative()) {
            EffortlessBuilding.log(player, ChatFormatting.RED + "Undo is not supported in survival mode.");
            return false;
        }

        return true;
    }

    private boolean validateBlockSet(Player player, BlockSet blocks) {
        if (blocks.size() > ServerConfig.validation.maxBlocksPlacedAtOnce.get()) {
            EffortlessBuilding.log(player, ChatFormatting.RED + "Too many blocks to place. Max: " + ServerConfig.validation.maxBlocksPlacedAtOnce.get());
            return false;
        }

        //Dont allow mixing breaking and placing blocks
        //TODO fix if skipping first block
        boolean breaking = blocks.getFirstBlockEntry().newBlockState == null || blocks.getFirstBlockEntry().newBlockState.isAir();
        for (var iterator = blocks.iterator(); iterator.hasNext(); ) {
            var block = iterator.next();
            if (block.newBlockState == null || block.newBlockState.isAir()) {
                if (!breaking) {
                    EffortlessBuilding.log(player, ChatFormatting.RED + "Cannot mix breaking and placing blocks.");
                    return false;
                }
            } else {
                if (breaking) {
                    EffortlessBuilding.log(player, ChatFormatting.RED + "Cannot mix breaking and placing blocks.");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateBlockEntry(Player player, BlockEntry block, boolean breaking) {
        if (!player.level.isLoaded(block.blockPos)) return false;

        if (breaking && BlockUtilities.isNullOrAir(block.existingBlockState)) return false;



        return true;
    }
    
    public boolean isPlacingOrBreakingBlocks() {
        return isPlacingOrBreakingBlocks;
    }
}
