package nl.requios.effortlessbuilding.compatibility;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.item.AbstractRandomizerBagItem;

public class CompatHelper {
	//TODO 1.13 compatibility
//    // Get a handle to the dank null item instance. This will remain null if the mod doesn't load
//    // and all checks will fail, so it works.
//    @GameRegistry.ObjectHolder("danknull:dank_null")
//    public static final Item dankNullItem = null;
//
//    public static IChiselsAndBitsProxy chiselsAndBitsProxy;
//
	public static void setup() {
		//TODO 1.13 compatibility
//        if (Loader.isModLoaded("chiselsandbits")) {
//            // reflection to avoid hard dependency
//            try {
//                chiselsAndBitsProxy = Class.forName("nl.requios.effortlessbuilding.compatibility.ActiveChiselsAndBitsProxy").asSubclass(ActiveChiselsAndBitsProxy.class).newInstance();
//            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
//                e.printStackTrace();
//            }
//        } else {
//            chiselsAndBitsProxy = new DummyChiselsAndBitsProxy();
//        }
	}

	// Check if the item given is a proxy for blocks. For now, we check for the randomizer bag,
	// /dank/null, or plain old blocks.
	public static boolean isItemBlockProxy(ItemStack stack) {
		Item item = stack.getItem();
		if (item instanceof BlockItem)
			return true;
		return item instanceof AbstractRandomizerBagItem;
		//TODO 1.13 compatibility
//        if (item == dankNullItem)
//            return true;
	}

	// Get the block to be placed by this proxy. For the /dank/null, it's the slot stack
	// pointed to by nbt integer selectedIndex.
	public static ItemStack getItemBlockFromStack(ItemStack proxy) {
		Item proxyItem = proxy.getItem();

		if (proxyItem instanceof BlockItem)
			return proxy;

		//Randomizer Bag
		if (proxyItem instanceof AbstractRandomizerBagItem) {
			ItemStack itemStack = proxy;
			while (!(itemStack.getItem() instanceof BlockItem || itemStack.isEmpty())) {
				if (itemStack.getItem() instanceof AbstractRandomizerBagItem randomizerBagItem)
					itemStack = randomizerBagItem.pickRandomStack(randomizerBagItem.getBagInventory(itemStack));
			}
			return itemStack;
		}

		//TODO 1.13 compatibility
		//Dank Null
//        if (proxyItem == dankNullItem) {
//            int index = 0;
//            if (proxy.hasTagCompound() && proxy.getTagCompound().hasKey("selectedIndex"))
//                index = proxy.getTagCompound().getInteger("selectedIndex");
//            return proxy.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getStackInSlot(index);
//        }

		return ItemStack.EMPTY;
	}

	public static ItemStack getItemBlockByState(ItemStack stack, BlockState state) {
		if (state == null) return ItemStack.EMPTY;

		Item blockItem = Item.byBlock(state.getBlock());
		if (stack.getItem() instanceof BlockItem)
			return stack;
		else if (stack.getItem() instanceof AbstractRandomizerBagItem randomizerBagItem) {
			IItemHandler bagInventory = randomizerBagItem.getBagInventory(stack);
			return randomizerBagItem.findStack(bagInventory, blockItem);
		}
		//TODO 1.13 compatibility
//        else if (stack.getItem() == dankNullItem) {
//            int index = itemHandlerSlotForItem(stack, blockItem);
//            if (index >= 0)
//                return stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).getStackInSlot(index);
//        }
		return ItemStack.EMPTY;
	}

	// Handle IItemHandler slot stacks not being modifiable. We must call IItemHandler#extractItem,
	// because the ItemStack returned by IItemHandler#getStackInSlot isn't modifiable.
	public static void shrinkStack(ItemStack origStack, ItemStack curStack, Player player) {
		//TODO 1.13 compatibility, offhand support
		//Hacky way to get the origstack, because given origStack is itemblock stack and never proxy
//        origStack = player.getHeldItem(EnumHand.MAIN_HAND);

//        if (origStack.getItem() == dankNullItem) {
//            int index = itemHandlerSlotForItem(origStack, curStack.getItem());
//            origStack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).extractItem(index, 1, false);
//        } else
		curStack.shrink(1);
	}

	private static int itemHandlerSlotForItem(ItemStack stack, Item blockItem) {
		LazyOptional<IItemHandler> itemHandlerLazyOptional = stack.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null);
		IItemHandler handler = itemHandlerLazyOptional.orElse(null);

		if (handler == null) {
			EffortlessBuilding.logger.warn("Itemblock proxy has no item handler capability!");
			return -1;
		}

		for (int i = 0; i < handler.getSlots(); i++) {
			ItemStack ref = handler.getStackInSlot(i);
			if (ref.getItem() instanceof BlockItem)
				if (ref.getItem() == blockItem)
					return i;
		}
		return -1;
	}

}
