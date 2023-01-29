package nl.requios.effortlessbuilding.systems;

import com.google.common.collect.Lists;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.requios.effortlessbuilding.create.foundation.utility.BlockHelper;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.BlockSet;

import java.util.*;

// Receives block placement requests from the client and places them
public class ServerBlockPlacer {
    private final Set<DelayedEntry> delayedEntries = Collections.synchronizedSet(new HashSet<>());
    private final Set<DelayedEntry> delayedEntriesView = Collections.unmodifiableSet(delayedEntries);
    private boolean isPlacingOrBreakingBlocks = false;

    public void placeBlocksDelayed(Player player, BlockSet blocks, long placeTime) {
    
        delayedEntries.add(new DelayedEntry(player, blocks, placeTime));
    }
    
    public void tick() {
        for (DelayedEntry entry : delayedEntries) {
            long gameTime = entry.player.level.getGameTime();
            if (gameTime >= entry.placeTime) {
                placeBlocks(entry.player, entry.blocks);
                delayedEntries.remove(entry);
            }
        }
    }
    
    public void placeBlocks(Player player, BlockSet blocks) {
//        EffortlessBuilding.log(player, "Placing " + blocks.size() + " blocks");
        
        for (BlockEntry block : blocks) {
            if (blocks.skipFirst && block.blockPos == blocks.firstPos) continue;
            placeBlock(player, block);
        }
    }
    
    public void placeBlock(Player player, BlockEntry block) {
        Level world = player.level;
        if (!world.isLoaded(block.blockPos)) return;

        isPlacingOrBreakingBlocks = true;
        boolean placedBlock = onPlaceItemIntoWorld(player, block) == InteractionResult.SUCCESS;
        isPlacingOrBreakingBlocks = false;
    }
    
    public void breakBlocks(Player player, BlockSet blocks) {
//        EffortlessBuilding.log(player, "Breaking " + blocks.size() + " blocks");

        for (BlockEntry block : blocks) {
            if (blocks.skipFirst && block.blockPos == blocks.firstPos) continue;
            breakBlock(player, block);
        }
    }
    
    public void breakBlock(Player player, BlockEntry block) {
        ServerLevel world = (ServerLevel) player.level;
        if (!world.isLoaded(block.blockPos) || world.isEmptyBlock(block.blockPos)) return;

        isPlacingOrBreakingBlocks = true;
        boolean brokeBlock = BlockHelper.destroyBlockAs(world, block.blockPos, player, player.getMainHandItem(), 0f, stack -> {
            if (!player.isCreative()) {
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        });
        isPlacingOrBreakingBlocks = false;
    }
    
    public Set<DelayedEntry> getDelayedEntries() {
        return delayedEntriesView;
    }
    
    public boolean isPlacingOrBreakingBlocks() {
        return isPlacingOrBreakingBlocks;
    }
    
    public record DelayedEntry(Player player, BlockSet blocks, long placeTime) {}

    //ForgeHooks::onPlaceItemIntoWorld
    private InteractionResult onPlaceItemIntoWorld(Player player, BlockEntry block) {

        ItemStack itemstack = block.itemStack;
        Level level = player.level;

        if (player != null && !player.getAbilities().mayBuild && !itemstack.hasAdventureModePlaceTagForBlock(level.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY), new BlockInWorld(level, block.blockPos, false)))
            return InteractionResult.PASS;

        // handle all placement events here
        Item item = itemstack.getItem();
        int size = itemstack.getCount();
        CompoundTag nbt = null;
        if (itemstack.getTag() != null)
            nbt = itemstack.getTag().copy();

        if (!(itemstack.getItem() instanceof BucketItem)) // if not bucket
            level.captureBlockSnapshots = true;

        ItemStack copy = itemstack.copy();
        ////
        BlockHelper.placeSchematicBlock(level, player, block.newBlockState, block.blockPos, block.itemStack, null);
        ////
        InteractionResult ret = InteractionResult.SUCCESS;
        if (itemstack.isEmpty())
            ForgeEventFactory.onPlayerDestroyItem(player, copy, InteractionHand.MAIN_HAND);

        level.captureBlockSnapshots = false;

        if (ret.consumesAction())
        {
            // save new item data
            int newSize = itemstack.getCount();
            CompoundTag newNBT = null;
            if (itemstack.getTag() != null)
            {
                newNBT = itemstack.getTag().copy();
            }
            @SuppressWarnings("unchecked")
            List<BlockSnapshot> blockSnapshots = (List<BlockSnapshot>)level.capturedBlockSnapshots.clone();
            level.capturedBlockSnapshots.clear();

            // make sure to set pre-placement item data for event
            itemstack.setCount(size);
            itemstack.setTag(nbt);

            Direction side = Direction.UP;

            boolean eventResult = false;
            if (blockSnapshots.size() > 1)
            {
                eventResult = ForgeEventFactory.onMultiBlockPlace(player, blockSnapshots, side);
            }
            else if (blockSnapshots.size() == 1)
            {
                eventResult = ForgeEventFactory.onBlockPlace(player, blockSnapshots.get(0), side);
            }

            if (eventResult)
            {
                ret = InteractionResult.FAIL; // cancel placement
                // revert back all captured blocks
                for (BlockSnapshot blocksnapshot : Lists.reverse(blockSnapshots))
                {
                    level.restoringBlockSnapshots = true;
                    blocksnapshot.restore(true, false);
                    level.restoringBlockSnapshots = false;
                }
            }
            else
            {
                // Change the stack to its new content
                itemstack.setCount(newSize);
                itemstack.setTag(newNBT);

                for (BlockSnapshot snap : blockSnapshots)
                {
                    int updateFlag = snap.getFlag();
                    BlockState oldBlock = snap.getReplacedBlock();
                    BlockState newBlock = level.getBlockState(snap.getPos());
                    newBlock.onPlace(level, snap.getPos(), oldBlock, false);

                    level.markAndNotifyBlock(snap.getPos(), level.getChunkAt(snap.getPos()), oldBlock, newBlock, updateFlag, 512);
                }
                if (player != null)
                    player.awardStat(Stats.ITEM_USED.get(item));
            }
        }
        level.capturedBlockSnapshots.clear();

        return ret;
    }
}
