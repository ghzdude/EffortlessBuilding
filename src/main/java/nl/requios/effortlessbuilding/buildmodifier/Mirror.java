package nl.requios.effortlessbuilding.buildmodifier;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.Half;
import net.minecraft.state.properties.SlabType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.items.IItemHandler;
import nl.requios.effortlessbuilding.item.ItemRandomizerBag;

import java.util.ArrayList;
import java.util.List;

public class Mirror {

	public static List<BlockPos> findCoordinates(PlayerEntity player, BlockPos startPos) {
		List<BlockPos> coordinates = new ArrayList<>();

		//find mirrorsettings for the player
		MirrorSettings m = ModifierSettingsManager.getModifierSettings(player).getMirrorSettings();
		if (!isEnabled(m, startPos)) return coordinates;

		if (m.mirrorX) coordinateMirrorX(m, startPos, coordinates);
		if (m.mirrorY) coordinateMirrorY(m, startPos, coordinates);
		if (m.mirrorZ) coordinateMirrorZ(m, startPos, coordinates);

		return coordinates;
	}

	private static void coordinateMirrorX(MirrorSettings m, BlockPos oldBlockPos, List<BlockPos> coordinates) {
		//find mirror position
		double x = m.position.x + (m.position.x - oldBlockPos.getX() - 0.5);
		BlockPos newBlockPos = new BlockPos(x, oldBlockPos.getY(), oldBlockPos.getZ());
		coordinates.add(newBlockPos);

		if (m.mirrorY) coordinateMirrorY(m, newBlockPos, coordinates);
		if (m.mirrorZ) coordinateMirrorZ(m, newBlockPos, coordinates);
	}

	private static void coordinateMirrorY(MirrorSettings m, BlockPos oldBlockPos, List<BlockPos> coordinates) {
		//find mirror position
		double y = m.position.y + (m.position.y - oldBlockPos.getY() - 0.5);
		BlockPos newBlockPos = new BlockPos(oldBlockPos.getX(), y, oldBlockPos.getZ());
		coordinates.add(newBlockPos);

		if (m.mirrorZ) coordinateMirrorZ(m, newBlockPos, coordinates);
	}

	private static void coordinateMirrorZ(MirrorSettings m, BlockPos oldBlockPos, List<BlockPos> coordinates) {
		//find mirror position
		double z = m.position.z + (m.position.z - oldBlockPos.getZ() - 0.5);
		BlockPos newBlockPos = new BlockPos(oldBlockPos.getX(), oldBlockPos.getY(), z);
		coordinates.add(newBlockPos);
	}

	public static List<BlockState> findBlockStates(PlayerEntity player, BlockPos startPos, BlockState blockState, ItemStack itemStack, List<ItemStack> itemStacks) {
		List<BlockState> blockStates = new ArrayList<>();

		//find mirrorsettings for the player
		MirrorSettings m = ModifierSettingsManager.getModifierSettings(player).getMirrorSettings();
		if (!isEnabled(m, startPos)) return blockStates;

		//Randomizer bag synergy
		IItemHandler bagInventory = null;
		if (!itemStack.isEmpty() && itemStack.getItem() instanceof ItemRandomizerBag) {
			bagInventory = ItemRandomizerBag.getBagInventory(itemStack);
		}

		if (m.mirrorX)
			blockStateMirrorX(player, m, startPos, blockState, bagInventory, itemStack, Hand.MAIN_HAND, blockStates, itemStacks);
		if (m.mirrorY)
			blockStateMirrorY(player, m, startPos, blockState, bagInventory, itemStack, Hand.MAIN_HAND, blockStates, itemStacks);
		if (m.mirrorZ)
			blockStateMirrorZ(player, m, startPos, blockState, bagInventory, itemStack, Hand.MAIN_HAND, blockStates, itemStacks);

		return blockStates;
	}

	private static void blockStateMirrorX(PlayerEntity player, MirrorSettings m, BlockPos oldBlockPos, BlockState oldBlockState,
										  IItemHandler bagInventory, ItemStack itemStack, Hand hand, List<BlockState> blockStates, List<ItemStack> itemStacks) {
		//find mirror position
		double x = m.position.x + (m.position.x - oldBlockPos.getX() - 0.5);
		BlockPos newBlockPos = new BlockPos(x, oldBlockPos.getY(), oldBlockPos.getZ());

		//Randomizer bag synergy
		if (bagInventory != null) {
			itemStack = ItemRandomizerBag.pickRandomStack(bagInventory);
			oldBlockState = BuildModifiers.getBlockStateFromItem(itemStack, player, oldBlockPos, Direction.UP, new Vector3d(0, 0, 0), hand);
		}

		//Find blockstate
		BlockState newBlockState = oldBlockState == null ? null : oldBlockState.mirror(net.minecraft.util.Mirror.FRONT_BACK);

		//Store blockstate and itemstack
		blockStates.add(newBlockState);
		itemStacks.add(itemStack);

		if (m.mirrorY)
			blockStateMirrorY(player, m, newBlockPos, newBlockState, bagInventory, itemStack, hand, blockStates, itemStacks);
		if (m.mirrorZ)
			blockStateMirrorZ(player, m, newBlockPos, newBlockState, bagInventory, itemStack, hand, blockStates, itemStacks);
	}

	private static void blockStateMirrorY(PlayerEntity player, MirrorSettings m, BlockPos oldBlockPos, BlockState oldBlockState,
										  IItemHandler bagInventory, ItemStack itemStack, Hand hand, List<BlockState> blockStates, List<ItemStack> itemStacks) {
		//find mirror position
		double y = m.position.y + (m.position.y - oldBlockPos.getY() - 0.5);
		BlockPos newBlockPos = new BlockPos(oldBlockPos.getX(), y, oldBlockPos.getZ());

		//Randomizer bag synergy
		if (bagInventory != null) {
			itemStack = ItemRandomizerBag.pickRandomStack(bagInventory);
			oldBlockState = BuildModifiers.getBlockStateFromItem(itemStack, player, oldBlockPos, Direction.UP, new Vector3d(0, 0, 0), hand);
		}

		//Find blockstate
		BlockState newBlockState = oldBlockState == null ? null : getVerticalMirror(oldBlockState);

		//Store blockstate and itemstack
		blockStates.add(newBlockState);
		itemStacks.add(itemStack);

		if (m.mirrorZ)
			blockStateMirrorZ(player, m, newBlockPos, newBlockState, bagInventory, itemStack, hand, blockStates, itemStacks);
	}

	private static void blockStateMirrorZ(PlayerEntity player, MirrorSettings m, BlockPos oldBlockPos, BlockState oldBlockState,
										  IItemHandler bagInventory, ItemStack itemStack, Hand hand, List<BlockState> blockStates, List<ItemStack> itemStacks) {
		//find mirror position
		double z = m.position.z + (m.position.z - oldBlockPos.getZ() - 0.5);
		BlockPos newBlockPos = new BlockPos(oldBlockPos.getX(), oldBlockPos.getY(), z);

		//Randomizer bag synergy
		if (bagInventory != null) {
			itemStack = ItemRandomizerBag.pickRandomStack(bagInventory);
			oldBlockState = BuildModifiers.getBlockStateFromItem(itemStack, player, oldBlockPos, Direction.UP, new Vector3d(0, 0, 0), hand);
		}

		//Find blockstate
		BlockState newBlockState = oldBlockState == null ? null : oldBlockState.mirror(net.minecraft.util.Mirror.LEFT_RIGHT);

		//Store blockstate and itemstack
		blockStates.add(newBlockState);
		itemStacks.add(itemStack);
	}

	public static boolean isEnabled(MirrorSettings m, BlockPos startPos) {
		if (m == null || !m.enabled || (!m.mirrorX && !m.mirrorY && !m.mirrorZ)) return false;

		//within mirror distance
		return !(startPos.getX() + 0.5 < m.position.x - m.radius) && !(startPos.getX() + 0.5 > m.position.x + m.radius) &&
			!(startPos.getY() + 0.5 < m.position.y - m.radius) && !(startPos.getY() + 0.5 > m.position.y + m.radius) &&
			!(startPos.getZ() + 0.5 < m.position.z - m.radius) && !(startPos.getZ() + 0.5 > m.position.z + m.radius);
	}

	private static BlockState getVerticalMirror(BlockState blockState) {
		//Stairs
		if (blockState.getBlock() instanceof StairsBlock) {
			if (blockState.getValue(StairsBlock.HALF) == Half.BOTTOM) {
				return blockState.setValue(StairsBlock.HALF, Half.TOP);
			} else {
				return blockState.setValue(StairsBlock.HALF, Half.BOTTOM);
			}
		}

		//Slabs
		if (blockState.getBlock() instanceof SlabBlock) {
			if (blockState.getValue(SlabBlock.TYPE) == SlabType.DOUBLE) {
				return blockState;
			} else if (blockState.getValue(SlabBlock.TYPE) == SlabType.BOTTOM) {
				return blockState.setValue(SlabBlock.TYPE, SlabType.TOP);
			} else {
				return blockState.setValue(SlabBlock.TYPE, SlabType.BOTTOM);
			}
		}

		//Buttons, endrod, observer, piston
		if (blockState.getBlock() instanceof DirectionalBlock) {
			if (blockState.getValue(DirectionalBlock.FACING) == Direction.DOWN) {
				return blockState.setValue(DirectionalBlock.FACING, Direction.UP);
			} else if (blockState.getValue(DirectionalBlock.FACING) == Direction.UP) {
				return blockState.setValue(DirectionalBlock.FACING, Direction.DOWN);
			}
		}

		//Dispenser, dropper
		if (blockState.getBlock() instanceof DispenserBlock) {
			if (blockState.getValue(DispenserBlock.FACING) == Direction.DOWN) {
				return blockState.setValue(DispenserBlock.FACING, Direction.UP);
			} else if (blockState.getValue(DispenserBlock.FACING) == Direction.UP) {
				return blockState.setValue(DispenserBlock.FACING, Direction.DOWN);
			}
		}

		return blockState;
	}

	public static class MirrorSettings {
		public boolean enabled = false;
		public Vector3d position = new Vector3d(0.5, 64.5, 0.5);
		public boolean mirrorX = true, mirrorY = false, mirrorZ = false;
		public int radius = 10;
		public boolean drawLines = true, drawPlanes = true;

		public MirrorSettings() {
		}

		public MirrorSettings(boolean mirrorEnabled, Vector3d position, boolean mirrorX, boolean mirrorY, boolean mirrorZ, int radius, boolean drawLines, boolean drawPlanes) {
			this.enabled = mirrorEnabled;
			this.position = position;
			this.mirrorX = mirrorX;
			this.mirrorY = mirrorY;
			this.mirrorZ = mirrorZ;
			this.radius = radius;
			this.drawLines = drawLines;
			this.drawPlanes = drawPlanes;
		}

		public int getReach() {
			return radius * 2; //Change ModifierSettings#setReachUpgrade too
		}
	}
}
