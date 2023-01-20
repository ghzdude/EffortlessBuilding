package nl.requios.effortlessbuilding.buildmode;

import com.mojang.math.Vector4f;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.buildmode.buildmodes.*;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.ReachHelper;

import java.util.*;

import static nl.requios.effortlessbuilding.buildmode.ModeOptions.OptionEnum;

@OnlyIn(Dist.CLIENT)
public class BuildModes {

	public void findCoordinates(List<BlockEntry> blocks, Player player, BuildModeEnum buildMode) {
		buildMode.instance.findCoordinates(blocks);

		//Limit number of blocks you can place
		int limit = ReachHelper.getMaxBlocksPlacedAtOnce(player);
		while (blocks.size() > limit) {
			blocks.remove(blocks.size()-1);
		}
	}

	public BuildModeEnum getBuildMode(Player player) {
		return ModeSettingsManager.getModeSettings(player).getBuildMode();
	}

	public void onCancel(Player player) {
		getBuildMode(player).instance.initialize();
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
