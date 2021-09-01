package nl.requios.effortlessbuilding.buildmodifier;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class BlockSet {
	private final List<BlockPos> coordinates;
	private final List<BlockState> previousBlockStates;
	private final List<BlockState> newBlockStates;
	private final Vec3 hitVec;
	private final BlockPos firstPos;
	private final BlockPos secondPos;

	public BlockSet(List<BlockPos> coordinates, List<BlockState> previousBlockStates, List<BlockState> newBlockStates, Vec3 hitVec,
					BlockPos firstPos, BlockPos secondPos) {
		this.coordinates = coordinates;
		this.previousBlockStates = previousBlockStates;
		this.newBlockStates = newBlockStates;
		this.hitVec = hitVec;
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

	public Vec3 getHitVec() {
		return hitVec;
	}

	public BlockPos getFirstPos() {
		return firstPos;
	}

	public BlockPos getSecondPos() {
		return secondPos;
	}
}
