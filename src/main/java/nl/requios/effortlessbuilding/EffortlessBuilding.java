package nl.requios.effortlessbuilding;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.item.Item;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import nl.requios.effortlessbuilding.capability.ModeCapabilityManager;
import nl.requios.effortlessbuilding.capability.ModifierCapabilityManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.gui.DiamondRandomizerBagContainer;
import nl.requios.effortlessbuilding.gui.GoldenRandomizerBagContainer;
import nl.requios.effortlessbuilding.gui.RandomizerBagContainer;
import nl.requios.effortlessbuilding.item.*;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.proxy.ClientProxy;
import nl.requios.effortlessbuilding.proxy.IProxy;
import nl.requios.effortlessbuilding.proxy.ServerProxy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(EffortlessBuilding.MODID)
@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class EffortlessBuilding {
	public static final String MODID = "effortlessbuilding";
	public static final Logger logger = LogManager.getLogger();

	public static EffortlessBuilding instance;
	public static IProxy proxy = DistExecutor.runForDist(() -> ClientProxy::new, () -> ServerProxy::new);

	//Registration
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
	private static final DeferredRegister<ContainerType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.CONTAINERS, EffortlessBuilding.MODID);

	public static final RegistryObject<Item> RANDOMIZER_BAG_ITEM = ITEMS.register("randomizer_bag", RandomizerBagItem::new);
	public static final RegistryObject<Item> GOLDEN_RANDOMIZER_BAG_ITEM = ITEMS.register("golden_randomizer_bag", GoldenRandomizerBagItem::new);
	public static final RegistryObject<Item> DIAMOND_RANDOMIZER_BAG_ITEM = ITEMS.register("diamond_randomizer_bag", DiamondRandomizerBagItem::new);
	public static final RegistryObject<Item> REACH_UPGRADE_1_ITEM = ITEMS.register("reach_upgrade1", ReachUpgrade1Item::new);
	public static final RegistryObject<Item> REACH_UPGRADE_2_ITEM = ITEMS.register("reach_upgrade2", ReachUpgrade2Item::new);
	public static final RegistryObject<Item> REACH_UPGRADE_3_ITEM = ITEMS.register("reach_upgrade3", ReachUpgrade3Item::new);

	public static final RegistryObject<ContainerType<RandomizerBagContainer>> RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("randomizer_bag", () -> registerContainer(RandomizerBagContainer::new));
	public static final RegistryObject<ContainerType<GoldenRandomizerBagContainer>> GOLDEN_RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("golden_randomizer_bag", () -> registerContainer(GoldenRandomizerBagContainer::new));
	public static final RegistryObject<ContainerType<DiamondRandomizerBagContainer>> DIAMOND_RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("diamond_randomizer_bag", () -> registerContainer(DiamondRandomizerBagContainer::new));

	public EffortlessBuilding() {
		instance = this;

		// Register ourselves for server and other game events we are interested in
		FMLJavaModLoadingContext.get().getModEventBus().register(this);

		ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
		CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());

		//Register config
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, BuildConfig.spec);
	}

	public static <T extends Container> ContainerType<T> registerContainer(IContainerFactory<T> fact){
		ContainerType<T> type = new ContainerType<T>(fact);
		return type;
	}

	public static void log(String msg) {
		logger.info(msg);
	}

	public static void log(PlayerEntity player, String msg) {
		log(player, msg, false);
	}

	public static void log(PlayerEntity player, String msg, boolean actionBar) {
		player.displayClientMessage(new StringTextComponent(msg), actionBar);
	}

	//Log with translation supported, call either on client or server (which then sends a message)
	public static void logTranslate(PlayerEntity player, String prefix, String translationKey, String suffix, boolean actionBar) {
		proxy.logTranslate(player, prefix, translationKey, suffix, actionBar);
	}

	@SubscribeEvent
	public void setup(final FMLCommonSetupEvent event) {
		CapabilityManager.INSTANCE.register(ModifierCapabilityManager.IModifierCapability.class, new ModifierCapabilityManager.Storage(), ModifierCapabilityManager.ModifierCapability::new);
		CapabilityManager.INSTANCE.register(ModeCapabilityManager.IModeCapability.class, new ModeCapabilityManager.Storage(), ModeCapabilityManager.ModeCapability::new);

		PacketHandler.register();

		//TODO 1.13 config
//        ConfigManager.sync(MODID, Config.Type.INSTANCE);

		proxy.setup(event);

		CompatHelper.setup();
	}

	@SubscribeEvent
	public void clientSetup(final FMLClientSetupEvent event) {

		proxy.clientSetup(event);
	}
}
