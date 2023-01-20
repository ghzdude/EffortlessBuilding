package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.ReachHelper;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.List;
import java.util.UUID;

public abstract class TwoClicksBuildMode extends BaseBuildMode {

	protected BlockEntry firstBlockEntry;

	@Override
	public boolean onClick(List<BlockEntry> blocks) {
		super.onClick(blocks);

		if (clicks == 1) {
			//First click, remember starting position

			//If clicking in air, reset and try again
			if (blocks.size() == 0) {
				clicks = 0;
				return false;
			}

			firstBlockEntry = blocks.get(0);
		} else {
			//Second click, place blocks
			clicks = 0;
			return true;
		}
		return false;
	}

	@Override
	public List<BlockPos> findCoordinates(Player player, BlockPos blockPos, boolean skipRaytrace) {
		List<BlockPos> list = new ArrayList<>();
		Dictionary<UUID, Integer> rightClickTable = player.level.isClientSide ? rightClickClientTable : rightClickServerTable;
		int rightClickNr = rightClickTable.get(player.getUUID());
		BlockPos firstPos = firstPosTable.get(player.getUUID());

		if (rightClickNr == 0) {
			if (blockPos != null)
				list.add(blockPos);
		} else {
			BlockPos secondPos = findSecondPos(player, firstPos, skipRaytrace);
			if (secondPos == null) return list;

			//Limit amount of blocks we can place per row
			int axisLimit = ReachHelper.getMaxBlocksPerAxis(player);

			int x1 = firstPos.getX(), x2 = secondPos.getX();
			int y1 = firstPos.getY(), y2 = secondPos.getY();
			int z1 = firstPos.getZ(), z2 = secondPos.getZ();

			//limit axis
			if (x2 - x1 >= axisLimit) x2 = x1 + axisLimit - 1;
			if (x1 - x2 >= axisLimit) x2 = x1 - axisLimit + 1;
			if (y2 - y1 >= axisLimit) y2 = y1 + axisLimit - 1;
			if (y1 - y2 >= axisLimit) y2 = y1 - axisLimit + 1;
			if (z2 - z1 >= axisLimit) z2 = z1 + axisLimit - 1;
			if (z1 - z2 >= axisLimit) z2 = z1 - axisLimit + 1;

			list.addAll(getAllBlocks(player, x1, y1, z1, x2, y2, z2));
		}

		return list;
	}

	//Finds the place of the second block pos based on criteria (floor must be on same height as first click, wall on same plane etc)
	protected abstract BlockPos findSecondPos(Player player, BlockPos firstPos, boolean skipRaytrace);

	//After first and second pos are known, we want all the blocks
	protected abstract List<BlockPos> getAllBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2);
}
