package nl.requios.effortlessbuilding;

import net.minecraftforge.common.config.Config;

import static net.minecraftforge.common.config.Config.*;

@Config(modid = EffortlessBuilding.MODID, name = "EffortlessBuilding", type = Type.INSTANCE, category = "")
public class BuildConfig {

    public static Reach reach = new Reach();
    public static SurvivalBalancers survivalBalancers = new SurvivalBalancers();
    public static Visuals visuals = new Visuals();

    public static class Reach {
        @Comment({"Reach: how far away the player can place blocks using mirror/array etc.",
                 "Enable the crafting of reach upgrades to increase reach.",
                 "If disabled, reach is set to level 3 for survival players."})
        public boolean enableReachUpgrades = true;

        @Comment({"Maximum reach in creative",
                 "Keep in mind that chunks need to be loaded to be able to place blocks inside."})
        public int maxReachCreative = 200;

        @Comment({"Maximum reach in survival without upgrades",
                 "Reach upgrades are craftable consumables that permanently increase reach.",
                 "Set to 0 to disable Effortless Building until the player has consumed a reach upgrade."})
        public int maxReachLevel0 = 20;

        @Comment("Maximum reach in survival with one upgrade")
        public int maxReachLevel1 = 50;

        @Comment("Maximum reach in survival with two upgrades")
        public int maxReachLevel2 = 100;

        @Comment("Maximum reach in survival with three upgrades")
        public int maxReachLevel3 = 200;
    }

    public static class SurvivalBalancers {
        @Comment({"Allows a survival player to break blocks that are far away, in addition to placing blocks.",
                 "Note: this allows insta-breaking of blocks in survival."})
        public boolean breakFar = false;

        @Comment({"Increases the time to mine a block when breaking multiple at once.",
                 "Mining time depends on how many blocks, what type of blocks, and the percentage below.",
                 "Example: breaking 1 dirt + 1 obsidian takes the time of breaking 1 dirt + 1 obsidian."})
        public boolean increasedMiningTime = true;

        @Comment({"How much the mining time of each additional block counts towards an increased mining time.",
                 "A percentage between 0% and 100%, where 0% is the same as disabling it,",
                 "and 100% takes as much time as breaking each block individually.",
                 "The block in front of you always counts as 100%."})
        @RangeInt(min = 0, max = 200)
        public int miningTimePercentage = 50;

        @Comment({"Determines what blocks can be replaced in survival.",
                 "-1: only blocks that can be harvested by hand (default)",
                 "0: blocks that can be harvested with wooden oref gold tools",
                 "1: blocks that can be harvested with stone tools",
                 "2: blocks that can be harvested with iron tools",
                 "3: blocks that can be harvested with diamond tools",
                 })
        @RangeInt(min = -1, max = 3)
        public int quickReplaceMiningLevel = -1;

        @Comment({"How many placements are remembered for the undo functionality."})
        @RequiresMcRestart
        public int undoStackSize = 10;
    }

    public static class Visuals {
        @Comment({"Show a block preview if you have a block in hand on build mode NORMAL"})
        public boolean alwaysShowBlockPreview = false;

        @Comment({"How long the dissolve effect takes when placing blocks.",
                 "Default between 30 and 60 ticks, you can multiply that here.",
                 "Recommended values:",
                 "Snappy: 0.7",
                 "Relaxing: 1.5"})
        public double dissolveTimeMultiplier = 1.0;

        @Comment({"Switch to using the simple performance shader when placing more than this many blocks."})
        public int shaderTreshold = 1500;

        @Comment({"Use fancy shaders while placing blocks"})
        public boolean useShaders = true;

    }
}
