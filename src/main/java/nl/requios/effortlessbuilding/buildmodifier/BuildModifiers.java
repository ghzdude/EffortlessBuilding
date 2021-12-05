package nl.requios.effortlessbuilding.buildmodifier;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.helper.InventoryHelper;
import nl.requios.effortlessbuilding.helper.SurvivalHelper;
import nl.requios.effortlessbuilding.item.AbstractRandomizerBagItem;
import nl.requios.effortlessbuilding.render.BlockPreviewRenderer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BuildModifiers {

	//Called from BuildModes
	public static void onBlockPlaced(PlayerEntity player, List<BlockPos> startCoordinates, Direction sideHit, Vector3d hitVec, boolean placeStartPos) {
		World world = player.level;
		AbstractRandomizerBagItem.renewRandomness();

		//Format hitvec to 0.x
		hitVec = new Vector3d(Math.abs(hitVec.x - ((int) hitVec.x)), Math.abs(hitVec.y - ((int) hitVec.y)), Math.abs(hitVec.z - ((int) hitVec.z)));

		//find coordinates and blockstates
		List<BlockPos> coordinates = findCoordinates(player, startCoordinates);
		List<ItemStack> itemStacks = new ArrayList<>();
		List<BlockState> blockStates = findBlockStates(player, startCoordinates, hitVec, sideHit, itemStacks);

		//check if valid blockstates
		if (blockStates.size() == 0 || coordinates.size() != blockStates.size()) return;

		//remember previous blockstates for undo
		List<BlockState> previousBlockStates = new ArrayList<>(coordinates.size());
		List<BlockState> newBlockStates = new ArrayList<>(coordinates.size());
		for (BlockPos coordinate : coordinates) {
			previousBlockStates.add(world.getBlockState(coordinate));
		}

		if (world.isClientSide) {

			BlockPreviewRenderer.onBlocksPlaced();

			newBlockStates = blockStates;

		} else {

			//place blocks
			for (int i = placeStartPos ? 0 : 1; i < coordinates.size(); i++) {
				BlockPos blockPos = coordinates.get(i);
				BlockState blockState = blockStates.get(i);
				ItemStack itemStack = itemStacks.get(i);

				if (world.isLoaded(blockPos)) {
					//check itemstack empty
					if (itemStack.isEmpty()) {
						//try to find new stack, otherwise continue
						itemStack = InventoryHelper.findItemStackInInventory(player, blockState.getBlock());
						if (itemStack.isEmpty()) continue;
					}
					SurvivalHelper.placeBlock(world, player, blockPos, blockState, itemStack, Direction.UP, hitVec, false, false, false);
				}
			}

			//find actual new blockstates for undo
			for (BlockPos coordinate : coordinates) {
				newBlockStates.add(world.getBlockState(coordinate));
			}
		}

		//Set first previousBlockState to empty if in NORMAL mode, to make undo/redo work
		//(Block is placed by the time it gets here, and unplaced after this)
		if (!placeStartPos) previousBlockStates.set(0, Blocks.AIR.defaultBlockState());

		//If all new blockstates are air then no use in adding it, no block was actually placed
		//Can happen when e.g. placing one block in yourself
		if (Collections.frequency(newBlockStates, Blocks.AIR.defaultBlockState()) != newBlockStates.size()) {
			//add to undo stack
			BlockPos firstPos = startCoordinates.get(0);
			BlockPos secondPos = startCoordinates.get(startCoordinates.size() - 1);
			UndoRedo.addUndo(player, new BlockSet(coordinates, previousBlockStates, newBlockStates, hitVec, firstPos, secondPos));
		}
	}

	public static void onBlockBroken(PlayerEntity player, List<BlockPos> startCoordinates, boolean breakStartPos) {
		World world = player.level;

		List<BlockPos> coordinates = findCoordinates(player, startCoordinates);

		if (coordinates.isEmpty()) return;

		//remember previous blockstates for undo
		List<BlockState> previousBlockStates = new ArrayList<>(coordinates.size());
		List<BlockState> newBlockStates = new ArrayList<>(coordinates.size());
		for (BlockPos coordinate : coordinates) {
			previousBlockStates.add(world.getBlockState(coordinate));
		}

		if (world.isClientSide) {
			BlockPreviewRenderer.onBlocksBroken();

			//list of air blockstates
			for (int i = 0; i < coordinates.size(); i++) {
				newBlockStates.add(Blocks.AIR.defaultBlockState());
			}

		} else {

			//If the player is going to instabreak grass or a plant, only break other instabreaking things
			boolean onlyInstaBreaking = !player.isCreative() &&
				world.getBlockState(startCoordinates.get(0)).getDestroySpeed(world, startCoordinates.get(0)) == 0f;

			//break all those blocks
			for (int i = breakStartPos ? 0 : 1; i < coordinates.size(); i++) {
				BlockPos coordinate = coordinates.get(i);
				if (world.isLoaded(coordinate) && !world.isEmptyBlock(coordinate)) {
					if (!onlyInstaBreaking || world.getBlockState(coordinate).getDestroySpeed(world, coordinate) == 0f) {
						SurvivalHelper.breakBlock(world, player, coordinate, false);
					}
				}
			}

			//find actual new blockstates for undo
			for (BlockPos coordinate : coordinates) {
				newBlockStates.add(world.getBlockState(coordinate));
			}
		}

		//Set first newBlockState to empty if in NORMAL mode, to make undo/redo work
		//(Block isn't broken yet by the time it gets here, and broken after this)
		if (!breakStartPos) newBlockStates.set(0, Blocks.AIR.defaultBlockState());

		//add to undo stack
		BlockPos firstPos = startCoordinates.get(0);
		BlockPos secondPos = startCoordinates.get(startCoordinates.size() - 1);
		Vector3d hitVec = new Vector3d(0.5, 0.5, 0.5);
		UndoRedo.addUndo(player, new BlockSet(coordinates, previousBlockStates, newBlockStates, hitVec, firstPos, secondPos));

	}

	public static List<BlockPos> findCoordinates(PlayerEntity player, List<BlockPos> posList) {
		List<BlockPos> coordinates = new ArrayList<>();
		//Add current blocks being placed too
		coordinates.addAll(posList);

		//Find mirror/array/radial mirror coordinates for each blockpos
		for (BlockPos blockPos : posList) {
			List<BlockPos> arrayCoordinates = Array.findCoordinates(player, blockPos);
			coordinates.addAll(arrayCoordinates);
			coordinates.addAll(Mirror.findCoordinates(player, blockPos));
			coordinates.addAll(RadialMirror.findCoordinates(player, blockPos));
			//get mirror for each array coordinate
			for (BlockPos coordinate : arrayCoordinates) {
				coordinates.addAll(Mirror.findCoordinates(player, coordinate));
				coordinates.addAll(RadialMirror.findCoordinates(player, coordinate));
			}
		}

		return coordinates;
	}

	public static List<BlockPos> findCoordinates(PlayerEntity player, BlockPos blockPos) {
		return findCoordinates(player, new ArrayList<>(Collections.singletonList(blockPos)));
	}

	public static List<BlockState> findBlockStates(PlayerEntity player, List<BlockPos> posList, Vector3d hitVec, Direction facing, List<ItemStack> itemStacks) {
		List<BlockState> blockStates = new ArrayList<>();
		itemStacks.clear();

		//Get itemstack
		ItemStack itemStack = player.getItemInHand(Hand.MAIN_HAND);
		if (itemStack.isEmpty() || !CompatHelper.isItemBlockProxy(itemStack)) {
			itemStack = player.getItemInHand(Hand.OFF_HAND);
		}
		if (itemStack.isEmpty() || !CompatHelper.isItemBlockProxy(itemStack)) {
			return blockStates;
		}

		//Get ItemBlock stack
		ItemStack itemBlock = ItemStack.EMPTY;
		if (itemStack.getItem() instanceof BlockItem) itemBlock = itemStack;
		else itemBlock = CompatHelper.getItemBlockFromStack(itemStack);
		AbstractRandomizerBagItem.resetRandomness();

		//Add blocks in posList first
		for (BlockPos blockPos : posList) {
			if (!(itemStack.getItem() instanceof BlockItem)) itemBlock = CompatHelper.getItemBlockFromStack(itemStack);
			BlockState blockState = getBlockStateFromItem(itemBlock, player, blockPos, facing, hitVec, Hand.MAIN_HAND);
			blockStates.add(blockState);
			itemStacks.add(itemBlock);
		}

		for (BlockPos blockPos : posList) {
			BlockState blockState = getBlockStateFromItem(itemBlock, player, blockPos, facing, hitVec, Hand.MAIN_HAND);

			List<BlockState> arrayBlockStates = Array.findBlockStates(player, blockPos, blockState, itemStack, itemStacks);
			blockStates.addAll(arrayBlockStates);
			blockStates.addAll(Mirror.findBlockStates(player, blockPos, blockState, itemStack, itemStacks));
			blockStates.addAll(RadialMirror.findBlockStates(player, blockPos, blockState, itemStack, itemStacks));
			//add mirror for each array coordinate
			List<BlockPos> arrayCoordinates = Array.findCoordinates(player, blockPos);
			for (int i = 0; i < arrayCoordinates.size(); i++) {
				BlockPos coordinate = arrayCoordinates.get(i);
				BlockState blockState1 = arrayBlockStates.get(i);
				blockStates.addAll(Mirror.findBlockStates(player, coordinate, blockState1, itemStack, itemStacks));
				blockStates.addAll(RadialMirror.findBlockStates(player, coordinate, blockState1, itemStack, itemStacks));
			}

			//Adjust blockstates for torches and ladders etc to place on a valid side
			//TODO optimize findCoordinates (done twice now)
			//TODO fix mirror
//            List<BlockPos> coordinates = findCoordinates(player, startPos);
//            for (int i = 0; i < blockStates.size(); i++) {
//                blockStates.set(i, blockStates.get(i).getBlock().getStateForPlacement(player.world, coordinates.get(i), facing,
//                        (float) hitVec.x, (float) hitVec.y, (float) hitVec.z, itemStacks.get(i).getMetadata(), player, EnumHand.MAIN_HAND));
//            }
		}

		return blockStates;
	}

	public static boolean isEnabled(ModifierSettingsManager.ModifierSettings modifierSettings, BlockPos startPos) {
		return Mirror.isEnabled(modifierSettings.getMirrorSettings(), startPos) ||
			Array.isEnabled(modifierSettings.getArraySettings()) ||
			RadialMirror.isEnabled(modifierSettings.getRadialMirrorSettings(), startPos) ||
			modifierSettings.doQuickReplace();
	}

	public static BlockState getBlockStateFromItem(ItemStack itemStack, PlayerEntity player, BlockPos blockPos, Direction facing, Vector3d hitVec, Hand hand) {
		return Block.byItem(itemStack.getItem()).getStateForPlacement(new BlockItemUseContext(new ItemUseContext(player, hand, new BlockRayTraceResult(hitVec, facing, blockPos, false))));
	}

	//Returns true if equal (or both null)
	public static boolean compareCoordinates(List<BlockPos> coordinates1, List<BlockPos> coordinates2) {
		if (coordinates1 == null && coordinates2 == null) return true;
		if (coordinates1 == null || coordinates2 == null) return false;

		//Check count, not actual values
		if (coordinates1.size() == coordinates2.size()) {
			if (coordinates1.size() == 1) {
				return coordinates1.get(0).equals(coordinates2.get(0));
			}
			return true;
		} else {
			return false;
		}

//        return coordinates1.equals(coordinates2);
	}
}
