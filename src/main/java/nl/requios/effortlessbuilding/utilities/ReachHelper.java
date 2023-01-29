package nl.requios.effortlessbuilding.utilities;

import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Mth;
import nl.requios.effortlessbuilding.CommonConfig;
import nl.requios.effortlessbuilding.EffortlessBuilding;

//Common
public class ReachHelper {
	private static final String REACH_UPGRADE_KEY = EffortlessBuilding.MODID + ":reachUpgrade";

	public static int getReachUpgrade(Player player) {
		if (!player.getPersistentData().contains(REACH_UPGRADE_KEY)) return 0;
		return player.getPersistentData().getInt(REACH_UPGRADE_KEY);
	}

	//Remember that to actually save it, this needs to be called on the server
	public static void setReachUpgrade(Player player, int reachUpgrade) {
		player.getPersistentData().putInt(REACH_UPGRADE_KEY, reachUpgrade);

		if (player.level.isClientSide) {
			//Set mirror radius to max
			int reach = 10;
			switch (reachUpgrade) {
				case 0:
					reach = CommonConfig.reach.maxReachLevel0.get();
					break;
				case 1:
					reach = CommonConfig.reach.maxReachLevel1.get();
					break;
				case 2:
					reach = CommonConfig.reach.maxReachLevel2.get();
					break;
				case 3:
					reach = CommonConfig.reach.maxReachLevel3.get();
					break;
			}

			//TODO enable
//			if (this.mirrorSettings != null)
//				this.mirrorSettings.radius = reach / 2;
//			if (this.radialMirrorSettings != null)
//				this.radialMirrorSettings.radius = reach / 2;
		} else {

		}
	}

	public static int getMaxReach(Player player) {
		if (player.isCreative()) return CommonConfig.reach.maxReachCreative.get();

		if (!CommonConfig.reach.enableReachUpgrades.get()) return CommonConfig.reach.maxReachLevel3.get();

		return switch (getReachUpgrade(player)) {
			case 1 -> CommonConfig.reach.maxReachLevel1.get();
			case 2 -> CommonConfig.reach.maxReachLevel2.get();
			case 3 -> CommonConfig.reach.maxReachLevel3.get();
			default -> CommonConfig.reach.maxReachLevel0.get();
		};
	}
	
	public static int getMaxMirrorRadius(Player player) {
		return getMaxReach(player) / 2;
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
