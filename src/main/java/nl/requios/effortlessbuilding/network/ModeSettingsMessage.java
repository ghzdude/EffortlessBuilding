package nl.requios.effortlessbuilding.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager.ModeSettings;

import java.util.function.Supplier;

/**
 * Shares mode settings (see ModeSettingsManager) between server and client
 */
public class ModeSettingsMessage {

	private ModeSettings modeSettings;

	public ModeSettingsMessage() {
	}

	public ModeSettingsMessage(ModeSettings modeSettings) {
		this.modeSettings = modeSettings;
	}

	public static void encode(ModeSettingsMessage message, FriendlyByteBuf buf) {
		buf.writeInt(message.modeSettings.getBuildMode().ordinal());
	}

	public static ModeSettingsMessage decode(FriendlyByteBuf buf) {
		BuildModes.BuildModeEnum buildMode = BuildModes.BuildModeEnum.values()[buf.readInt()];

		return new ModeSettingsMessage(new ModeSettings(buildMode));
	}

	public static class Handler {
		public static void handle(ModeSettingsMessage message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);

				// Sanitize
				ModeSettingsManager.sanitize(message.modeSettings, player);

				ModeSettingsManager.setModeSettings(player, message.modeSettings);
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
