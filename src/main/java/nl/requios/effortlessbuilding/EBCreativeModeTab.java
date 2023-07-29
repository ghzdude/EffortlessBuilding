package nl.requios.effortlessbuilding;

import net.minecraft.world.item.CreativeModeTab;

public class EBCreativeModeTab implements CreativeModeTab.DisplayItemsGenerator {

    @Override
    public void accept(CreativeModeTab.ItemDisplayParameters pParameters, CreativeModeTab.Output pOutput) {
        pOutput.accept(EffortlessBuilding.RANDOMIZER_BAG_ITEM.get());
        pOutput.accept(EffortlessBuilding.GOLDEN_RANDOMIZER_BAG_ITEM.get());
        pOutput.accept(EffortlessBuilding.DIAMOND_RANDOMIZER_BAG_ITEM.get());

        pOutput.accept(EffortlessBuilding.REACH_UPGRADE_1_ITEM.get());
        pOutput.accept(EffortlessBuilding.REACH_UPGRADE_2_ITEM.get());
        pOutput.accept(EffortlessBuilding.REACH_UPGRADE_3_ITEM.get());

        pOutput.accept(EffortlessBuilding.MUSCLES_ITEM.get());
        pOutput.accept(EffortlessBuilding.ELASTIC_HAND_ITEM.get());
        pOutput.accept(EffortlessBuilding.BUILDING_TECHNIQUES_BOOK_ITEM.get());
    }
}
