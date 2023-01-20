package nl.requios.effortlessbuilding.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static nl.requios.effortlessbuilding.buildmode.ModeSettingsManager.ModeSettings;

@Mod.EventBusSubscriber
public class ModeCapabilityManager {

	public static Capability<IModeCapability> MODE_CAPABILITY = CapabilityManager.get(new CapabilityToken<>(){});

	// Allows for the capability to persist after death.
	@SubscribeEvent
	public static void clonePlayer(PlayerEvent.Clone event) {
		LazyOptional<IModeCapability> original = event.getOriginal().getCapability(MODE_CAPABILITY, null);
		LazyOptional<IModeCapability> clone = event.getEntity().getCapability(MODE_CAPABILITY, null);
		clone.ifPresent(cloneModeCapability ->
			original.ifPresent(originalModeCapability ->
				cloneModeCapability.setModeData(originalModeCapability.getModeData())));
	}

	public interface IModeCapability {
		ModeSettings getModeData();

		void setModeData(ModeSettings modeSettings);
	}

	public static class ModeCapability implements IModeCapability {
		private ModeSettings modeSettings;

		@Override
		public ModeSettings getModeData() {
			return modeSettings;
		}

		@Override
		public void setModeData(ModeSettings modeSettings) {
			this.modeSettings = modeSettings;
		}
	}

	public static class Provider extends CapabilityProvider<Provider> implements ICapabilitySerializable<Tag> {

		private IModeCapability instance = new ModeCapability();
		private LazyOptional<IModeCapability> modeCapabilityOptional = LazyOptional.of(() -> instance);

		public Provider() {
			super(Provider.class);
			gatherCapabilities();
		}

		@Nonnull
		@Override
		public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
			if (cap == MODE_CAPABILITY) return modeCapabilityOptional.cast();
			return LazyOptional.empty();
		}

		@Override
		public void invalidateCaps() {
			super.invalidateCaps();
			modeCapabilityOptional.invalidate();
		}

		@Override
		public void reviveCaps() {
			super.reviveCaps();
			modeCapabilityOptional = LazyOptional.of(() -> instance);
		}

		@Override
		public Tag serializeNBT() {
			CompoundTag compound = new CompoundTag();
			ModeSettings modeSettings = instance.getModeData();
			if (modeSettings == null) modeSettings = new ModeSettingsManager.ModeSettings();

			//compound.putInteger("buildMode", modeSettings.getBuildMode().ordinal());

			//TODO add mode settings

			return compound;
		}

		@Override
		public void deserializeNBT(Tag nbt) {
			CompoundTag compound = (CompoundTag) nbt;

			//BuildModes.BuildModeEnum buildMode = BuildModes.BuildModeEnum.values()[compound.getInteger("buildMode")];

			//TODO add mode settings

			ModeSettings modeSettings = new ModeSettings(BuildModes.BuildModeEnum.DISABLED);
			instance.setModeData(modeSettings);
		}

	}
}
