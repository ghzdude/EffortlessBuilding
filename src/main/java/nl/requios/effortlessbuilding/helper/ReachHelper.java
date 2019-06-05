package nl.requios.effortlessbuilding.helper;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.MathHelper;
import nl.requios.effortlessbuilding.BuildConfig;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;

public class ReachHelper {
    public static int getMaxReach(EntityPlayer player) {
        if (player.isCreative()) return BuildConfig.reach.maxReachCreative;

        if (!BuildConfig.reach.enableReachUpgrades) return BuildConfig.reach.maxReachLevel3;

        //Check buildsettings for reachUpgrade
        int reachUpgrade = ModifierSettingsManager.getModifierSettings(player).getReachUpgrade();
        switch (reachUpgrade) {
            case 0: return BuildConfig.reach.maxReachLevel0;
            case 1: return BuildConfig.reach.maxReachLevel1;
            case 2: return BuildConfig.reach.maxReachLevel2;
            case 3: return BuildConfig.reach.maxReachLevel3;
        }
        return BuildConfig.reach.maxReachLevel0;
    }

    public static int getPlacementReach(EntityPlayer player) {
        return getMaxReach(player) / 4;
    }

    public static int getMaxBlocksPlacedAtOnce(EntityPlayer player) {
        if (player.isCreative()) return 1000000;
        return MathHelper.ceil(Math.pow(getMaxReach(player), 1.6));
        //Level 0: 90
        //Level 1: 353
        //Level 2: 1000
        //Level 3: 2828
    }

    public static int getMaxBlocksPerAxis(EntityPlayer player) {
        if (player.isCreative()) return 2000;
        return MathHelper.ceil(getMaxReach(player) * 0.3);
        //Level 0: 6
        //Level 1: 15
        //Level 2: 30
        //Level 3: 60
    }

    public static boolean canBreakFar(EntityPlayer player) {
        return player.isCreative() || BuildConfig.survivalBalancers.breakFar;
    }
}
