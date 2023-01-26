package nl.requios.effortlessbuilding.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ItemHandlerCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
	IItemHandler itemHandler;

	public ItemHandlerCapabilityProvider(int size) {
		itemHandler = new ItemStackHandler(size);
	}

	@Nonnull
	@Override
	public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {
		return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.orEmpty(cap, LazyOptional.of(() -> itemHandler));
	}

	@Override
	public CompoundTag serializeNBT() {
		return ((ItemStackHandler) itemHandler).serializeNBT();
	}

	@Override
	public void deserializeNBT(CompoundTag nbt) {
		((ItemStackHandler) itemHandler).deserializeNBT(nbt);
	}
}
