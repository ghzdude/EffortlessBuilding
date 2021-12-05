package nl.requios.effortlessbuilding.gui;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.ClickType;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Hand;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.item.GoldenRandomizerBagItem;

public class GoldenRandomizerBagContainer extends Container {

	private static final int INV_START = GoldenRandomizerBagItem.INV_SIZE,
			INV_END = INV_START + 26,
			HOTBAR_START = INV_END + 1,
			HOTBAR_END = HOTBAR_START + 8;
	private final IItemHandler bagInventory;

	public GoldenRandomizerBagContainer(ContainerType<?> type, int id){
		super(type, id);
		bagInventory = null;
	}

	//Client
	public GoldenRandomizerBagContainer(int id, PlayerInventory playerInventory, PacketBuffer packetBuffer) {
		this(id, playerInventory);
	}

	//Server?
	public GoldenRandomizerBagContainer(int containerId, PlayerInventory playerInventory) {
		this(containerId, playerInventory, new ItemStackHandler(GoldenRandomizerBagItem.INV_SIZE));
	}

	public GoldenRandomizerBagContainer(int containerId, PlayerInventory playerInventory, IItemHandler inventory) {
		super(EffortlessBuilding.GOLDEN_RANDOMIZER_BAG_CONTAINER.get(), containerId);
		bagInventory = inventory;

		for (int i = 0; i < GoldenRandomizerBagItem.INV_SIZE; ++i) {
			this.addSlot(new SlotItemHandler(bagInventory, i, 8 + (18 * i), 20));
		}

		// add player inventory slots
		for (int i = 0; i < 3; ++i) {
			for (int j = 0; j < 9; ++j) {
				addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 51 + i * 18));
			}
		}

		// add hotbar slots
		for (int i = 0; i < 9; ++i) {
			addSlot(new Slot(playerInventory, i, 8 + i * 18, 109));
		}
	}

	@Override
	public boolean stillValid(PlayerEntity playerIn) {
		return true;
	}

	@Override
	public Slot getSlot(int parSlotIndex) {
		if (parSlotIndex >= slots.size())
			parSlotIndex = slots.size() - 1;
		return super.getSlot(parSlotIndex);
	}

	@Override
	public ItemStack quickMoveStack(PlayerEntity playerIn, int slotIndex) {
		ItemStack itemstack = ItemStack.EMPTY;
		Slot slot = this.slots.get(slotIndex);

		if (slot != null && slot.hasItem()) {
			ItemStack itemstack1 = slot.getItem();
			itemstack = itemstack1.copy();

			// If item is in our custom inventory
			if (slotIndex < INV_START) {
				// try to place in player inventory / action bar
				if (!this.moveItemStackTo(itemstack1, INV_START, HOTBAR_END + 1, true)) {
					return ItemStack.EMPTY;
				}

				slot.onQuickCraft(itemstack1, itemstack);
			}
			// Item is in inventory / hotbar, try to place in custom inventory or armor slots
			else {
				/**
				 * Implementation number 1: Shift-click into your custom inventory
				 */
				if (slotIndex >= INV_START) {
					// place in custom inventory
					if (!this.moveItemStackTo(itemstack1, 0, INV_START, false)) {
						return ItemStack.EMPTY;
					}
				}
			}

			if (itemstack1.getCount() == 0) {
				slot.set(ItemStack.EMPTY);
			} else {
				slot.setChanged();
			}

			if (itemstack1.getCount() == itemstack.getCount()) {
				return ItemStack.EMPTY;
			}

			slot.onTake(playerIn, itemstack1);
		}

		return itemstack;
	}

	/**
	 * You should override this method to prevent the player from moving the stack that
	 * opened the inventory, otherwise if the player moves it, the inventory will not
	 * be able to save properly
	 * @return
	 */
	@Override
	public ItemStack clicked(int slot, int dragType, ClickType clickTypeIn, PlayerEntity player) {
		// this will prevent the player from interacting with the item that opened the inventory:
		if (slot >= 0 && getSlot(slot) != null && getSlot(slot).getItem().equals(player.getItemInHand(Hand.MAIN_HAND))) {
			//Do nothing;
			return ItemStack.EMPTY;
		}
		return super.clicked(slot, dragType, clickTypeIn, player);
	}

	/**
	 * Callback for when the crafting gui is closed.
	 */
	@Override
	public void removed(PlayerEntity player) {
		super.removed(player);
		if (!player.level.isClientSide) {
			broadcastChanges();
		}
	}
}
