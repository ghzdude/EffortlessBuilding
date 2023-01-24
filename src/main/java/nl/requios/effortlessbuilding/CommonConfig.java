package nl.requios.effortlessbuilding;

import net.minecraftforge.common.ForgeConfigSpec;
import nl.requios.effortlessbuilding.create.foundation.render.SuperByteBufferCache;

public class CommonConfig {

	private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
	public static final Reach reach = new Reach(builder);
	public static final SurvivalBalancers survivalBalancers = new SurvivalBalancers(builder);
	public static final Visuals visuals = new Visuals(builder);
	public static final ForgeConfigSpec spec = builder.build();

	public static class Reach {
		public final ForgeConfigSpec.ConfigValue<Boolean> enableReachUpgrades;
		public final ForgeConfigSpec.ConfigValue<Integer> maxReachCreative;
		public final ForgeConfigSpec.ConfigValue<Integer> maxReachLevel0;
		public final ForgeConfigSpec.ConfigValue<Integer> maxReachLevel1;
		public final ForgeConfigSpec.ConfigValue<Integer> maxReachLevel2;
		public final ForgeConfigSpec.ConfigValue<Integer> maxReachLevel3;

		public Reach(ForgeConfigSpec.Builder builder) {
			builder.push("Reach");
			enableReachUpgrades = builder
				.comment("Reach: how far away the player can place blocks using mirror/array etc.",
					"Enable the crafting of reach upgrades to increase reach.",
					"If disabled, reach is set to level 3 for survival players.")
				.define("enableReachUpgrades", true);

			maxReachCreative = builder
				.comment("Maximum reach in creative",
					"Keep in mind that chunks need to be loaded to be able to place blocks inside.")
				.define("maxReachCreative", 200);

			maxReachLevel0 = builder
				.comment("Maximum reach in survival without upgrades",
					"Reach upgrades are craftable consumables that permanently increase reach.",
					"Set to 0 to disable Effortless Building until the player has consumed a reach upgrade.")
				.define("maxReachLevel0", 20);

			maxReachLevel1 = builder
				.comment("Maximum reach in survival with one upgrade")
				.define("maxReachLevel1", 50);

			maxReachLevel2 = builder
				.comment("Maximum reach in survival with two upgrades")
				.define("maxReachLevel2", 100);

			maxReachLevel3 = builder
				.comment("Maximum reach in survival with three upgrades")
				.define("maxReachLevel3", 200);

			builder.pop();
		}
	}

	public static class SurvivalBalancers {
		public final ForgeConfigSpec.ConfigValue<Integer> quickReplaceMiningLevel;
		public final ForgeConfigSpec.ConfigValue<Integer> undoStackSize;

		public SurvivalBalancers(ForgeConfigSpec.Builder builder) {
			builder.push("SurvivalBalancers");

			quickReplaceMiningLevel = builder
				.comment("Determines what blocks can be replaced in survival.",
					"-1: only blocks that can be harvested by hand (default)",
					"0: blocks that can be harvested with wooden or gold tools",
					"1: blocks that can be harvested with stone tools",
					"2: blocks that can be harvested with iron tools",
					"3: blocks that can be harvested with diamond tools",
					"4: blocks that can be harvested with netherite tools")
				.defineInRange("quickReplaceMiningLevel", -1, -1, 3);

			undoStackSize = builder
				.comment("How many placements are remembered for the undo functionality.")
				.worldRestart()
				.define("undoStackSize", 50);

			builder.pop();
		}
	}

	public static class Visuals {
		public final ForgeConfigSpec.ConfigValue<Integer> appearAnimationLength;
		public final ForgeConfigSpec.ConfigValue<Integer> breakAnimationLength;

        public Visuals(ForgeConfigSpec.Builder builder) {
			builder.push("Visuals");

			appearAnimationLength = builder
				.comment("How long it takes for a block to appear when placed in ticks.",
					"Set to 0 to disable animation.")
				.define("appearAnimationLength", 5);

			breakAnimationLength = builder
				.comment("How long the break animation is in ticks.",
					"Set to 0 to disable animation.")
				.define("breakAnimationLength", 10);

			builder.pop();
		}
	}
}
