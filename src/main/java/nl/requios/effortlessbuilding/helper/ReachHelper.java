package nl.requios.effortlessbuilding.helper;

import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import nl.requios.effortlessbuilding.CommonConfig;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;

public class ReachHelper {
	public static int getMaxReach(Player player) {
		if (player.isCreative()) return CommonConfig.reach.maxReachCreative.get();

		if (!CommonConfig.reach.enableReachUpgrades.get()) return CommonConfig.reach.maxReachLevel3.get();

		//Check buildsettings for reachUpgrade
		int reachUpgrade = ModifierSettingsManager.getModifierSettings(player).getReachUpgrade();
		switch (reachUpgrade) {
			case 0:
				return CommonConfig.reach.maxReachLevel0.get();
			case 1:
				return CommonConfig.reach.maxReachLevel1.get();
			case 2:
				return CommonConfig.reach.maxReachLevel2.get();
			case 3:
				return CommonConfig.reach.maxReachLevel3.get();
		}
		return CommonConfig.reach.maxReachLevel0.get();
	}

	public static int getPlacementReach(Player player) {
		return getMaxReach(player) / 4;
	}

	public static int getMaxBlocksPlacedAtOnce(Player player) {
		if (player.isCreative()) return 1000000;
		return Mth.ceil(Math.pow(getMaxReach(player), 1.6));
		//Level 0: 121
		//Level 1: 523
		//Level 2: 1585
		//Level 3: 4805
	}

	public static int getMaxBlocksPerAxis(Player player) {
		if (player.isCreative()) return 2000;
		return Mth.ceil(getMaxReach(player) * 0.3);
		//Level 0: 6
		//Level 1: 15
		//Level 2: 30
		//Level 3: 60
	}

	public static boolean canBreakFar(Player player) {
		return player.isCreative();
	}
}
