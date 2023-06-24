package nl.requios.effortlessbuilding.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import org.jetbrains.annotations.NotNull;

//Adds a single item with a chance to any loot tables. Specify loot tables in the JSON file.
//Add JSON files to resources/data/effortlessbuilding/loot_modifiers, and list them in resources/data/forge/loot_modifiers/global_loot_modifiers.json
//https://forge.gemwire.uk/wiki/Dynamic_Loot_Modification
//https://forums.minecraftforge.net/topic/112960-1182-solved-adding-modded-items-to-existing-vanilla-loot-tables/
//https://mcreator.net/wiki/minecraft-vanilla-loot-tables-list#toc-index-1
public class SingleItemLootModifier extends LootModifier {

    public static final RegistryObject<Codec<SingleItemLootModifier>> CODEC = EffortlessBuilding.LOOT_MODIFIERS.register("single_item_loot_modifier", () ->
            RecordCodecBuilder.create(inst -> codecStart(inst).and(
                    inst.group(
                            Codec.FLOAT.fieldOf("chance").forGetter(m -> m.chance),
                            ForgeRegistries.ITEMS.getCodec().fieldOf("item").forGetter(m -> m.item)
                    )).apply(inst, SingleItemLootModifier::new)
            ));

    private final float chance;
    private final Item item;

    public SingleItemLootModifier(LootItemCondition[] conditionsIn, float chance, Item item) {
        super(conditionsIn);
        this.chance = chance;
        this.item = item;
    }

    @NotNull
    @Override
    public ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot, LootContext context) {
        //
        // Additional conditions can be checked, though as much as possible should be parameterized via JSON data.
        // It is better to write a new ILootCondition implementation than to do things here.
        //
        //with chance, add an item
        if (context.getRandom().nextFloat() < chance) {
            generatedLoot.add(new ItemStack(item, 1));
        }
        return generatedLoot;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
