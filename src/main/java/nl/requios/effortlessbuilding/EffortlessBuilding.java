package nl.requios.effortlessbuilding;

import net.minecraft.block.Block;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import nl.requios.effortlessbuilding.capability.ModeCapabilityManager;
import nl.requios.effortlessbuilding.capability.ModifierCapabilityManager;
import nl.requios.effortlessbuilding.command.CommandReach;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.gui.RandomizerBagGuiHandler;
import nl.requios.effortlessbuilding.item.ItemRandomizerBag;
import nl.requios.effortlessbuilding.item.ItemReachUpgrade1;
import nl.requios.effortlessbuilding.item.ItemReachUpgrade2;
import nl.requios.effortlessbuilding.item.ItemReachUpgrade3;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.proxy.ClientProxy;
import nl.requios.effortlessbuilding.proxy.IProxy;
import nl.requios.effortlessbuilding.proxy.ServerProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EffortlessBuilding.MODID)
@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
public class EffortlessBuilding
{
    public static final String MODID = "effortlessbuilding";
    public static final String NAME = "Effortless Building";
    public static final String VERSION = "1.14.4-2.16";

    public static EffortlessBuilding instance;

    public static final Logger logger = LogManager.getLogger();

    public static IProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);

    public static final ItemRandomizerBag ITEM_RANDOMIZER_BAG = new ItemRandomizerBag();
    public static final ItemReachUpgrade1 ITEM_REACH_UPGRADE_1 = new ItemReachUpgrade1();
    public static final ItemReachUpgrade2 ITEM_REACH_UPGRADE_2 = new ItemReachUpgrade2();
    public static final ItemReachUpgrade3 ITEM_REACH_UPGRADE_3 = new ItemReachUpgrade3();

    public static final Block[] BLOCKS = {
    };

    public static final Item[] ITEMS = {
            ITEM_RANDOMIZER_BAG,
            ITEM_REACH_UPGRADE_1,
            ITEM_REACH_UPGRADE_2,
            ITEM_REACH_UPGRADE_3
    };

    public static final ResourceLocation RANDOMIZER_BAG_GUI = new ResourceLocation(EffortlessBuilding.MODID, "randomizer_bag");

    public EffortlessBuilding() {
        instance = this;

        // Register the setup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        // Register the enqueueIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        // Register the processIMC method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);
        // Register the clientSetup method for modloading
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::clientSetup);

        //Register config
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BuildConfig.spec);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void setup(final FMLCommonSetupEvent event)
    {
        CapabilityManager.INSTANCE.register(ModifierCapabilityManager.IModifierCapability.class, new ModifierCapabilityManager.Storage(), ModifierCapabilityManager.ModifierCapability::new);
        CapabilityManager.INSTANCE.register(ModeCapabilityManager.IModeCapability.class, new ModeCapabilityManager.Storage(), ModeCapabilityManager.ModeCapability::new);

        PacketHandler.register();

        //TODO 1.13 config
//        ConfigManager.sync(MODID, Config.Type.INSTANCE);

        //TODO 1.14 GUI
//        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.GUIFACTORY, () -> RandomizerBagGuiHandler::openGui);

        proxy.setup(event);

        CompatHelper.setup();
    }

    @SubscribeEvent
    public void clientSetup(final FMLClientSetupEvent event) {

        proxy.clientSetup(event);
    }

    @SubscribeEvent
    public void enqueueIMC(final InterModEnqueueEvent event) {

        // some example code to dispatch IMC to another mod
//        InterModComms.sendTo("examplemod", "helloworld", () -> { logger.info("Hello world from the MDK"); return "Hello world";});
    }

    @SubscribeEvent
    public void processIMC(final InterModProcessEvent event) {

        // some example code to receive and process InterModComms from other mods
//        logger.info("Got IMC {}", event.getIMCStream().
//                map(m->m.getMessageSupplier().get()).
//                collect(Collectors.toList()));
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
        CommandReach.register(event.getCommandDispatcher());
    }


    public static void log(String msg){
        logger.info(msg);
    }

    public static void log(PlayerEntity player, String msg){
        log(player, msg, false);
    }

    public static void log(PlayerEntity player, String msg, boolean actionBar){
        player.sendStatusMessage(new StringTextComponent(msg), actionBar);
    }
}
