package nl.requios.effortlessbuilding;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.IContainerFactory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.gui.DiamondRandomizerBagContainer;
import nl.requios.effortlessbuilding.gui.GoldenRandomizerBagContainer;
import nl.requios.effortlessbuilding.gui.RandomizerBagContainer;
import nl.requios.effortlessbuilding.systems.DelayedBlockPlacer;
import nl.requios.effortlessbuilding.item.*;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.proxy.ClientProxy;
import nl.requios.effortlessbuilding.proxy.IProxy;
import nl.requios.effortlessbuilding.proxy.ServerProxy;
import nl.requios.effortlessbuilding.systems.ServerBlockPlacer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(EffortlessBuilding.MODID)
public class EffortlessBuilding {

	public static final String MODID = "effortlessbuilding";
	public static final Logger logger = LogManager.getLogger();

	public static EffortlessBuilding instance;
	public static IProxy proxy = DistExecutor.unsafeRunForDist(() -> ClientProxy::new, () -> ServerProxy::new);

	public static final ServerBlockPlacer SERVER_BLOCK_PLACER = new ServerBlockPlacer();
	public static final DelayedBlockPlacer DELAYED_BLOCK_PLACER = new DelayedBlockPlacer();

	//Registration
	private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
	private static final DeferredRegister<MenuType<?>> CONTAINERS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, EffortlessBuilding.MODID);

	public static final RegistryObject<Item> RANDOMIZER_BAG_ITEM = ITEMS.register("randomizer_bag", RandomizerBagItem::new);
	public static final RegistryObject<Item> GOLDEN_RANDOMIZER_BAG_ITEM = ITEMS.register("golden_randomizer_bag", GoldenRandomizerBagItem::new);
	public static final RegistryObject<Item> DIAMOND_RANDOMIZER_BAG_ITEM = ITEMS.register("diamond_randomizer_bag", DiamondRandomizerBagItem::new);
	public static final RegistryObject<Item> REACH_UPGRADE_1_ITEM = ITEMS.register("reach_upgrade1", ReachUpgrade1Item::new);
	public static final RegistryObject<Item> REACH_UPGRADE_2_ITEM = ITEMS.register("reach_upgrade2", ReachUpgrade2Item::new);
	public static final RegistryObject<Item> REACH_UPGRADE_3_ITEM = ITEMS.register("reach_upgrade3", ReachUpgrade3Item::new);

	public static final RegistryObject<MenuType<RandomizerBagContainer>> RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("randomizer_bag", () -> registerContainer(RandomizerBagContainer::new));
	public static final RegistryObject<MenuType<GoldenRandomizerBagContainer>> GOLDEN_RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("golden_randomizer_bag", () -> registerContainer(GoldenRandomizerBagContainer::new));
	public static final RegistryObject<MenuType<DiamondRandomizerBagContainer>> DIAMOND_RANDOMIZER_BAG_CONTAINER = CONTAINERS.register("diamond_randomizer_bag", () -> registerContainer(DiamondRandomizerBagContainer::new));

	public EffortlessBuilding() {
		instance = this;

		ModLoadingContext modLoadingContext = ModLoadingContext.get();
		IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		IEventBus forgeEventBus = MinecraftForge.EVENT_BUS;

		modEventBus.addListener(EffortlessBuilding::setup);

		DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> EffortlessBuildingClient.onConstructorClient(modEventBus, forgeEventBus));

		ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
		CONTAINERS.register(FMLJavaModLoadingContext.get().getModEventBus());

		//Register config
		ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CommonConfig.spec);
		ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.spec);
	}

	public static void setup(final FMLCommonSetupEvent event) {
		PacketHandler.register();

		CompatHelper.setup();
	}

	public static <T extends AbstractContainerMenu> MenuType<T> registerContainer(IContainerFactory<T> fact){
		MenuType<T> type = new MenuType<T>(fact);
		return type;
	}

	public static void log(String msg) {
		logger.info(msg);
	}

	public static void log(Player player, String msg) {
		log(player, msg, false);
	}

	public static void log(Player player, String msg, boolean actionBar) {
		player.displayClientMessage(Component.literal(msg), actionBar);
	}

	//Log with translation supported, call either on client or server (which then sends a message)
	public static void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar) {
		proxy.logTranslate(player, prefix, translationKey, suffix, actionBar);
	}

}
