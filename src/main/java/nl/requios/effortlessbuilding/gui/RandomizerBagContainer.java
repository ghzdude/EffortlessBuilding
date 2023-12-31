package nl.requios.effortlessbuilding.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;
import nl.requios.effortlessbuilding.item.ItemRandomizerBag;

public class RandomizerBagContainer extends Container {

    private final IItemHandler bagInventory;
    private final int sizeInventory;

    private static final int INV_START = ItemRandomizerBag.INV_SIZE, INV_END = INV_START + 26,
            HOTBAR_START = INV_END + 1, HOTBAR_END = HOTBAR_START + 8;

    public RandomizerBagContainer(InventoryPlayer parInventoryPlayer, IItemHandler parIInventory) {
        bagInventory = parIInventory;
        sizeInventory = bagInventory.getSlots();
        for (int i = 0; i < sizeInventory; ++i) {
            this.addSlotToContainer(new SlotItemHandler(bagInventory, i, 44 + (18 * i), 20));
        }

        // add player inventory slots
        int i;
        for (i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                addSlotToContainer(new Slot(parInventoryPlayer, j + i * 9 + 9, 8 + j * 18, 51 + i * 18));
            }
        }

        // add hotbar slots
        for (i = 0; i < 9; ++i) {
            addSlotToContainer(new Slot(parInventoryPlayer, i, 8 + i * 18, 109));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    @Override
    public Slot getSlot(int parSlotIndex)
    {
        if(parSlotIndex >= inventorySlots.size())
            parSlotIndex = inventorySlots.size() - 1;
        return super.getSlot(parSlotIndex);
    }

    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int slotIndex) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(slotIndex);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            // If item is in our custom inventory
            if (slotIndex < INV_START) {
                // try to place in player inventory / action bar
                if (!this.mergeItemStack(itemstack1, INV_START, HOTBAR_END + 1, true)) {
                    return ItemStack.EMPTY;
                }

                slot.onSlotChange(itemstack1, itemstack);
            }
            // Item is in inventory / hotbar, try to place in custom inventory or armor slots
            else {
                /**
                 * Implementation number 1: Shift-click into your custom inventory
                 */
                if (slotIndex >= INV_START) {
                    // place in custom inventory
                    if (!this.mergeItemStack(itemstack1, 0, INV_START, false)) {
                        return ItemStack.EMPTY;
                    }
                }

            }

            if (itemstack1.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
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
     */
    @Override
    public ItemStack slotClick(int slot, int dragType, ClickType clickTypeIn, EntityPlayer player) {
        // this will prevent the player from interacting with the item that opened the inventory:
        if (slot >= 0 && getSlot(slot) != null && getSlot(slot).getStack().equals(player.getHeldItem(EnumHand.MAIN_HAND))) {
            return ItemStack.EMPTY;
        }
        return super.slotClick(slot, dragType, clickTypeIn, player);
    }

    /**
     * Callback for when the crafting gui is closed.
     */
    @Override
    public void onContainerClosed(EntityPlayer player)
    {
        super.onContainerClosed(player);
        if(!player.world.isRemote)
        {
            detectAndSendChanges();
        }
    }
}
