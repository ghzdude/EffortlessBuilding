package nl.requios.effortlessbuilding;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import nl.requios.effortlessbuilding.gui.DiamondRandomizerBagScreen;
import nl.requios.effortlessbuilding.gui.GoldenRandomizerBagScreen;
import nl.requios.effortlessbuilding.gui.RandomizerBagScreen;
import nl.requios.effortlessbuilding.systems.BlockChain;

public class EffortlessBuildingClient {

    public static final BlockChain BLOCK_CHAIN = new BlockChain();

    public static void onConstructorClient(IEventBus modEventBus, IEventBus forgeEventBus) {
        modEventBus.addListener(EffortlessBuildingClient::clientSetup);
    }

    public static void clientSetup(final FMLClientSetupEvent event) {
        MenuScreens.register(EffortlessBuilding.RANDOMIZER_BAG_CONTAINER.get(), RandomizerBagScreen::new);
        MenuScreens.register(EffortlessBuilding.GOLDEN_RANDOMIZER_BAG_CONTAINER.get(), GoldenRandomizerBagScreen::new);
        MenuScreens.register(EffortlessBuilding.DIAMOND_RANDOMIZER_BAG_CONTAINER.get(), DiamondRandomizerBagScreen::new);
    }
}
