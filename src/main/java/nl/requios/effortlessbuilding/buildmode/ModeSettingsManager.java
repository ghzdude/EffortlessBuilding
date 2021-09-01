package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fmllegacy.network.PacketDistributor;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.capability.ModeCapabilityManager;
import nl.requios.effortlessbuilding.helper.ReachHelper;
import nl.requios.effortlessbuilding.network.ModeSettingsMessage;
import nl.requios.effortlessbuilding.network.PacketHandler;

import javax.annotation.Nonnull;

@Mod.EventBusSubscriber
public class ModeSettingsManager {

	//Retrieves the buildsettings of a player through the modifierCapability capability
	//Never returns null
	@Nonnull
	public static ModeSettings getModeSettings(Player player) {
		LazyOptional<ModeCapabilityManager.IModeCapability> modeCapability =
			player.getCapability(ModeCapabilityManager.modeCapability, null);

		if (modeCapability.isPresent()) {
			ModeCapabilityManager.IModeCapability capability = modeCapability.orElse(null);
			if (capability.getModeData() == null) {
				capability.setModeData(new ModeSettings());
			}
			return capability.getModeData();
		}

		//Player does not have modeCapability capability
		//Return dummy settings
		return new ModeSettings();
//        throw new IllegalArgumentException("Player does not have modeCapability capability");
	}

	public static void setModeSettings(Player player, ModeSettings modeSettings) {
		if (player == null) {
			EffortlessBuilding.log("Cannot set buildmode settings, player is null");
			return;
		}
		LazyOptional<ModeCapabilityManager.IModeCapability> modeCapability =
			player.getCapability(ModeCapabilityManager.modeCapability, null);

		modeCapability.ifPresent((capability) -> {
			capability.setModeData(modeSettings);

			BuildModes.initializeMode(player);
		});

		if (!modeCapability.isPresent()) {
			EffortlessBuilding.log(player, "Saving buildmode settings failed.");
		}
	}

	public static String sanitize(ModeSettings modeSettings, Player player) {
		int maxReach = ReachHelper.getMaxReach(player);
		String error = "";

		//TODO sanitize

		return error;
	}

	public static void handleNewPlayer(Player player) {
		//Makes sure player has mode settings (if it doesnt it will create it)
		getModeSettings(player);

		//Only on server
		if (!player.level.isClientSide) {
			//Send to client
			ModeSettingsMessage msg = new ModeSettingsMessage(getModeSettings(player));
			PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), msg);
		}
	}

	public static class ModeSettings {
		private BuildModes.BuildModeEnum buildMode = BuildModes.BuildModeEnum.NORMAL;

		public ModeSettings() {
		}

		public ModeSettings(BuildModes.BuildModeEnum buildMode) {
			this.buildMode = buildMode;
		}

		public BuildModes.BuildModeEnum getBuildMode() {
			return this.buildMode;
		}

		public void setBuildMode(BuildModes.BuildModeEnum buildMode) {
			this.buildMode = buildMode;
		}
	}
}
