package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import nl.requios.effortlessbuilding.helper.ReachHelper;

import java.util.*;

public class Line implements IBuildMode {
    //In singleplayer client and server variables are shared
    //Split everything that needs separate values and may not be called twice in one click
    Dictionary<UUID, Integer> rightClickClientTable = new Hashtable<>();
    Dictionary<UUID, Integer> rightClickServerTable = new Hashtable<>();
    Dictionary<UUID, BlockPos> firstPosTable = new Hashtable<>();
    Dictionary<UUID, EnumFacing> sideHitTable = new Hashtable<>();
    Dictionary<UUID, Vec3d> hitVecTable = new Hashtable<>();

    class Criteria {
        Vec3d planeBound;
        Vec3d lineBound;
        double distToLineSq;
        double distToPlayerSq;

        Criteria(Vec3d planeBound, BlockPos firstPos, Vec3d start) {
            this.planeBound = planeBound;
            this.lineBound = toLongestLine(this.planeBound, firstPos);
            this.distToLineSq = this.lineBound.subtract(this.planeBound).lengthSquared();
            this.distToPlayerSq = this.planeBound.subtract(start).lengthSquared();
        }

        //Make it from a plane into a line
        //Select the axis that is longest
        private Vec3d toLongestLine(Vec3d boundVec, BlockPos firstPos) {
            BlockPos bound = new BlockPos(boundVec);

            BlockPos firstToSecond = bound.subtract(firstPos);
            firstToSecond = new BlockPos(Math.abs(firstToSecond.getX()), Math.abs(firstToSecond.getY()), Math.abs(firstToSecond.getZ()));
            int longest = Math.max(firstToSecond.getX(), Math.max(firstToSecond.getY(), firstToSecond.getZ()));
            if (longest == firstToSecond.getX()) {
                return new Vec3d(bound.getX(), firstPos.getY(), firstPos.getZ());
            }
            if (longest == firstToSecond.getY()) {
                return new Vec3d(firstPos.getX(), bound.getY(), firstPos.getZ());
            }
            if (longest == firstToSecond.getZ()) {
                return new Vec3d(firstPos.getX(), firstPos.getY(), bound.getZ());
            }
            return null;
        }

        //check if its not behind the player and its not too close and not too far
        //also check if raytrace from player to block does not intersect blocks
        public boolean isValid(Vec3d start, Vec3d look, int reach, EntityPlayer player, boolean skipRaytrace) {

            boolean intersects = false;
            if (!skipRaytrace) {
                //collision within a 1 block radius to selected is fine
                RayTraceResult rayTraceResult = player.world.rayTraceBlocks(start, lineBound, false, true, false);
                intersects = rayTraceResult != null && rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK &&
                             planeBound.subtract(rayTraceResult.hitVec).lengthSquared() > 4;
            }

            return planeBound.subtract(start).dotProduct(look) > 0 &&
                   distToPlayerSq > 4 && distToPlayerSq < reach * reach &&
                   !intersects;
        }
    }

    @Override
    public void initialize(EntityPlayer player) {
        rightClickClientTable.put(player.getUniqueID(), 0);
        rightClickServerTable.put(player.getUniqueID(), 0);
        firstPosTable.put(player.getUniqueID(), BlockPos.ORIGIN);
        sideHitTable.put(player.getUniqueID(), EnumFacing.UP);
        hitVecTable.put(player.getUniqueID(), Vec3d.ZERO);
    }

    @Override
    public List<BlockPos> onRightClick(EntityPlayer player, BlockPos blockPos, EnumFacing sideHit, Vec3d hitVec, boolean skipRaytrace) {
        List<BlockPos> list = new ArrayList<>();

        Dictionary<UUID, Integer> rightClickTable = player.world.isRemote ? rightClickClientTable : rightClickServerTable;
        int rightClickNr = rightClickTable.get(player.getUniqueID());
        rightClickNr++;
        rightClickTable.put(player.getUniqueID(), rightClickNr);

        if (rightClickNr == 1) {
            //If clicking in air, reset and try again
            if (blockPos == null) {
                rightClickTable.put(player.getUniqueID(), 0);
                return list;
            }

            //First click, remember starting position
            firstPosTable.put(player.getUniqueID(), blockPos);
            sideHitTable.put(player.getUniqueID(), sideHit);
            hitVecTable.put(player.getUniqueID(), hitVec);
            //Keep list empty, dont place any blocks yet
        } else {
            //Second click, place wall

            list = findCoordinates(player, blockPos, skipRaytrace);
            rightClickTable.put(player.getUniqueID(), 0);
        }

        return list;
    }

    @Override
    public List<BlockPos> findCoordinates(EntityPlayer player, BlockPos blockPos, boolean skipRaytrace) {
        List<BlockPos> list = new ArrayList<>();
        Dictionary<UUID, Integer> rightClickTable = player.world.isRemote ? rightClickClientTable : rightClickServerTable;
        int rightClickNr = rightClickTable.get(player.getUniqueID());
        BlockPos firstPos = firstPosTable.get(player.getUniqueID());

        if (rightClickNr == 0) {
            if (blockPos != null)
                list.add(blockPos);
        } else {
            BlockPos secondPos = findLine(player, firstPos, skipRaytrace);
            if (secondPos == null) return list;

            //Limit amount of blocks we can place
            int axisLimit = ReachHelper.getMaxBlocksPerAxis(player);

            //Add whole line
            int x1 = firstPos.getX(), x2 = secondPos.getX();
            int y1 = firstPos.getY(), y2 = secondPos.getY();
            int z1 = firstPos.getZ(), z2 = secondPos.getZ();

            outerloop:
            for (int l = x1; x1 < x2 ? l <= x2 : l >= x2; l += x1 < x2 ? 1 : -1) {

                for (int n = z1; z1 < z2 ? n <= z2 : n >= z2; n += z1 < z2 ? 1 : -1) {

                    for (int m = y1; y1 < y2 ? m <= y2 : m >= y2; m += y1 < y2 ? 1 : -1) {
                        if (list.size() >= axisLimit) break outerloop;
                        list.add(new BlockPos(l, m, n));
                    }
                }
            }
        }

        return list;
    }

    public BlockPos findLine(EntityPlayer player, BlockPos firstPos, boolean skipRaytrace) {
        Vec3d look = player.getLookVec();
        Vec3d start = new Vec3d(player.posX, player.posY + player.getEyeHeight(), player.posZ);

        List<Criteria> criteriaList = new ArrayList<>(3);

        //X
        Vec3d xBound = BuildModes.findXBound(firstPos.getX(), start, look);
        criteriaList.add(new Criteria(xBound, firstPos, start));

        //Y
        Vec3d yBound = BuildModes.findYBound(firstPos.getY(), start, look);
        criteriaList.add(new Criteria(yBound, firstPos, start));

        //Z
        Vec3d zBound = BuildModes.findZBound(firstPos.getZ(), start, look);
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
                if (criteria.distToLineSq < selected.distToLineSq)
                    selected = criteria;
            }

            //TODO: if multiple are very close, choose closest to player

        }

        return new BlockPos(selected.lineBound);
    }


    @Override
    public EnumFacing getSideHit(EntityPlayer player) {
        return sideHitTable.get(player.getUniqueID());
    }

    @Override
    public Vec3d getHitVec(EntityPlayer player) {
        return hitVecTable.get(player.getUniqueID());
    }
}
