package nl.requios.effortlessbuilding.systems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import nl.requios.effortlessbuilding.CommonConfig;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;

//Common
public class PowerLevel {
	private static final String POWER_LEVEL_KEY = EffortlessBuilding.MODID + ":powerLevel";
	private static final int MAX_POWER_LEVEL = 3;

	public int getPowerLevel(Player player) {
		if (!player.getPersistentData().contains(POWER_LEVEL_KEY)) return 0;
		return player.getPersistentData().getInt(POWER_LEVEL_KEY);
	}

	public void increasePowerLevel(Player player) {
		int powerLevel = getPowerLevel(player);
		if (powerLevel < MAX_POWER_LEVEL) {
			setPowerLevel(player, powerLevel + 1);
		}
	}

	public void setPowerLevel(Player player, int powerLevel) {
		player.getPersistentData().putInt(POWER_LEVEL_KEY, powerLevel);

		if (player.level.isClientSide) {
			EffortlessBuildingClient.BUILD_MODIFIERS.onPowerLevelChanged(powerLevel);
		}
	}

	public int getMaxReach(Player player) {
		if (player.isCreative()) return CommonConfig.reach.creative.get();
		return switch (getPowerLevel(player)) {
			case 1 -> CommonConfig.reach.level1.get();
			case 2 -> CommonConfig.reach.level2.get();
			case 3 -> CommonConfig.reach.level3.get();
			default -> CommonConfig.reach.level0.get();
		};
	}

	public int getPlacementReach(Player player) {
		if (player.isCreative()) return CommonConfig.reach.creative.get();
		return switch (getPowerLevel(player)) {
			case 1 -> CommonConfig.reach.level1.get();
			case 2 -> CommonConfig.reach.level2.get();
			case 3 -> CommonConfig.reach.level3.get();
			default -> CommonConfig.reach.level0.get();
		};
	}

	//How far away we can detect the second and third click of build modes (distance to player)
	public int getBuildModeReach(Player player) {
		//A bit further than placement reach, so you can build lines when looking to the side without having to move.
		return getMaxReach(player) + 6;
	}

	public int getMaxBlocksPlacedAtOnce(Player player) {
		if (player.isCreative()) return CommonConfig.maxBlocksPlacedAtOnce.creative.get();
		return switch (getPowerLevel(player)) {
			case 1 -> CommonConfig.maxBlocksPlacedAtOnce.level1.get();
			case 2 -> CommonConfig.maxBlocksPlacedAtOnce.level2.get();
			case 3 -> CommonConfig.maxBlocksPlacedAtOnce.level3.get();
			default -> CommonConfig.maxBlocksPlacedAtOnce.level0.get();
		};
	}

	public int getMaxBlocksPerAxis(Player player) {
		if (player.isCreative()) return CommonConfig.maxBlocksPerAxis.creative.get();
		return switch (getPowerLevel(player)) {
			case 1 -> CommonConfig.maxBlocksPerAxis.level1.get();
			case 2 -> CommonConfig.maxBlocksPerAxis.level2.get();
			case 3 -> CommonConfig.maxBlocksPerAxis.level3.get();
			default -> CommonConfig.maxBlocksPerAxis.level0.get();
		};
	}

	public int getMaxMirrorRadius(Player player) {
		if (player.isCreative()) return CommonConfig.maxMirrorRadius.creative.get();
		return switch (getPowerLevel(player)) {
			case 1 -> CommonConfig.maxMirrorRadius.level1.get();
			case 2 -> CommonConfig.maxMirrorRadius.level2.get();
			case 3 -> CommonConfig.maxMirrorRadius.level3.get();
			default -> CommonConfig.maxMirrorRadius.level0.get();
		};
	}

	public boolean isDisabled(Player player) {
		return getMaxBlocksPlacedAtOnce(player) <= 0 || getMaxBlocksPerAxis(player) <= 0;
	}

	//Static methods are used by client and server
	public static boolean canBreakFar(Player player) {
		return player.isCreative();
	}

	public static boolean canReplaceBlocks(Player player) {
		return player.isCreative();
	}
}
