package nl.requios.effortlessbuilding.capability;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nl.requios.effortlessbuilding.buildmode.BuildModes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static nl.requios.effortlessbuilding.buildmode.ModeSettingsManager.ModeSettings;

import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager.ModeSettings;

@Mod.EventBusSubscriber
public class ModeCapabilityManager {

	@CapabilityInject(IModeCapability.class)
	public final static Capability<IModeCapability> modeCapability = null;

	// Allows for the capability to persist after death.
	@SubscribeEvent
	public static void clonePlayer(PlayerEvent.Clone event) {
		LazyOptional<IModeCapability> original = event.getOriginal().getCapability(modeCapability, null);
		LazyOptional<IModeCapability> clone = event.getEntity().getCapability(modeCapability, null);
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

	public static class Storage implements Capability.IStorage<IModeCapability> {
		@Override
		public INBT writeNBT(Capability<IModeCapability> capability, IModeCapability instance, Direction side) {
			CompoundNBT compound = new CompoundNBT();
			ModeSettings modeSettings = instance.getModeData();
			if (modeSettings == null) modeSettings = new ModeSettings();

			//compound.putInteger("buildMode", modeSettings.getBuildMode().ordinal());

			//TODO add mode settings

			return compound;
		}

		@Override
		public void readNBT(Capability<IModeCapability> capability, IModeCapability instance, Direction side, INBT nbt) {
			CompoundNBT compound = (CompoundNBT) nbt;

			//BuildModes.BuildModeEnum buildMode = BuildModes.BuildModeEnum.values()[compound.getInteger("buildMode")];

			//TODO add mode settings

			ModeSettings modeSettings = new ModeSettings(BuildModes.BuildModeEnum.NORMAL);
			instance.setModeData(modeSettings);
		}
	}

	public static class Provider implements ICapabilitySerializable<INBT> {
		IModeCapability inst = modeCapability.getDefaultInstance();

		@Nonnull
		@Override
		public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
			return modeCapability.orEmpty(cap, LazyOptional.of(() -> inst));
		}

		@Override
		public INBT serializeNBT() {
			return modeCapability.getStorage().writeNBT(modeCapability, inst, null);
		}

		@Override
		public void deserializeNBT(INBT nbt) {
			modeCapability.getStorage().readNBT(modeCapability, inst, null, nbt);
		}

	}
}
