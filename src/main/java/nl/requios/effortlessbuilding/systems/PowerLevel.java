package nl.requios.effortlessbuilding.systems;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.CommonConfig;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.PowerLevelPacket;

@OnlyIn(Dist.CLIENT)
public class PowerLevel {

	private int powerLevel;

	public int getPowerLevel() {
		return powerLevel;
	}

	public int getNextPowerLevel() {
		return Math.min(powerLevel + 1, ServerPowerLevel.MAX_POWER_LEVEL);
	}

	public void setPowerLevel(int powerLevel) {
		this.powerLevel = powerLevel;
		EffortlessBuildingClient.BUILD_MODIFIERS.onPowerLevelChanged(powerLevel);
	}

	public boolean canIncreasePowerLevel() {
		return getPowerLevel() < ServerPowerLevel.MAX_POWER_LEVEL;
	}

	public void increasePowerLevel() {
		if (canIncreasePowerLevel()) {
			setPowerLevel(getPowerLevel() + 1);
			PacketHandler.INSTANCE.sendToServer(new PowerLevelPacket(powerLevel));
		}
	}

	@Deprecated
	public int getMaxReach(Player player) {
		return getPlacementReach(player);
	}

	public int getPlacementReach(Player player) {
		return getPlacementReach(player, false);
	}

	public int getPlacementReach(Player player, boolean nextPowerLevel) {
		if (player.isCreative()) return CommonConfig.reach.creative.get();
		return switch (nextPowerLevel ? getNextPowerLevel() : getPowerLevel()) {
			case 1 -> CommonConfig.reach.level1.get();
			case 2 -> CommonConfig.reach.level2.get();
			case 3 -> CommonConfig.reach.level3.get();
			default -> CommonConfig.reach.level0.get();
		};
	}

	//How far away we can detect the second and third click of build modes (distance to player)
	public int getBuildModeReach(Player player) {
		//A bit further than placement reach, so you can build lines when looking to the side without having to move.
		return getPlacementReach(player) + 6;
	}

	public int getMaxBlocksPlacedAtOnce(Player player) {
		return getMaxBlocksPlacedAtOnce(player, false);
	}

	public int getMaxBlocksPlacedAtOnce(Player player, boolean nextPowerLevel) {
		if (player.isCreative()) return CommonConfig.maxBlocksPlacedAtOnce.creative.get();
		return switch (nextPowerLevel ? getNextPowerLevel() : getPowerLevel()) {
			case 1 -> CommonConfig.maxBlocksPlacedAtOnce.level1.get();
			case 2 -> CommonConfig.maxBlocksPlacedAtOnce.level2.get();
			case 3 -> CommonConfig.maxBlocksPlacedAtOnce.level3.get();
			default -> CommonConfig.maxBlocksPlacedAtOnce.level0.get();
		};
	}

	public int getMaxBlocksPerAxis(Player player) {
		return getMaxBlocksPerAxis(player, false);
	}

	public int getMaxBlocksPerAxis(Player player, boolean nextPowerLevel) {
		if (player.isCreative()) return CommonConfig.maxBlocksPerAxis.creative.get();
		return switch (nextPowerLevel ? getNextPowerLevel() : getPowerLevel()) {
			case 1 -> CommonConfig.maxBlocksPerAxis.level1.get();
			case 2 -> CommonConfig.maxBlocksPerAxis.level2.get();
			case 3 -> CommonConfig.maxBlocksPerAxis.level3.get();
			default -> CommonConfig.maxBlocksPerAxis.level0.get();
		};
	}

	public int getMaxMirrorRadius(Player player) {
		return getMaxMirrorRadius(player, false);
	}

	public int getMaxMirrorRadius(Player player, boolean nextPowerLevel) {
		if (player.isCreative()) return CommonConfig.maxMirrorRadius.creative.get();
		return switch (getPowerLevel() + (nextPowerLevel ? 1 : 0)) {
			case 1 -> CommonConfig.maxMirrorRadius.level1.get();
			case 2 -> CommonConfig.maxMirrorRadius.level2.get();
			case 3 -> CommonConfig.maxMirrorRadius.level3.get();
			default -> CommonConfig.maxMirrorRadius.level0.get();
		};
	}

	public boolean isDisabled(Player player) {
		return getMaxBlocksPlacedAtOnce(player) <= 0 || getMaxBlocksPerAxis(player) <= 0;
	}

	public boolean canBreakFar(Player player) {
		return player.isCreative();
	}

	public boolean canReplaceBlocks(Player player) {
		return player.isCreative();
	}
}
