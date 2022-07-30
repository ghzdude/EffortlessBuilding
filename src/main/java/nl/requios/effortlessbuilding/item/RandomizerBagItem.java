package nl.requios.effortlessbuilding.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import nl.requios.effortlessbuilding.gui.RandomizerBagContainer;

import javax.annotation.Nullable;

public class RandomizerBagItem extends AbstractRandomizerBagItem {
	public static final int INV_SIZE = 5;

	@Override
	public int getInventorySize() {
		return 5;
	}

	@Override
	public MenuProvider getContainerProvider(ItemStack bag) {
		return new ContainerProvider(bag);
	}

	public static class ContainerProvider implements MenuProvider {

		private final ItemStack bag;

		public ContainerProvider(ItemStack bag) {
			this.bag = bag;
		}

		@Override
		public Component getDisplayName() {
			return Component.translatable("effortlessbuilding:randomizer_bag");
		}

		@Nullable
		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
			return new RandomizerBagContainer(containerId, playerInventory, ((AbstractRandomizerBagItem)bag.getItem()).getBagInventory(bag));
		}
	}
}
