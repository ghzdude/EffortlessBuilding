package nl.requios.effortlessbuilding.buildmode.buildmodes;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.TwoClicksBuildMode;
import nl.requios.effortlessbuilding.helper.ReachHelper;

import java.util.ArrayList;
import java.util.List;

public class Line extends TwoClicksBuildMode {

	public static BlockPos findLine(PlayerEntity player, BlockPos firstPos, boolean skipRaytrace) {
		Vector3d look = BuildModes.getPlayerLookVec(player);
		Vector3d start = new Vector3d(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());

		List<Criteria> criteriaList = new ArrayList<>(3);

		//X
		Vector3d xBound = BuildModes.findXBound(firstPos.getX(), start, look);
		criteriaList.add(new Criteria(xBound, firstPos, start));

		//Y
		Vector3d yBound = BuildModes.findYBound(firstPos.getY(), start, look);
		criteriaList.add(new Criteria(yBound, firstPos, start));

		//Z
		Vector3d zBound = BuildModes.findZBound(firstPos.getZ(), start, look);
		criteriaList.add(new Criteria(zBound, firstPos, start));

		//Remove invalid criteria
		int reach = ReachHelper.getPlacementReach(player) * 4; //4 times as much as normal placement reach
		criteriaList.removeIf(criteria -> !criteria.isValid(start, look, reach, player, skipRaytrace));

		//If none are valid, return empty list of blocks
		if (criteriaList.isEmpty()) return null;

		//If only 1 is valid, choose that one
		Criteria selected = criteriaList.get(0);

		//If multiple are valid, choose based on criteria
		if (criteriaList.size() > 1) {
			//Select the one that is closest (from wall position to its line counterpart)
			for (int i = 1; i < criteriaList.size(); i++) {
				Criteria criteria = criteriaList.get(i);
				if (criteria.distToLineSq < 2.0 && selected.distToLineSq < 2.0) {
					//Both very close to line, choose closest to player
					if (criteria.distToPlayerSq < selected.distToPlayerSq)
						selected = criteria;
				} else {
					//Pick closest to line
					if (criteria.distToLineSq < selected.distToLineSq)
						selected = criteria;
				}
			}

		}

		return new BlockPos(selected.lineBound);
	}

	public static List<BlockPos> getLineBlocks(PlayerEntity player, int x1, int y1, int z1, int x2, int y2, int z2) {
		List<BlockPos> list = new ArrayList<>();

		if (x1 != x2) {
			addXLineBlocks(list, x1, x2, y1, z1);
		} else if (y1 != y2) {
			addYLineBlocks(list, y1, y2, x1, z1);
		} else {
			addZLineBlocks(list, z1, z2, x1, y1);
		}

		return list;
	}

	public static void addXLineBlocks(List<BlockPos> list, int x1, int x2, int y, int z) {
		for (int x = x1; x1 < x2 ? x <= x2 : x >= x2; x += x1 < x2 ? 1 : -1) {
			list.add(new BlockPos(x, y, z));
		}
	}

	public static void addYLineBlocks(List<BlockPos> list, int y1, int y2, int x, int z) {
		for (int y = y1; y1 < y2 ? y <= y2 : y >= y2; y += y1 < y2 ? 1 : -1) {
			list.add(new BlockPos(x, y, z));
		}
	}

	public static void addZLineBlocks(List<BlockPos> list, int z1, int z2, int x, int y) {
		for (int z = z1; z1 < z2 ? z <= z2 : z >= z2; z += z1 < z2 ? 1 : -1) {
			list.add(new BlockPos(x, y, z));
		}
	}

	@Override
	protected BlockPos findSecondPos(PlayerEntity player, BlockPos firstPos, boolean skipRaytrace) {
		return findLine(player, firstPos, skipRaytrace);
	}

	@Override
	protected List<BlockPos> getAllBlocks(PlayerEntity player, int x1, int y1, int z1, int x2, int y2, int z2) {
		return getLineBlocks(player, x1, y1, z1, x2, y2, z2);
	}

	static class Criteria {
		Vector3d planeBound;
		Vector3d lineBound;
		double distToLineSq;
		double distToPlayerSq;

		Criteria(Vector3d planeBound, BlockPos firstPos, Vector3d start) {
			this.planeBound = planeBound;
			this.lineBound = toLongestLine(this.planeBound, firstPos);
			this.distToLineSq = this.lineBound.subtract(this.planeBound).lengthSqr();
			this.distToPlayerSq = this.planeBound.subtract(start).lengthSqr();
		}

		//Make it from a plane into a line
		//Select the axis that is longest
		private Vector3d toLongestLine(Vector3d boundVec, BlockPos firstPos) {
			BlockPos bound = new BlockPos(boundVec);

			BlockPos firstToSecond = bound.subtract(firstPos);
			firstToSecond = new BlockPos(Math.abs(firstToSecond.getX()), Math.abs(firstToSecond.getY()), Math.abs(firstToSecond.getZ()));
			int longest = Math.max(firstToSecond.getX(), Math.max(firstToSecond.getY(), firstToSecond.getZ()));
			if (longest == firstToSecond.getX()) {
				return new Vector3d(bound.getX(), firstPos.getY(), firstPos.getZ());
			}
			if (longest == firstToSecond.getY()) {
				return new Vector3d(firstPos.getX(), bound.getY(), firstPos.getZ());
			}
			if (longest == firstToSecond.getZ()) {
				return new Vector3d(firstPos.getX(), firstPos.getY(), bound.getZ());
			}
			return null;
		}

		//check if its not behind the player and its not too close and not too far
		//also check if raytrace from player to block does not intersect blocks
		public boolean isValid(Vector3d start, Vector3d look, int reach, PlayerEntity player, boolean skipRaytrace) {

			return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, lineBound, planeBound, distToPlayerSq);
		}

	}
}
