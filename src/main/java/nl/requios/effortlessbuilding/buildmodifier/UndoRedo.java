package nl.requios.effortlessbuilding.buildmodifier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;
import nl.requios.effortlessbuilding.BuildConfig;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.helper.FixedStack;
import nl.requios.effortlessbuilding.helper.InventoryHelper;
import nl.requios.effortlessbuilding.helper.SurvivalHelper;
import nl.requios.effortlessbuilding.render.BlockPreviewRenderer;

import java.util.*;

public class UndoRedo {

	//Undo and redo stacks per player
	//Gets added to twice in singleplayer (server and client) if not careful. So separate stacks.
	private static final Map<UUID, FixedStack<BlockSet>> undoStacksClient = new HashMap<>();
	private static final Map<UUID, FixedStack<BlockSet>> undoStacksServer = new HashMap<>();
	private static final Map<UUID, FixedStack<BlockSet>> redoStacksClient = new HashMap<>();
	private static final Map<UUID, FixedStack<BlockSet>> redoStacksServer = new HashMap<>();

	//add to undo stack
	public static void addUndo(PlayerEntity player, BlockSet blockSet) {
		Map<UUID, FixedStack<BlockSet>> undoStacks = player.level.isClientSide ? undoStacksClient : undoStacksServer;

		//Assert coordinates is as long as previous and new blockstate lists
		if (blockSet.getCoordinates().size() != blockSet.getPreviousBlockStates().size() ||
			blockSet.getCoordinates().size() != blockSet.getNewBlockStates().size()) {
			EffortlessBuilding.logger.error("Coordinates and blockstate lists are not equal length. Coordinates: {}. Previous blockstates: {}. New blockstates: {}.",
				blockSet.getCoordinates().size(), blockSet.getPreviousBlockStates().size(), blockSet.getNewBlockStates().size());
		}

		//Warn if previous and new blockstate are equal
		//Can happen in a lot of valid cases
//        for (int i = 0; i < blockSet.getCoordinates().size(); i++) {
//            if (blockSet.getPreviousBlockStates().get(i).equals(blockSet.getNewBlockStates().get(i))) {
//                EffortlessBuilding.logger.warn("Previous and new blockstates are equal at index {}. Blockstate: {}.",
//                        i, blockSet.getPreviousBlockStates().get(i));
//            }
//        }

		//If no stack exists, make one
		if (!undoStacks.containsKey(player.getUUID())) {
			undoStacks.put(player.getUUID(), new FixedStack<>(new BlockSet[BuildConfig.survivalBalancers.undoStackSize.get()]));
		}

		undoStacks.get(player.getUUID()).push(blockSet);
	}

	private static void addRedo(PlayerEntity player, BlockSet blockSet) {
		Map<UUID, FixedStack<BlockSet>> redoStacks = player.level.isClientSide ? redoStacksClient : redoStacksServer;

		//(No asserts necessary, it's private)

		//If no stack exists, make one
		if (!redoStacks.containsKey(player.getUUID())) {
			redoStacks.put(player.getUUID(), new FixedStack<>(new BlockSet[BuildConfig.survivalBalancers.undoStackSize.get()]));
		}

		redoStacks.get(player.getUUID()).push(blockSet);
	}

	public static boolean undo(PlayerEntity player) {
		Map<UUID, FixedStack<BlockSet>> undoStacks = player.level.isClientSide ? undoStacksClient : undoStacksServer;

		if (!undoStacks.containsKey(player.getUUID())) return false;

		FixedStack<BlockSet> undoStack = undoStacks.get(player.getUUID());

		if (undoStack.isEmpty()) return false;

		BlockSet blockSet = undoStack.pop();
		List<BlockPos> coordinates = blockSet.getCoordinates();
		List<BlockState> previousBlockStates = blockSet.getPreviousBlockStates();
		List<BlockState> newBlockStates = blockSet.getNewBlockStates();
		Vector3d hitVec = blockSet.getHitVec();

		//Find up to date itemstacks in player inventory
		List<ItemStack> itemStacks = findItemStacksInInventory(player, previousBlockStates);

		if (player.level.isClientSide) {
			BlockPreviewRenderer.onBlocksBroken(coordinates, itemStacks, newBlockStates, blockSet.getSecondPos(), blockSet.getFirstPos());
		} else {
			//break all those blocks, reset to what they were
			for (int i = 0; i < coordinates.size(); i++) {
				BlockPos coordinate = coordinates.get(i);
				ItemStack itemStack = itemStacks.get(i);

				if (previousBlockStates.get(i).equals(newBlockStates.get(i))) continue;

				//get blockstate from itemstack
				BlockState previousBlockState = Blocks.AIR.defaultBlockState();
				if (itemStack.getItem() instanceof BlockItem) {
					previousBlockState = ((BlockItem) itemStack.getItem()).getBlock().defaultBlockState();
				}

				if (player.level.isLoaded(coordinate)) {
					//check itemstack empty
					if (itemStack.isEmpty()) {
						itemStack = findItemStackInInventory(player, previousBlockStates.get(i));
						//get blockstate from new itemstack
						if (!itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem) {
							previousBlockState = ((BlockItem) itemStack.getItem()).getBlock().defaultBlockState();
						} else {
							if (previousBlockStates.get(i).getBlock() != Blocks.AIR)
								EffortlessBuilding.logTranslate(player, "", previousBlockStates.get(i).getBlock().getDescriptionId(), " not found in inventory", true);
							previousBlockState = Blocks.AIR.defaultBlockState();
						}
					}
					if (itemStack.isEmpty()) SurvivalHelper.breakBlock(player.level, player, coordinate, true);
					//if previousBlockState is air, placeBlock will set it to air
					SurvivalHelper.placeBlock(player.level, player, coordinate, previousBlockState, itemStack, Direction.UP, hitVec, true, false, false);
				}
			}
		}

		//add to redo
		addRedo(player, blockSet);

		return true;
	}

	public static boolean redo(PlayerEntity player) {
		Map<UUID, FixedStack<BlockSet>> redoStacks = player.level.isClientSide ? redoStacksClient : redoStacksServer;

		if (!redoStacks.containsKey(player.getUUID())) return false;

		FixedStack<BlockSet> redoStack = redoStacks.get(player.getUUID());

		if (redoStack.isEmpty()) return false;

		BlockSet blockSet = redoStack.pop();
		List<BlockPos> coordinates = blockSet.getCoordinates();
		List<BlockState> previousBlockStates = blockSet.getPreviousBlockStates();
		List<BlockState> newBlockStates = blockSet.getNewBlockStates();
		Vector3d hitVec = blockSet.getHitVec();

		//Find up to date itemstacks in player inventory
		List<ItemStack> itemStacks = findItemStacksInInventory(player, newBlockStates);

		if (player.level.isClientSide) {
			BlockPreviewRenderer.onBlocksPlaced(coordinates, itemStacks, newBlockStates, blockSet.getFirstPos(), blockSet.getSecondPos());
		} else {
			//place blocks
			for (int i = 0; i < coordinates.size(); i++) {
				BlockPos coordinate = coordinates.get(i);
				ItemStack itemStack = itemStacks.get(i);

				if (previousBlockStates.get(i).equals(newBlockStates.get(i))) continue;

				//get blockstate from itemstack
				BlockState newBlockState = Blocks.AIR.defaultBlockState();
				if (itemStack.getItem() instanceof BlockItem) {
					newBlockState = ((BlockItem) itemStack.getItem()).getBlock().defaultBlockState();
				}

				if (player.level.isLoaded(coordinate)) {
					//check itemstack empty
					if (itemStack.isEmpty()) {
						itemStack = findItemStackInInventory(player, newBlockStates.get(i));
						//get blockstate from new itemstack
						if (!itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem) {
							newBlockState = ((BlockItem) itemStack.getItem()).getBlock().defaultBlockState();
						} else {
							if (newBlockStates.get(i).getBlock() != Blocks.AIR)
								EffortlessBuilding.logTranslate(player, "", newBlockStates.get(i).getBlock().getDescriptionId(), " not found in inventory", true);
							newBlockState = Blocks.AIR.defaultBlockState();
						}
					}
					if (itemStack.isEmpty()) SurvivalHelper.breakBlock(player.level, player, coordinate, true);
					SurvivalHelper.placeBlock(player.level, player, coordinate, newBlockState, itemStack, Direction.UP, hitVec, true, false, false);
				}
			}
		}

		//add to undo
		addUndo(player, blockSet);

		return true;
	}

	public static void clear(PlayerEntity player) {
		Map<UUID, FixedStack<BlockSet>> undoStacks = player.level.isClientSide ? undoStacksClient : undoStacksServer;
		Map<UUID, FixedStack<BlockSet>> redoStacks = player.level.isClientSide ? redoStacksClient : redoStacksServer;
		if (undoStacks.containsKey(player.getUUID())) {
			undoStacks.get(player.getUUID()).clear();
		}
		if (redoStacks.containsKey(player.getUUID())) {
			redoStacks.get(player.getUUID()).clear();
		}
	}

	private static List<ItemStack> findItemStacksInInventory(PlayerEntity player, List<BlockState> blockStates) {
		List<ItemStack> itemStacks = new ArrayList<>(blockStates.size());
		for (BlockState blockState : blockStates) {
			itemStacks.add(findItemStackInInventory(player, blockState));
		}
		return itemStacks;
	}

	private static ItemStack findItemStackInInventory(PlayerEntity player, BlockState blockState) {
		ItemStack itemStack = ItemStack.EMPTY;
		if (blockState == null) return itemStack;

		//First try previousBlockStates
		//TODO try to find itemstack with right blockstate first
		// then change line 103 back (get state from item)
		itemStack = InventoryHelper.findItemStackInInventory(player, blockState.getBlock());


		//then anything it drops
		if (itemStack.isEmpty()) {
			//Cannot check drops on clientside because loot tables are server only
			if (!player.level.isClientSide) {
				List<ItemStack> itemsDropped = Block.getDrops(blockState, (ServerWorld) player.level, BlockPos.ZERO, null);
				for (ItemStack itemStackDropped : itemsDropped) {
					if (itemStackDropped.getItem() instanceof BlockItem) {
						Block block = ((BlockItem) itemStackDropped.getItem()).getBlock();
						itemStack = InventoryHelper.findItemStackInInventory(player, block);
					}
				}
			}
		}

		//then air
		//(already empty)

		return itemStack;
	}
}
