package nl.requios.effortlessbuilding.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.TextComponent;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.network.ModifierSettingsMessage;
import nl.requios.effortlessbuilding.network.PacketHandler;

public class CommandReach {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("reach").then(Commands.literal("set").then(Commands.argument("level", IntegerArgumentType.integer(0, 3)).executes((context) -> {
			return setReachLevel(context.getSource().getPlayerOrException(), IntegerArgumentType.getInteger(context, "level"));
		}))).then(Commands.literal("get").executes((context -> {
			return getReachLevel(context.getSource().getPlayerOrException());
		}))));
	}

	private static int setReachLevel(ServerPlayer player, int level) {
		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
		modifierSettings.setReachUpgrade(level);
		ModifierSettingsManager.setModifierSettings(player, modifierSettings);
		//Send to client
		PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), new ModifierSettingsMessage(modifierSettings));

		player.sendMessage(new TextComponent("Reach level of " + player.getName().getString() + " set to " + modifierSettings.getReachUpgrade()), player.getUUID());

		return 1;
	}

	private static int getReachLevel(ServerPlayer player) {
		int reachUpgrade = ModifierSettingsManager.getModifierSettings(player).getReachUpgrade();
		EffortlessBuilding.log(player, "Current reach: level " + reachUpgrade);

		return 1;
	}
}
