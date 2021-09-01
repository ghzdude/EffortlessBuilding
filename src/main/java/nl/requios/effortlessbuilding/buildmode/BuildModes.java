package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.buildmodes.*;
import nl.requios.effortlessbuilding.buildmodifier.BuildModifiers;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.helper.ReachHelper;
import nl.requios.effortlessbuilding.helper.SurvivalHelper;
import nl.requios.effortlessbuilding.network.BlockBrokenMessage;
import nl.requios.effortlessbuilding.network.BlockPlacedMessage;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import static nl.requios.effortlessbuilding.buildmode.ModeOptions.OptionEnum;

public class BuildModes {

	//Static variables are shared between client and server in singleplayer
	//We need them separate
	public static Dictionary<PlayerEntity, Boolean> currentlyBreakingClient = new Hashtable<>();
	public static Dictionary<PlayerEntity, Boolean> currentlyBreakingServer = new Hashtable<>();

	//Uses a network message to get the previous raytraceresult from the player
	//The server could keep track of all raytraceresults but this might lag with many players
	//Raytraceresult is needed for sideHit and hitVec
	public static void onBlockPlacedMessage(PlayerEntity player, BlockPlacedMessage message) {

		//Check if not in the middle of breaking
		Dictionary<PlayerEntity, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
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
			//TODO 1.13 replaceable
			boolean replaceable = player.level.getBlockState(startPos).getMaterial().isReplaceable();
			boolean becomesDoubleSlab = SurvivalHelper.doesBecomeDoubleSlab(player, startPos, message.getSideHit());
			if (!modifierSettings.doQuickReplace() && !replaceable && !becomesDoubleSlab) {
				startPos = startPos.relative(message.getSideHit());
			}

			//Get under tall grass and other replaceable blocks
			if (modifierSettings.doQuickReplace() && replaceable) {
				startPos = startPos.below();
			}

			//Check if player reach does not exceed startpos
			int maxReach = ReachHelper.getMaxReach(player);
			if (buildMode != BuildModeEnum.NORMAL && player.blockPosition().distSqr(startPos) > maxReach * maxReach) {
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

		Direction sideHit = buildMode.instance.getSideHit(player);
		if (sideHit == null) sideHit = message.getSideHit();

		Vector3d hitVec = buildMode.instance.getHitVec(player);
		if (hitVec == null) hitVec = message.getHitVec();

		BuildModifiers.onBlockPlaced(player, coordinates, sideHit, hitVec, message.getPlaceStartPos());

		//Only works when finishing a buildmode is equal to placing some blocks
		//No intermediate blocks allowed
		currentlyBreaking.remove(player);

	}

	//Use a network message to break blocks in the distance using clientside mouse input
	public static void onBlockBrokenMessage(PlayerEntity player, BlockBrokenMessage message) {
		BlockPos startPos = message.isBlockHit() ? message.getBlockPos() : null;
		onBlockBroken(player, startPos, true);
	}

	public static void onBlockBroken(PlayerEntity player, BlockPos startPos, boolean breakStartPos) {

		//Check if not in the middle of placing
		Dictionary<PlayerEntity, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
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
		List<BlockPos> coordinates = buildMode.instance.onRightClick(player, startPos, Direction.UP, Vector3d.ZERO, true);

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

	public static List<BlockPos> findCoordinates(PlayerEntity player, BlockPos startPos, boolean skipRaytrace) {
		List<BlockPos> coordinates = new ArrayList<>();

		ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
		coordinates.addAll(modeSettings.getBuildMode().instance.findCoordinates(player, startPos, skipRaytrace));

		return coordinates;
	}

	public static void initializeMode(PlayerEntity player) {
		//Resetting mode, so not placing or breaking
		Dictionary<PlayerEntity, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
		currentlyBreaking.remove(player);

		ModeSettingsManager.getModeSettings(player).getBuildMode().instance.initialize(player);
	}

	public static boolean isCurrentlyPlacing(PlayerEntity player) {
		Dictionary<PlayerEntity, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
		return currentlyBreaking.get(player) != null && !currentlyBreaking.get(player);
	}

	public static boolean isCurrentlyBreaking(PlayerEntity player) {
		Dictionary<PlayerEntity, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
		return currentlyBreaking.get(player) != null && currentlyBreaking.get(player);
	}

	//Either placing or breaking
	public static boolean isActive(PlayerEntity player) {
		Dictionary<PlayerEntity, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
		return currentlyBreaking.get(player) != null;
	}

	//Find coordinates on a line bound by a plane
	public static Vector3d findXBound(double x, Vector3d start, Vector3d look) {
		//then y and z are
		double y = (x - start.x) / look.x * look.y + start.y;
		double z = (x - start.x) / look.x * look.z + start.z;

		return new Vector3d(x, y, z);
	}


	//-- Common build mode functionality --//

	public static Vector3d findYBound(double y, Vector3d start, Vector3d look) {
		//then x and z are
		double x = (y - start.y) / look.y * look.x + start.x;
		double z = (y - start.y) / look.y * look.z + start.z;

		return new Vector3d(x, y, z);
	}

	public static Vector3d findZBound(double z, Vector3d start, Vector3d look) {
		//then x and y are
		double x = (z - start.z) / look.z * look.x + start.x;
		double y = (z - start.z) / look.z * look.y + start.y;

		return new Vector3d(x, y, z);
	}

	//Use this instead of player.getLookVec() in any buildmodes code
	public static Vector3d getPlayerLookVec(PlayerEntity player) {
		Vector3d lookVec = player.getLookAngle();
		double x = lookVec.x;
		double y = lookVec.y;
		double z = lookVec.z;

		//Further calculations (findXBound etc) don't like any component being 0 or 1 (e.g. dividing by 0)
		//isCriteriaValid below will take up to 2 minutes to raytrace blocks towards infinity if that is the case
		//So make sure they are close to but never exactly 0 or 1
		if (Math.abs(x) < 0.0001) x = 0.0001;
		if (Math.abs(x - 1.0) < 0.0001) x = 0.9999;
		if (Math.abs(x + 1.0) < 0.0001) x = -0.9999;

		if (Math.abs(y) < 0.0001) y = 0.0001;
		if (Math.abs(y - 1.0) < 0.0001) y = 0.9999;
		if (Math.abs(y + 1.0) < 0.0001) y = -0.9999;

		if (Math.abs(z) < 0.0001) z = 0.0001;
		if (Math.abs(z - 1.0) < 0.0001) z = 0.9999;
		if (Math.abs(z + 1.0) < 0.0001) z = -0.9999;

		return new Vector3d(x, y, z);
	}

	public static boolean isCriteriaValid(Vector3d start, Vector3d look, int reach, PlayerEntity player, boolean skipRaytrace, Vector3d lineBound, Vector3d planeBound, double distToPlayerSq) {
		boolean intersects = false;
		if (!skipRaytrace) {
			//collision within a 1 block radius to selected is fine
			RayTraceContext rayTraceContext = new RayTraceContext(start, lineBound, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, player);
			RayTraceResult rayTraceResult = player.level.clip(rayTraceContext);
			intersects = rayTraceResult != null && rayTraceResult.getType() == RayTraceResult.Type.BLOCK &&
				planeBound.subtract(rayTraceResult.getLocation()).lengthSqr() > 4;
		}

		return planeBound.subtract(start).dot(look) > 0 &&
			distToPlayerSq > 2 && distToPlayerSq < reach * reach &&
			!intersects;
	}

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
}
