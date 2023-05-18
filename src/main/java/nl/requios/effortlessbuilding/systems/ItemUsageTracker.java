package nl.requios.effortlessbuilding.systems;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.item.AbstractRandomizerBagItem;
import nl.requios.effortlessbuilding.utilities.InventoryHelper;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ItemUsageTracker {

    //How many blocks we want to place
    public Map<Item, Integer> total = new HashMap<>();

    //How many blocks we have in inventory in total
    public Map<Item, Integer> inInventory = new HashMap<>();

    //How many blocks are missing from our inventory
    public Map<Item, Integer> missing = new HashMap<>();

    public void initialize(Player player, ItemStack heldItem) {
        total.clear();
        inInventory.clear();
        missing.clear();

        if (CompatHelper.isItemBlockProxy(heldItem, false)) {
            AbstractRandomizerBagItem.resetRandomness();
        }
    }

    //returns if we have enough items in inventory to use count more
    public boolean increaseUsageCount(Item item, int count, Player player) {
        if (item == null) return true;
        int newValue = total.getOrDefault(item, 0) + count;
        total.put(item, newValue);

        if (player.isCreative()) return true;
        int have = 0;
        if (inInventory.containsKey(item)) {
            have = inInventory.get(item);
        } else {
            have = InventoryHelper.findTotalItemsInInventory(player, item);
            inInventory.put(item, have);
        }

        return have >= newValue;
    }

    public void calculateMissingItems(Player player) {
        if (player.isCreative()) return;
        for (Item item : total.keySet()) {
            int used = total.get(item);
            int have = inInventory.getOrDefault(item, 0);
            if (used > have) {
                missing.put(item, used - have);
            }
        }
    }

    public int getValidCount(Item item) {
        return total.getOrDefault(item, 0) - missing.getOrDefault(item, 0);
    }

    public int getMissingCount(Item item) {
        return missing.getOrDefault(item, 0);
    }
}
