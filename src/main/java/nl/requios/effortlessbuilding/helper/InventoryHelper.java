package nl.requios.effortlessbuilding.helper;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;

public class InventoryHelper {

	public static ItemStack findItemStackInInventory(PlayerEntity player, Block block) {
		for (ItemStack invStack : player.inventory.items) {
			if (!invStack.isEmpty() && invStack.getItem() instanceof BlockItem &&
				((BlockItem) invStack.getItem()).getBlock().equals(block)) {
				return invStack;
			}
		}
		return ItemStack.EMPTY;
	}

	public static int findTotalBlocksInInventory(PlayerEntity player, Block block) {
		int total = 0;
		for (ItemStack invStack : player.inventory.items) {
			if (!invStack.isEmpty() && invStack.getItem() instanceof BlockItem &&
				((BlockItem) invStack.getItem()).getBlock().equals(block)) {
				total += invStack.getCount();
			}
		}
		return total;
	}
}
