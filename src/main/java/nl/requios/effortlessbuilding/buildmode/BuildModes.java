package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.buildmodes.*;
import nl.requios.effortlessbuilding.buildmode.buildmodes.Circle;
import nl.requios.effortlessbuilding.buildmode.buildmodes.Cylinder;
import nl.requios.effortlessbuilding.buildmode.buildmodes.Sphere;
import nl.requios.effortlessbuilding.buildmodifier.*;
import nl.requios.effortlessbuilding.helper.ReachHelper;
import nl.requios.effortlessbuilding.helper.SurvivalHelper;
import nl.requios.effortlessbuilding.network.BlockBrokenMessage;
import nl.requios.effortlessbuilding.network.BlockPlacedMessage;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static nl.requios.effortlessbuilding.buildmode.ModeOptions.*;

public class BuildModes {

    //Static variables are shared between client and server in singleplayer
    //We need them separate
    public static Dictionary<EntityPlayer, Boolean> currentlyBreakingClient = new Hashtable<>();
    public static Dictionary<EntityPlayer, Boolean> currentlyBreakingServer = new Hashtable<>();

    public enum BuildModeEnum {
        NORMAL("effortlessbuilding.mode.normal", new Normal()),
        NORMAL_PLUS("effortlessbuilding.mode.normal_plus", new NormalPlus(), OptionEnum.BUILD_SPEED),
        LINE("effortlessbuilding.mode.line", new Line() /*, OptionEnum.THICKNESS*/),
        WALL("effortlessbuilding.mode.wall", new Wall(), OptionEnum.FILL),
        FLOOR("effortlessbuilding.mode.floor", new Floor(), OptionEnum.FILL),
        DIAGONAL_LINE("effortlessbuilding.mode.diagonal_line", new DiagonalLine() /*, OptionEnum.THICKNESS*/),
        DIAGONAL_WALL("effortlessbuilding.mode.diagonal_wall", new DiagonalWall() /*, OptionEnum.FILL*/),
        SLOPE_FLOOR("effortlessbuilding.mode.slope_floor", new SlopeFloor(), OptionEnum.RAISED_EDGE),
        CIRCLE("effortlessbuilding.mode.circle", new Circle(), OptionEnum.CIRCLE_START, OptionEnum.FILL),
        CYLINDER("effortlessbuilding.mode.cylinder", new Cylinder(), OptionEnum.CIRCLE_START, OptionEnum.FILL),
        SPHERE("effortlessbuilding.mode.sphere", new Sphere(), OptionEnum.CIRCLE_START, OptionEnum.FILL),
        CUBE("effortlessbuilding.mode.cube", new Cube(), OptionEnum.CUBE_FILL);

        public String name;
        public IBuildMode instance;
        public OptionEnum[] options;

        BuildModeEnum(String name, IBuildMode instance, OptionEnum... options) {
            this.name = name;
            this.instance = instance;
            this.options = options;
        }
    }

    //Uses a network message to get the previous raytraceresult from the player
    //The server could keep track of all raytraceresults but this might lag with many players
    //Raytraceresult is needed for sideHit and hitVec
    public static void onBlockPlacedMessage(EntityPlayer player, BlockPlacedMessage message) {

        //Check if not in the middle of breaking
        Dictionary<EntityPlayer, Boolean> currentlyBreaking = player.world.isRemote ? currentlyBreakingClient : currentlyBreakingServer;
        if (currentlyBreaking.get(player) != null && currentlyBreaking.get(player)) {
            //Cancel breaking
            initializeMode(player);
            return;
        }

        ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
        ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
        BuildModeEnum buildMode = modeSettings.getBuildMode();

        BlockPos startPos = null;

        if (message.isBlockHit() && message.getBlockPos() != null) {
            startPos = message.getBlockPos();

            //Offset in direction of sidehit if not quickreplace and not replaceable
            boolean replaceable = player.world.getBlockState(startPos).getBlock().isReplaceable(player.world, startPos);
            boolean becomesDoubleSlab = SurvivalHelper.doesBecomeDoubleSlab(player, startPos, message.getSideHit());
            if (!modifierSettings.doQuickReplace() && !replaceable && !becomesDoubleSlab) {
                startPos = startPos.offset(message.getSideHit());
            }

            //Get under tall grass and other replaceable blocks
            if (modifierSettings.doQuickReplace() && replaceable) {
                startPos = startPos.down();
            }

            //Check if player reach does not exceed startpos
            int maxReach = ReachHelper.getMaxReach(player);
            if (buildMode != BuildModeEnum.NORMAL && player.getPosition().distanceSq(startPos) > maxReach * maxReach) {
                EffortlessBuilding.log(player, "Placement exceeds your reach.");
                return;
            }
        }

        //Even when no starting block is found, call buildmode instance
        //We might want to place things in the air
        List<BlockPos> coordinates = buildMode.instance.onRightClick(player, startPos, message.getSideHit(), message.getHitVec(), modifierSettings.doQuickReplace());

        if (coordinates.isEmpty()) {
            currentlyBreaking.put(player, false);
            return;
        }

        //Limit number of blocks you can place
        int limit = ReachHelper.getMaxBlocksPlacedAtOnce(player);
        if (coordinates.size() > limit) {
            coordinates = coordinates.subList(0, limit);
        }

        EnumFacing sideHit = buildMode.instance.getSideHit(player);
        if (sideHit == null) sideHit = message.getSideHit();

        Vec3d hitVec = buildMode.instance.getHitVec(player);
        if (hitVec == null) hitVec = message.getHitVec();

        BuildModifiers.onBlockPlaced(player, coordinates, sideHit, hitVec, message.getPlaceStartPos());

        //Only works when finishing a buildmode is equal to placing some blocks
        //No intermediate blocks allowed
        currentlyBreaking.remove(player);

    }

    //Use a network message to break blocks in the distance using clientside mouse input
    public static void onBlockBrokenMessage(EntityPlayer player, BlockBrokenMessage message) {
        BlockPos startPos = message.isBlockHit() ? message.getBlockPos() : null;
        onBlockBroken(player, startPos, true);
    }

    public static void onBlockBroken(EntityPlayer player, BlockPos startPos, boolean breakStartPos) {

        //Check if not in the middle of placing
        Dictionary<EntityPlayer, Boolean> currentlyBreaking = player.world.isRemote ? currentlyBreakingClient : currentlyBreakingServer;
        if (currentlyBreaking.get(player) != null && !currentlyBreaking.get(player)) {
            //Cancel placing
            initializeMode(player);
            return;
        }

        //If first click
        if (currentlyBreaking.get(player) == null) {
            //If startpos is null, dont do anything
            if (startPos == null) return;
        }

        ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
        ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);

        //Get coordinates
        BuildModeEnum buildMode = modeSettings.getBuildMode();
        List<BlockPos> coordinates = buildMode.instance.onRightClick(player, startPos, EnumFacing.UP, Vec3d.ZERO, true);

        if (coordinates.isEmpty()) {
            currentlyBreaking.put(player, true);
            return;
        }

        //Let buildmodifiers break blocks
        BuildModifiers.onBlockBroken(player, coordinates, breakStartPos);

        //Only works when finishing a buildmode is equal to breaking some blocks
        //No intermediate blocks allowed
        currentlyBreaking.remove(player);
    }

    public static List<BlockPos> findCoordinates(EntityPlayer player, BlockPos startPos, boolean skipRaytrace) {
        List<BlockPos> coordinates = new ArrayList<>();

        ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
        coordinates.addAll(modeSettings.getBuildMode().instance.findCoordinates(player, startPos, skipRaytrace));

        return coordinates;
    }

    public static void initializeMode(EntityPlayer player) {
        //Resetting mode, so not placing or breaking
        Dictionary<EntityPlayer, Boolean> currentlyBreaking = player.world.isRemote ? currentlyBreakingClient : currentlyBreakingServer;
        currentlyBreaking.remove(player);

        ModeSettingsManager.getModeSettings(player).getBuildMode().instance.initialize(player);
    }

    public static boolean isCurrentlyPlacing(EntityPlayer player) {
        Dictionary<EntityPlayer, Boolean> currentlyBreaking = player.world.isRemote ? currentlyBreakingClient : currentlyBreakingServer;
        return currentlyBreaking.get(player) != null && !currentlyBreaking.get(player);
    }

    public static boolean isCurrentlyBreaking(EntityPlayer player) {
        Dictionary<EntityPlayer, Boolean> currentlyBreaking = player.world.isRemote ? currentlyBreakingClient : currentlyBreakingServer;
        return currentlyBreaking.get(player) != null && currentlyBreaking.get(player);
    }

    //Either placing or breaking
    public static boolean isActive(EntityPlayer player) {
        Dictionary<EntityPlayer, Boolean> currentlyBreaking = player.world.isRemote ? currentlyBreakingClient : currentlyBreakingServer;
        return currentlyBreaking.get(player) != null;
    }


    //Find coordinates on a line bound by a plane
    public static Vec3d findXBound(double x, Vec3d start, Vec3d look) {
        //then y and z are
        double y = (x - start.x) / look.x * look.y + start.y;
        double z = (x - start.x) / look.x * look.z + start.z;

        return new Vec3d(x, y, z);
    }

    public static Vec3d findYBound(double y, Vec3d start, Vec3d look) {
        //then x and z are
        double x = (y - start.y) / look.y * look.x + start.x;
        double z = (y - start.y) / look.y * look.z + start.z;

        return new Vec3d(x, y, z);
    }

    public static Vec3d findZBound(double z, Vec3d start, Vec3d look) {
        //then x and y are
        double x = (z - start.z) / look.z * look.x + start.x;
        double y = (z - start.z) / look.z * look.y + start.y;

        return new Vec3d(x, y, z);
    }

    public static boolean isCriteriaValid(Vec3d start, Vec3d look, int reach, EntityPlayer player, boolean skipRaytrace, Vec3d lineBound, Vec3d planeBound, double distToPlayerSq) {
        boolean intersects = false;
        if (!skipRaytrace) {
            //collision within a 1 block radius to selected is fine
            RayTraceResult rayTraceResult = player.world.rayTraceBlocks(start, lineBound, false, true, false);
            intersects = rayTraceResult != null && rayTraceResult.typeOfHit == RayTraceResult.Type.BLOCK &&
                         planeBound.subtract(rayTraceResult.hitVec).lengthSquared() > 4;
        }

        return planeBound.subtract(start).dotProduct(look) > 0 &&
               distToPlayerSq > 2 && distToPlayerSq < reach * reach &&
               !intersects;
    }

}
