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

public class Wall extends TwoClicksBuildMode {

	public static BlockPos findWall(PlayerEntity player, BlockPos firstPos, boolean skipRaytrace) {
		Vector3d look = BuildModes.getPlayerLookVec(player);
		Vector3d start = new Vector3d(player.getX(), player.getY() + player.getEyeHeight(), player.getZ());

		List<Criteria> criteriaList = new ArrayList<>(3);

		//X
		Vector3d xBound = BuildModes.findXBound(firstPos.getX(), start, look);
		criteriaList.add(new Criteria(xBound, firstPos, start, look));

		//Z
		Vector3d zBound = BuildModes.findZBound(firstPos.getZ(), start, look);
		criteriaList.add(new Criteria(zBound, firstPos, start, look));

		//Remove invalid criteria
		int reach = ReachHelper.getPlacementReach(player) * 4; //4 times as much as normal placement reach
		criteriaList.removeIf(criteria -> !criteria.isValid(start, look, reach, player, skipRaytrace));

		//If none are valid, return empty list of blocks
		if (criteriaList.isEmpty()) return null;

		//If only 1 is valid, choose that one
		Criteria selected = criteriaList.get(0);

		//If multiple are valid, choose based on criteria
		if (criteriaList.size() > 1) {
			//Select the one that is closest
			//Limit the angle to not be too extreme
			for (int i = 1; i < criteriaList.size(); i++) {
				Criteria criteria = criteriaList.get(i);
				if (criteria.distToPlayerSq < selected.distToPlayerSq && Math.abs(criteria.angle) - Math.abs(selected.angle) < 3)
					selected = criteria;
			}
		}

		return new BlockPos(selected.planeBound);
	}

	public static List<BlockPos> getWallBlocks(PlayerEntity player, int x1, int y1, int z1, int x2, int y2, int z2) {
		List<BlockPos> list = new ArrayList<>();

		if (x1 == x2) {
			if (ModeOptions.getFill() == ModeOptions.ActionEnum.FULL)
				addXWallBlocks(list, x1, y1, y2, z1, z2);
			else
				addXHollowWallBlocks(list, x1, y1, y2, z1, z2);
		} else {
			if (ModeOptions.getFill() == ModeOptions.ActionEnum.FULL)
				addZWallBlocks(list, x1, x2, y1, y2, z1);
			else
				addZHollowWallBlocks(list, x1, x2, y1, y2, z1);
		}

		return list;
	}

	public static void addXWallBlocks(List<BlockPos> list, int x, int y1, int y2, int z1, int z2) {

		for (int z = z1; z1 < z2 ? z <= z2 : z >= z2; z += z1 < z2 ? 1 : -1) {

			for (int y = y1; y1 < y2 ? y <= y2 : y >= y2; y += y1 < y2 ? 1 : -1) {
				list.add(new BlockPos(x, y, z));
			}
		}
	}

	public static void addZWallBlocks(List<BlockPos> list, int x1, int x2, int y1, int y2, int z) {

		for (int x = x1; x1 < x2 ? x <= x2 : x >= x2; x += x1 < x2 ? 1 : -1) {

			for (int y = y1; y1 < y2 ? y <= y2 : y >= y2; y += y1 < y2 ? 1 : -1) {
				list.add(new BlockPos(x, y, z));
			}
		}
	}

	public static void addXHollowWallBlocks(List<BlockPos> list, int x, int y1, int y2, int z1, int z2) {
		Line.addZLineBlocks(list, z1, z2, x, y1);
		Line.addZLineBlocks(list, z1, z2, x, y2);
		Line.addYLineBlocks(list, y1, y2, x, z1);
		Line.addYLineBlocks(list, y1, y2, x, z2);
	}

	public static void addZHollowWallBlocks(List<BlockPos> list, int x1, int x2, int y1, int y2, int z) {
		Line.addXLineBlocks(list, x1, x2, y1, z);
		Line.addXLineBlocks(list, x1, x2, y2, z);
		Line.addYLineBlocks(list, y1, y2, x1, z);
		Line.addYLineBlocks(list, y1, y2, x2, z);
	}

	@Override
	protected BlockPos findSecondPos(PlayerEntity player, BlockPos firstPos, boolean skipRaytrace) {
		return findWall(player, firstPos, skipRaytrace);
	}

	@Override
	protected List<BlockPos> getAllBlocks(PlayerEntity player, int x1, int y1, int z1, int x2, int y2, int z2) {
		return getWallBlocks(player, x1, y1, z1, x2, y2, z2);
	}

	static class Criteria {
		Vector3d planeBound;
		double distToPlayerSq;
		double angle;

		Criteria(Vector3d planeBound, BlockPos firstPos, Vector3d start, Vector3d look) {
			this.planeBound = planeBound;
			this.distToPlayerSq = this.planeBound.subtract(start).lengthSqr();
			Vector3d wall = this.planeBound.subtract(Vector3d.atLowerCornerOf(firstPos));
			this.angle = wall.x * look.x + wall.z * look.z; //dot product ignoring y (looking up/down should not affect this angle)
		}

		//check if its not behind the player and its not too close and not too far
		//also check if raytrace from player to block does not intersect blocks
		public boolean isValid(Vector3d start, Vector3d look, int reach, PlayerEntity player, boolean skipRaytrace) {

			return BuildModes.isCriteriaValid(start, look, reach, player, skipRaytrace, planeBound, planeBound, distToPlayerSq);
		}
	}
}
