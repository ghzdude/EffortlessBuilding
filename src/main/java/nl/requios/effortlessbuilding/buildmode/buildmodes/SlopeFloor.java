package nl.requios.effortlessbuilding.buildmode.buildmodes;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import nl.requios.effortlessbuilding.buildmode.ModeOptions;
import nl.requios.effortlessbuilding.buildmode.ThreeClicksBuildMode;
import nl.requios.effortlessbuilding.helper.ReachHelper;

import java.util.ArrayList;
import java.util.List;

public class SlopeFloor extends ThreeClicksBuildMode {

    @Override
    protected BlockPos findSecondPos(EntityPlayer player, BlockPos firstPos, boolean skipRaytrace) {
        return Floor.findFloor(player, firstPos, skipRaytrace);
    }

    @Override
    protected BlockPos findThirdPos(EntityPlayer player, BlockPos firstPos, BlockPos secondPos, boolean skipRaytrace) {
        return findHeight(player, secondPos, skipRaytrace);
    }

    @Override
    protected List<BlockPos> getIntermediateBlocks(EntityPlayer player, int x1, int y1, int z1, int x2, int y2, int z2) {
        return Floor.getFloorBlocks(player, x1, y1, z1, x2, y2, z2);
    }

    @Override
    protected List<BlockPos> getFinalBlocks(EntityPlayer player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
        return getSlopeFloorBlocks(player, x1, y1, z1, x2, y2, z2, x3, y3, z3);
    }

    //Add slope floor from first to second
    public static List<BlockPos> getSlopeFloorBlocks(EntityPlayer player, int x1, int y1, int z1, int x2, int y2, int z2, int x3, int y3, int z3) {
        List<BlockPos> list = new ArrayList<>();

        int axisLimit = ReachHelper.getMaxBlocksPerAxis(player);

        //Determine whether to use x or z axis to slope up
        boolean onXAxis = true;

        int xLength = Math.abs(x2 - x1);
        int zLength = Math.abs(z2 - z1);

        if (ModeOptions.getRaisedEdge() == ModeOptions.ActionEnum.SHORT_EDGE) {
            //Slope along short edge
            if (zLength > xLength) onXAxis = false;
        } else {
            //Slope along long edge
            if (zLength <= xLength) onXAxis = false;
        }

        if (onXAxis) {
            //Along X goes up

            //Get diagonal line blocks
            List<BlockPos> diagonalLineBlocks = DiagonalLine.getDiagonalLineBlocks(player, x1, y1, z1, x2, y3, z1, 1f);

            //Limit amount of blocks we can place
            int lowest = Math.min(z1, z2);
            int highest = Math.max(z1, z2);

            if (highest - lowest >= axisLimit) highest = lowest + axisLimit - 1;

            //Copy diagonal line on x axis
            for (int z = lowest; z <= highest; z++) {
                for (BlockPos blockPos : diagonalLineBlocks) {
                    list.add(new BlockPos(blockPos.getX(), blockPos.getY(), z));
                }
            }

        } else {
            //Along Z goes up

            //Get diagonal line blocks
            List<BlockPos> diagonalLineBlocks = DiagonalLine.getDiagonalLineBlocks(player, x1, y1, z1, x1, y3, z2, 1f);

            //Limit amount of blocks we can place
            int lowest = Math.min(x1, x2);
            int highest = Math.max(x1, x2);

            if (highest - lowest >= axisLimit) highest = lowest + axisLimit - 1;

            //Copy diagonal line on x axis
            for (int x = lowest; x <= highest; x++) {
                for (BlockPos blockPos : diagonalLineBlocks) {
                    list.add(new BlockPos(x, blockPos.getY(), blockPos.getZ()));
                }
            }
        }

        return list;
    }
}