package nl.requios.effortlessbuilding.buildmode;

import com.mojang.math.Vector4f;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
	public static Dictionary<Player, Boolean> currentlyBreakingClient = new Hashtable<>();
	public static Dictionary<Player, Boolean> currentlyBreakingServer = new Hashtable<>();

	//Uses a network message to get the previous raytraceresult from the player
	//The server could keep track of all raytraceresults but this might lag with many players
	//Raytraceresult is needed for sideHit and hitVec
	public static void onBlockPlacedMessage(Player player, BlockPlacedMessage message) {

		//Check if not in the middle of breaking
		Dictionary<Player, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
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
			if (buildMode != BuildModeEnum.DISABLED && player.blockPosition().distSqr(startPos) > maxReach * maxReach) {
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

		Vec3 hitVec = buildMode.instance.getHitVec(player);
		if (hitVec == null) hitVec = message.getHitVec();

		BuildModifiers.onBlockPlaced(player, coordinates, sideHit, hitVec, message.getPlaceStartPos());

		//Only works when finishing a buildmode is equal to placing some blocks
		//No intermediate blocks allowed
		currentlyBreaking.remove(player);
	}

	//Use a network message to break blocks in the distance using clientside mouse input
	public static void onBlockBrokenMessage(Player player, BlockBrokenMessage message) {
		BlockPos startPos = message.isBlockHit() ? message.getBlockPos() : null;
		onBlockBroken(player, startPos, true);
	}

	public static void onBlockBroken(Player player, BlockPos startPos, boolean breakStartPos) {

		//Check if not in the middle of placing
		Dictionary<Player, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
		if (currentlyBreaking.get(player) != null && !currentlyBreaking.get(player)) {
			//Cancel placing
			initializeMode(player);
			return;
		}

		if (!ReachHelper.canBreakFar(player)) return;

		//If first click
		if (currentlyBreaking.get(player) == null) {
			//If startpos is null, dont do anything
			if (startPos == null) return;
		}

		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
		ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);

		//Get coordinates
		BuildModeEnum buildMode = modeSettings.getBuildMode();
		List<BlockPos> coordinates = buildMode.instance.onRightClick(player, startPos, Direction.UP, Vec3.ZERO, true);

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

	public static List<BlockPos> findCoordinates(Player player, BlockPos startPos, boolean skipRaytrace) {
		List<BlockPos> coordinates = new ArrayList<>();

		ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
		coordinates.addAll(modeSettings.getBuildMode().instance.findCoordinates(player, startPos, skipRaytrace));

		return coordinates;
	}

	public static void initializeMode(Player player) {
		//Resetting mode, so not placing or breaking
		Dictionary<Player, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
		currentlyBreaking.remove(player);

		ModeSettingsManager.getModeSettings(player).getBuildMode().instance.initialize(player);
	}

	public static boolean isCurrentlyPlacing(Player player) {
		Dictionary<Player, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
		return currentlyBreaking.get(player) != null && !currentlyBreaking.get(player);
	}

	public static boolean isCurrentlyBreaking(Player player) {
		Dictionary<Player, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
		return currentlyBreaking.get(player) != null && currentlyBreaking.get(player);
	}

	//Either placing or breaking
	public static boolean isActive(Player player) {
		Dictionary<Player, Boolean> currentlyBreaking = player.level.isClientSide ? currentlyBreakingClient : currentlyBreakingServer;
		return currentlyBreaking.get(player) != null;
	}

	//Find coordinates on a line bound by a plane
	public static Vec3 findXBound(double x, Vec3 start, Vec3 look) {
		//then y and z are
		double y = (x - start.x) / look.x * look.y + start.y;
		double z = (x - start.x) / look.x * look.z + start.z;

		return new Vec3(x, y, z);
	}

	public static Vec3 findYBound(double y, Vec3 start, Vec3 look) {
		//then x and z are
		double x = (y - start.y) / look.y * look.x + start.x;
		double z = (y - start.y) / look.y * look.z + start.z;

		return new Vec3(x, y, z);
	}

	public static Vec3 findZBound(double z, Vec3 start, Vec3 look) {
		//then x and y are
		double x = (z - start.z) / look.z * look.x + start.x;
		double y = (z - start.z) / look.z * look.y + start.y;

		return new Vec3(x, y, z);
	}

	//Use this instead of player.getLookVec() in any buildmodes code
	public static Vec3 getPlayerLookVec(Player player) {
		Vec3 lookVec = player.getLookAngle();
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

		return new Vec3(x, y, z);
	}

	public static boolean isCriteriaValid(Vec3 start, Vec3 look, int reach, Player player, boolean skipRaytrace, Vec3 lineBound, Vec3 planeBound, double distToPlayerSq) {
		boolean intersects = false;
		if (!skipRaytrace) {
			//collision within a 1 block radius to selected is fine
			ClipContext rayTraceContext = new ClipContext(start, lineBound, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player);
			HitResult rayTraceResult = player.level.clip(rayTraceContext);
			intersects = rayTraceResult != null && rayTraceResult.getType() == HitResult.Type.BLOCK &&
				planeBound.subtract(rayTraceResult.getLocation()).lengthSqr() > 4;
		}

		return planeBound.subtract(start).dot(look) > 0 &&
			distToPlayerSq > 2 && distToPlayerSq < reach * reach &&
			!intersects;
	}

	public enum BuildModeEnum {
		DISABLED("normal", new Disabled(), BuildModeCategoryEnum.BASIC),
		SINGLE("normal_plus", new Single(), BuildModeCategoryEnum.BASIC, OptionEnum.BUILD_SPEED),
		LINE("line", new Line(), BuildModeCategoryEnum.BASIC /*, OptionEnum.THICKNESS*/),
		WALL("wall", new Wall(), BuildModeCategoryEnum.BASIC, OptionEnum.FILL),
		FLOOR("floor", new Floor(), BuildModeCategoryEnum.BASIC, OptionEnum.FILL),
		CUBE("cube", new Cube(), BuildModeCategoryEnum.BASIC, OptionEnum.CUBE_FILL),
		DIAGONAL_LINE("diagonal_line", new DiagonalLine(),  BuildModeCategoryEnum.DIAGONAL /*, OptionEnum.THICKNESS*/),
		DIAGONAL_WALL("diagonal_wall", new DiagonalWall(),  BuildModeCategoryEnum.DIAGONAL /*, OptionEnum.FILL*/),
		SLOPE_FLOOR("slope_floor", new SlopeFloor(), BuildModeCategoryEnum.DIAGONAL, OptionEnum.RAISED_EDGE),
		CIRCLE("circle", new Circle(), BuildModeCategoryEnum.CIRCULAR, OptionEnum.CIRCLE_START, OptionEnum.FILL),
		CYLINDER("cylinder", new Cylinder(), BuildModeCategoryEnum.CIRCULAR, OptionEnum.CIRCLE_START, OptionEnum.FILL),
		SPHERE("sphere", new Sphere(), BuildModeCategoryEnum.CIRCULAR, OptionEnum.CIRCLE_START, OptionEnum.FILL);
//		PYRAMID("pyramid", new Pyramid(), BuildModeCategoryEnum.ROOF),
//		CONE("cone", new Cone(), BuildModeCategoryEnum.ROOF),
//		DOME("dome", new Dome(), BuildModeCategoryEnum.ROOF);

		private final String name;
		public final IBuildMode instance;
		public final BuildModeCategoryEnum category;
		public final OptionEnum[] options;

		BuildModeEnum(String name, IBuildMode instance, BuildModeCategoryEnum category, OptionEnum... options) {
			this.name = name;
			this.instance = instance;
			this.category = category;
			this.options = options;
		}

		public String getNameKey() {
			return "effortlessbuilding.mode." + name;
		}

		public String getDescriptionKey() {
			return "effortlessbuilding.modedescription." + name;
		}
	}

	public enum BuildModeCategoryEnum {
		BASIC(new Vector4f(0f, .5f, 1f, .8f)),
		DIAGONAL(new Vector4f(0.56f, 0.28f, 0.87f, .8f)),
		CIRCULAR(new Vector4f(0.29f, 0.76f, 0.3f, 1f)),
		ROOF(new Vector4f(0.83f, 0.87f, 0.23f, .8f));

		public final Vector4f color;

		BuildModeCategoryEnum(Vector4f color) {
			this.color = color;
		}
	}
}
