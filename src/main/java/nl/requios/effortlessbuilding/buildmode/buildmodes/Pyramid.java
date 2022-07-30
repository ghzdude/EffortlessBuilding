package nl.requios.effortlessbuilding.buildmode.buildmodes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import nl.requios.effortlessbuilding.buildmode.ModeOptions;
import nl.requios.effortlessbuilding.buildmode.ThreeClicksBuildMode;
import nl.requios.effortlessbuilding.helper.ReachHelper;

import java.util.ArrayList;
import java.util.List;

public class Pyramid extends ThreeClicksBuildMode {

	@Override
	protected BlockPos findSecondPos(Player player, BlockPos firstPos, boolean skipRaytrace) {
		return Floor.findFloor(player, firstPos, skipRaytrace);
	}

	@Override
	protected BlockPos findThirdPos(Player player, BlockPos firstPos, BlockPos secondPos, boolean skipRaytrace) {
		return findHeight(player, secondPos, skipRaytrace);
	}

	@Override
	protected List<BlockPos> getIntermediateBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2) {
		return Floor.getFloorBlocks(player, x1, y1, z1, x2, y2, z2);
	}

	@Override
	protected List<BlockPos> getFinalBlocks(Player player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
		//TODO
		return SlopeFloor.getSlopeFloorBlocks(player, x1, y1, z1, x2, y2, z2, x3, y3, z3);
	}
}