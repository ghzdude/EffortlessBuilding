package nl.requios.effortlessbuilding;

import net.minecraftforge.common.ForgeConfigSpec;

import static net.minecraftforge.common.ForgeConfigSpec.*;

public class CommonConfig {

	private static final Builder builder = new Builder();
	public static final Reach reach = new Reach(builder);
	public static final MaxBlocksPlacedAtOnce maxBlocksPlacedAtOnce = new MaxBlocksPlacedAtOnce(builder);
	public static final MaxBlocksPerAxis maxBlocksPerAxis = new MaxBlocksPerAxis(builder);
	public static final ForgeConfigSpec spec = builder.build();

	public static class Reach {
		public final IntValue reachCreative;
		public final IntValue reachLevel0;
		public final IntValue reachLevel1;
		public final IntValue reachLevel2;
		public final IntValue reachLevel3;

		public Reach(Builder builder) {
			builder.push("Reach");

			reachCreative = builder
				.comment("How far away the player can place and break blocks.")
				.defineInRange("maxReachCreative", 200, 0, 10000);

			reachLevel0 = builder
				.comment("Maximum reach in survival without upgrades",
					"Reach upgrades are craftable consumables that permanently increase reach.",
					"Set to 0 to disable Effortless Building until the player has consumed a reach upgrade.")
				.defineInRange("reachLevel0", 20, 0, 10000);

			reachLevel1 = builder
				.defineInRange("reachLevel1", 50, 0, 10000);

			reachLevel2 = builder
				.defineInRange("reachLevel2", 100, 0, 10000);

			reachLevel3 = builder
				.defineInRange("reachLevel3", 200, 0, 10000);

			builder.pop();
		}
	}

	public static class MaxBlocksPlacedAtOnce {
		public final IntValue creative;
		public final IntValue level0;
		public final IntValue level1;
		public final IntValue level2;
		public final IntValue level3;

		public MaxBlocksPlacedAtOnce(Builder builder) {
			builder.push("MaxBlocksPlacedAtOnce");

			creative = builder
				.comment("How many blocks can be placed in one click.")
				.defineInRange("maxBlocksPlacedAtOnceCreative", 10000, 0, 10000);

			level0 = builder
				.comment("Maximum blocks placed at once in survival without upgrades")
				.defineInRange("maxBlocksPlacedAtOnceLevel0", 100, 0, 10000);

			level1 = builder
				.defineInRange("maxBlocksPlacedAtOnceLevel1", 200, 0, 10000);

			level2 = builder
				.defineInRange("maxBlocksPlacedAtOnceLevel2", 500, 0, 10000);

			level3 = builder
				.defineInRange("maxBlocksPlacedAtOnceLevel3", 1000, 0, 10000);

			builder.pop();
		}
	}

	public static class MaxBlocksPerAxis {
		public final IntValue creative;
		public final IntValue level0;
		public final IntValue level1;
		public final IntValue level2;
		public final IntValue level3;

		public MaxBlocksPerAxis(Builder builder) {
			builder.push("MaxBlocksPerAxis");

			creative = builder
				.comment("How many blocks can be placed at once per axis.")
				.defineInRange("maxBlocksPerAxisCreative", 10000, 0, 10000);

			level0 = builder
				.comment("Maximum blocks placed at once in survival without upgrades")
				.defineInRange("maxBlocksPerAxisLevel0", 100, 0, 10000);

			level1 = builder
				.defineInRange("maxBlocksPerAxisLevel1", 200, 0, 10000);

			level2 = builder
				.defineInRange("maxBlocksPerAxisLevel2", 500, 0, 10000);

			level3 = builder
				.defineInRange("maxBlocksPerAxisLevel3", 1000, 0, 10000);

			builder.pop();
		}
	}
}
