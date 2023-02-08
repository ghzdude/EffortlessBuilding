package nl.requios.effortlessbuilding.utilities;

import com.google.common.collect.Lists;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemHandlerHelper;
import nl.requios.effortlessbuilding.create.foundation.utility.BlockHelper;

import java.util.List;

//Server only
public class BlockPlacerHelper {

    public static boolean breakBlock(Player player, BlockEntry blockEntry) {
        ItemStack usedTool = player.getMainHandItem();
        if (usedTool.isEmpty() || !(usedTool.getItem() instanceof DiggerItem)) {
            ItemStack offhand = player.getOffhandItem();
            if (!offhand.isEmpty() && offhand.getItem() instanceof DiggerItem) {
                usedTool = offhand;
            }
        }

        boolean brokeBlock = BlockHelper.destroyBlockAs(player.level, blockEntry.blockPos, player, usedTool, 0f, stack -> {
            if (!player.isCreative()) {
                ItemHandlerHelper.giveItemToPlayer(player, stack);
            }
        });
        return brokeBlock;
    }

    public static boolean placeBlock(Player player, BlockEntry blockEntry) {
        if (blockEntry.itemStack == null) {
            return placeBlockWithoutItem(player, blockEntry);
        } else {
            var interactionResult = placeItem(player, blockEntry);
            return interactionResult == InteractionResult.SUCCESS;
        }
    }

    private static boolean placeBlockWithoutItem(Player player, BlockEntry blockEntry) {
        Level level = player.level;

        level.captureBlockSnapshots = true;
        BlockHelper.placeSchematicBlock(level, player, blockEntry.newBlockState, blockEntry.blockPos, blockEntry.itemStack, null);
        level.captureBlockSnapshots = false;

        //Find out if we get to keep the placed block by sending a forge event
        @SuppressWarnings("unchecked")
        List<BlockSnapshot> blockSnapshots = (List<BlockSnapshot>)level.capturedBlockSnapshots.clone();
        level.capturedBlockSnapshots.clear();
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
            for (BlockSnapshot snap : blockSnapshots)
            {
                int updateFlag = snap.getFlag();
                BlockState oldBlock = snap.getReplacedBlock();
                BlockState newBlock = level.getBlockState(snap.getPos());
                newBlock.onPlace(level, snap.getPos(), oldBlock, false);

                level.markAndNotifyBlock(snap.getPos(), level.getChunkAt(snap.getPos()), oldBlock, newBlock, updateFlag, 512);
            }
        }
        level.capturedBlockSnapshots.clear();
        return !eventResult;
    }

    //ForgeHooks::onPlaceItemIntoWorld
    private static InteractionResult placeItem(Player player, BlockEntry block) {
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
