package nl.requios.effortlessbuilding.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import nl.requios.effortlessbuilding.gui.RandomizerBagContainer;

import javax.annotation.Nullable;

public class RandomizerBagItem extends AbstractRandomizerBagItem{
	public static final int INV_SIZE = 5;

	@Override
	public int getInventorySize() {
		return 5;
	}

	@Override
	public INamedContainerProvider getContainerProvider(ItemStack bag) {
		return new ContainerProvider(bag);
	}

	public static class ContainerProvider implements INamedContainerProvider {

		private final ItemStack bag;

		public ContainerProvider(ItemStack bag) {
			this.bag = bag;
		}

		@Override
		public ITextComponent getDisplayName() {
			return new TranslationTextComponent("effortlessbuilding:randomizer_bag");
		}

		@Nullable
		@Override
		public Container createMenu(int containerId, PlayerInventory playerInventory, PlayerEntity player) {
			return new RandomizerBagContainer(containerId, playerInventory, ((AbstractRandomizerBagItem)bag.getItem()).getBagInventory(bag));
		}
	}
}
