package nl.requios.effortlessbuilding.systems;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import nl.requios.effortlessbuilding.CommonConfig;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.utilities.UndoRedoBlockSet;
import nl.requios.effortlessbuilding.utilities.FixedStack;
import nl.requios.effortlessbuilding.utilities.InventoryHelper;
import nl.requios.effortlessbuilding.utilities.SurvivalHelper;

import java.util.*;

//Server only
public class UndoRedo {

	//Undo and redo stacks per player
	//Gets added to twice in singleplayer (server and client) if not careful. So separate stacks.
	private static final Map<UUID, FixedStack<UndoRedoBlockSet>> undoStacksClient = new HashMap<>();
	private static final Map<UUID, FixedStack<UndoRedoBlockSet>> undoStacksServer = new HashMap<>();
	private static final Map<UUID, FixedStack<UndoRedoBlockSet>> redoStacksClient = new HashMap<>();
	private static final Map<UUID, FixedStack<UndoRedoBlockSet>> redoStacksServer = new HashMap<>();

	//add to undo stack
	public static void addUndo(Player player, UndoRedoBlockSet blockSet) {
		Map<UUID, FixedStack<UndoRedoBlockSet>> undoStacks = player.level.isClientSide ? undoStacksClient : undoStacksServer;

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
			undoStacks.put(player.getUUID(), new FixedStack<>(new UndoRedoBlockSet[CommonConfig.survivalBalancers.undoStackSize.get()]));
		}

		undoStacks.get(player.getUUID()).push(blockSet);
	}

	private static void addRedo(Player player, UndoRedoBlockSet blockSet) {
		Map<UUID, FixedStack<UndoRedoBlockSet>> redoStacks = player.level.isClientSide ? redoStacksClient : redoStacksServer;

		//(No asserts necessary, it's private)

		//If no stack exists, make one
		if (!redoStacks.containsKey(player.getUUID())) {
			redoStacks.put(player.getUUID(), new FixedStack<>(new UndoRedoBlockSet[CommonConfig.survivalBalancers.undoStackSize.get()]));
		}

		redoStacks.get(player.getUUID()).push(blockSet);
	}

	public static boolean undo(Player player) {
		Map<UUID, FixedStack<UndoRedoBlockSet>> undoStacks = player.level.isClientSide ? undoStacksClient : undoStacksServer;

		if (!undoStacks.containsKey(player.getUUID())) return false;

		FixedStack<UndoRedoBlockSet> undoStack = undoStacks.get(player.getUUID());

		if (undoStack.isEmpty()) return false;

		UndoRedoBlockSet blockSet = undoStack.pop();
		List<BlockPos> coordinates = blockSet.getCoordinates();
		List<BlockState> previousBlockStates = blockSet.getPreviousBlockStates();
		List<BlockState> newBlockStates = blockSet.getNewBlockStates();

		//Find up to date itemstacks in player inventory
		List<ItemStack> itemStacks = findItemStacksInInventory(player, previousBlockStates);

		if (player.level.isClientSide) {
//			BlockPreviews.onBlocksBroken(coordinates, itemStacks, newBlockStates, blockSet.getSecondPos(), blockSet.getFirstPos());
		} else {
			//break all those blocks, reset to what they were
			for (int i = 0; i < coordinates.size(); i++) {
				BlockPos coordinate = coordinates.get(i);
				ItemStack itemStack = itemStacks.get(i);

				if (previousBlockStates.get(i).equals(newBlockStates.get(i))) continue;

				//get blockstate from itemstack
				BlockState previousBlockState = previousBlockStates.get(i);
				if (itemStack.getItem() instanceof BlockItem) {
					previousBlockState = ((BlockItem) itemStack.getItem()).getBlock().defaultBlockState();
				}

				if (player.level.isLoaded(coordinate)) {
					//check itemstack empty
					if (itemStack.isEmpty() && !player.isCreative()) {
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
					SurvivalHelper.placeBlock(player.level, player, coordinate, previousBlockState, itemStack, true, false, false);
				}
			}
		}

		//add to redo
		addRedo(player, blockSet);

		return true;
	}

	public static boolean redo(Player player) {
		Map<UUID, FixedStack<UndoRedoBlockSet>> redoStacks = player.level.isClientSide ? redoStacksClient : redoStacksServer;

		if (!redoStacks.containsKey(player.getUUID())) return false;

		FixedStack<UndoRedoBlockSet> redoStack = redoStacks.get(player.getUUID());

		if (redoStack.isEmpty()) return false;

		UndoRedoBlockSet blockSet = redoStack.pop();
		List<BlockPos> coordinates = blockSet.getCoordinates();
		List<BlockState> previousBlockStates = blockSet.getPreviousBlockStates();
		List<BlockState> newBlockStates = blockSet.getNewBlockStates();

		//Find up to date itemstacks in player inventory
		List<ItemStack> itemStacks = findItemStacksInInventory(player, newBlockStates);

		if (player.level.isClientSide) {
//			BlockPreviews.onBlocksPlaced(coordinates, itemStacks, newBlockStates, blockSet.getFirstPos(), blockSet.getSecondPos());
		} else {
			//place blocks
			for (int i = 0; i < coordinates.size(); i++) {
				BlockPos coordinate = coordinates.get(i);
				ItemStack itemStack = itemStacks.get(i);

				if (previousBlockStates.get(i).equals(newBlockStates.get(i))) continue;

				//get blockstate from itemstack
				BlockState newBlockState = newBlockStates.get(i);
				if (itemStack.getItem() instanceof BlockItem) {
					newBlockState = ((BlockItem) itemStack.getItem()).getBlock().defaultBlockState();
				}

				if (player.level.isLoaded(coordinate)) {
					//check itemstack empty
					if (itemStack.isEmpty() && !player.isCreative()) {
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
					SurvivalHelper.placeBlock(player.level, player, coordinate, newBlockState, itemStack, true, false, false);
				}
			}
		}

		//add to undo
		addUndo(player, blockSet);

		return true;
	}

	public static void clear(Player player) {
		Map<UUID, FixedStack<UndoRedoBlockSet>> undoStacks = player.level.isClientSide ? undoStacksClient : undoStacksServer;
		Map<UUID, FixedStack<UndoRedoBlockSet>> redoStacks = player.level.isClientSide ? redoStacksClient : redoStacksServer;
		if (undoStacks.containsKey(player.getUUID())) {
			undoStacks.get(player.getUUID()).clear();
		}
		if (redoStacks.containsKey(player.getUUID())) {
			redoStacks.get(player.getUUID()).clear();
		}
	}

	private static List<ItemStack> findItemStacksInInventory(Player player, List<BlockState> blockStates) {
		List<ItemStack> itemStacks = new ArrayList<>(blockStates.size());
		for (BlockState blockState : blockStates) {
			itemStacks.add(findItemStackInInventory(player, blockState));
		}
		return itemStacks;
	}

	private static ItemStack findItemStackInInventory(Player player, BlockState blockState) {
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
				List<ItemStack> itemsDropped = Block.getDrops(blockState, (ServerLevel) player.level, BlockPos.ZERO, null);
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