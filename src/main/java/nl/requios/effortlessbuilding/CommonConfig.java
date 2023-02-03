package nl.requios.effortlessbuilding;

import net.minecraftforge.common.ForgeConfigSpec;
import nl.requios.effortlessbuilding.create.foundation.render.SuperByteBufferCache;

import static net.minecraftforge.common.ForgeConfigSpec.*;

public class CommonConfig {

	private static final Builder builder = new Builder();
	public static final Reach reach = new Reach(builder);
	public static final SurvivalBalancers survivalBalancers = new SurvivalBalancers(builder);
	public static final ForgeConfigSpec spec = builder.build();

	public static class Reach {
		public final BooleanValue enableReachUpgrades;
		public final IntValue maxReachCreative;
		public final IntValue maxReachLevel0;
		public final IntValue maxReachLevel1;
		public final IntValue maxReachLevel2;
		public final IntValue maxReachLevel3;

		public Reach(Builder builder) {
			builder.push("Reach");
			enableReachUpgrades = builder
				.comment("Reach: how far away the player can place blocks using mirror/array etc.",
					"Enable the crafting of reach upgrades to increase reach.",
					"If disabled, reach is set to level 3 for survival players.")
				.define("enableReachUpgrades", true);

			maxReachCreative = builder
				.comment("Maximum reach in creative",
					"Keep in mind that chunks need to be loaded to be able to place blocks inside.")
				.defineInRange("maxReachCreative", 200, 0, 1000);

			maxReachLevel0 = builder
				.comment("Maximum reach in survival without upgrades",
					"Reach upgrades are craftable consumables that permanently increase reach.",
					"Set to 0 to disable Effortless Building until the player has consumed a reach upgrade.")
				.defineInRange("maxReachLevel0", 20, 0, 1000);

			maxReachLevel1 = builder
				.comment("Maximum reach in survival with one upgrade")
				.defineInRange("maxReachLevel1", 50, 0, 1000);

			maxReachLevel2 = builder
				.comment("Maximum reach in survival with two upgrades")
				.defineInRange("maxReachLevel2", 100, 0, 1000);

			maxReachLevel3 = builder
				.comment("Maximum reach in survival with three upgrades")
				.defineInRange("maxReachLevel3", 200, 0, 1000);

			builder.pop();
		}
	}

	public static class SurvivalBalancers {
		public final IntValue quickReplaceMiningLevel;

		public SurvivalBalancers(Builder builder) {
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

			builder.pop();
		}
	}
}
