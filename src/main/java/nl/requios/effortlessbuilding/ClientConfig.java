package nl.requios.effortlessbuilding;

import net.minecraftforge.common.ForgeConfigSpec;

public class ClientConfig {

	private static final ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
	public static final Visuals visuals = new Visuals(builder);
	public static final ForgeConfigSpec spec = builder.build();

	public static class Visuals {
		public final ForgeConfigSpec.ConfigValue<Boolean> showBlockPreviews;
		public final ForgeConfigSpec.ConfigValue<Boolean> alwaysShowBlockPreview;
		public final ForgeConfigSpec.ConfigValue<Integer> maxBlockPreviews;

		public Visuals(ForgeConfigSpec.Builder builder) {
			builder.push("Visuals");

			showBlockPreviews = builder
					.comment("Show previews of the blocks while placing them")
					.define("useShaders", true);

			alwaysShowBlockPreview = builder
				.comment("Show a block preview if you have a block in hand even in the 'Disabled' build mode")
				.define("alwaysShowBlockPreview", false);

			maxBlockPreviews = builder
				.comment("Don't show block previews when placing more than this many blocks. " +
						 "The outline will always be rendered.")
				.define("shaderTreshold", 500);


			builder.pop();
		}
	}
}
