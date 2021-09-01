package nl.requios.effortlessbuilding.buildmode.buildmodes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeOptions;
import nl.requios.effortlessbuilding.buildmode.TwoClicksBuildMode;
import nl.requios.effortlessbuilding.helper.ReachHelper;

import java.util.ArrayList;
import java.util.List;

public class Floor extends TwoClicksBuildMode {

	public static BlockPos findFloor(PlayerEntity player, BlockPos firstPos, boolean skipRaytrace) {
		Vector3d look = BuildModes.getPlayerLookVec(player);
		Vector3d start = new Vector3d(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());

		List<Criteria> criteriaList = new ArrayList<>(3);

		//Y
		Vector3d yBound = BuildModes.findYBound(firstPos.getY(), start, look);
		criteriaList.add(new Criteria(yBound, start));

		//Remove invalid criteria
		int reach = ReachHelper.getPlacementReach(player) * 4; //4 times as much as normal placement reach
		criteriaList.removeIf(criteria -> !criteria.isValid(start, look, reach, player, skipRaytrace));

		//If none are valid, return empty list of blocks
		if (criteriaList.isEmpty()) return null;

		//Then only 1 can be valid, return that one
		Criteria selected = criteriaList.get(0);

		return new BlockPos(selected.planeBound);
	}

	public static List<BlockPos> getFloorBlocks(PlayerEntity player, int x1, int y1, int z1, int x2, int y2, int z2) {
		List<BlockPos> list = new ArrayList<>();

		if (ModeOptions.getFill() == ModeOptions.ActionEnum.FULL)
			addFloorBlocks(list, x1, x2, y1, z1, z2);
		else
			addHollowFloorBlocks(list, x1, x2, y1, z1, z2);

		return list;
	}

	public static void addFloorBlocks(List<BlockPos> list, int x1, int x2, int y, int z1, int z2) {

		for (int l = x1; x1 < x2 ? l <= x2 : l >= x2; l += x1 < x2 ? 1 : -1) {

			for (int n = z1; z1 < z2 ? n <= z2 : n >= z2; n += z1 < z2 ? 1 : -1) {

				list.add(new BlockPos(l, y, n));
			}
		}
	}

	public static void addHollowFloorBlocks(List<BlockPos> list, int x1, int x2, int y, int z1, int z2) {
		Line.addXLineBlocks(list, x1, x2, y, z1);
		Line.addXLineBlocks(list, x1, x2, y, z2);
		Line.addZLineBlocks(list, z1, z2, x1, y);
		Line.addZLineBlocks(list, z1, z2, x2, y);
	}

	@Override
	protected BlockPos findSecondPos(PlayerEntity player, BlockPos firstPos, boolean skipRaytrace) {
		return findFloor(player, firstPos, skipRaytrace);
	}

	@Override
	protected List<BlockPos> getAllBlocks(PlayerEntity player, int x1, int y1, int z1, int x2, int y2, int z2) {
		return getFloorBlocks(player, x1, y1, z1, x2, y2, z2);
	}

	static class Criteria {
		Vector3d planeBound;
		double distToPlayerSq;

		Criteria(Vector3d planeBound, Vector3d start) {
			this.planeBound = planeBound;
			this.distToPlayerSq = this.planeBound.subtract(start).lengthSqr();
		}

		//check if its not behind the player and its not too close and not too far
		//also check if raytrace from player to block does not intersect blocks
		public boolean isValid(Vector3d start, Vector3d look, int reach, PlayerEntity player, boolean skipRaytrace) {

			return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, planeBound, planeBound, distToPlayerSq);
		}
	}
}
