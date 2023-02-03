package nl.requios.effortlessbuilding;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static net.minecraftforge.common.ForgeConfigSpec.*;

public class ServerConfig {
    private static final Builder builder = new Builder();
    public static final ServerConfig.Validation validation = new ServerConfig.Validation(builder);
    public static final ServerConfig.Memory memory = new ServerConfig.Memory(builder);
    public static final ForgeConfigSpec spec = builder.build();

    public static class Validation {
        public final BooleanValue allowInSurvival;
        public final BooleanValue useWhitelist;
        public final ConfigValue<List<? extends String>> whitelist;

        public Validation(Builder builder) {
            builder.push("Validation");

            allowInSurvival = builder
                    .comment("Allow use of the mod for players that are in survival mode. Otherwise, only creative mode players can use the mod.")
                    .define("allowInSurvival", true);

            useWhitelist = builder
                    .comment("Use a whitelist to determine which players can use the mod. If false, all players can use the mod.")
                    .define("useWhitelist", false);

            whitelist = builder
                    .comment("List of player names that can use the mod.")
                    .defineList("whitelist", Arrays.asList("Player1", "Player2"), o -> true);

            builder.pop();
        }
    }

    public static class Memory {
        public final IntValue undoStackSize;

        public Memory(Builder builder) {
            builder.push("Memory");

            undoStackSize = builder
                    .comment("How many placements are remembered for the undo functionality.")
                    .worldRestart()
                    .defineInRange("undoStackSize", 50, 10, 200);

            builder.pop();
        }
    }
}
