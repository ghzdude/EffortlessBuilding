package nl.requios.effortlessbuilding.utilities;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;

import java.util.List;

//Server only
@Deprecated
public class UndoRedoBlockSet {
	private final List<BlockPos> coordinates;
	private final List<BlockState> previousBlockStates;
	private final List<BlockState> newBlockStates;
	private final BlockPos firstPos;
	private final BlockPos secondPos;

	public UndoRedoBlockSet(List<BlockPos> coordinates, List<BlockState> previousBlockStates, List<BlockState> newBlockStates,
							BlockPos firstPos, BlockPos secondPos) {
		this.coordinates = coordinates;
		this.previousBlockStates = previousBlockStates;
		this.newBlockStates = newBlockStates;
		this.firstPos = firstPos;
		this.secondPos = secondPos;
	}

	public List<BlockPos> getCoordinates() {
		return coordinates;
	}

	public List<BlockState> getPreviousBlockStates() {
		return previousBlockStates;
	}

	public List<BlockState> getNewBlockStates() {
		return newBlockStates;
	}

	public BlockPos getFirstPos() {
		return firstPos;
	}

	public BlockPos getSecondPos() {
		return secondPos;
	}
}
