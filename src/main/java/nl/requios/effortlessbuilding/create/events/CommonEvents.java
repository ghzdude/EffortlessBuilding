package nl.requios.effortlessbuilding.create.events;

import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import nl.requios.effortlessbuilding.create.foundation.utility.WorldAttached;

@EventBusSubscriber
public class CommonEvents {

	@SubscribeEvent
	public static void onUnloadWorld(LevelEvent.Unload event) {
		LevelAccessor world = event.getLevel();
		WorldAttached.invalidateWorld(world);
	}


	@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
	public static class ModBusEvents {

//		@SubscribeEvent
//		public static void addPackFinders(AddPackFindersEvent event) {
//			if (event.getPackType() == PackType.CLIENT_RESOURCES) {
//				IModFileInfo modFileInfo = ModList.get().getModFileById(Create.ID);
//				if (modFileInfo == null) {
//					Create.LOGGER.error("Could not find Create mod file info; built-in resource packs will be missing!");
//					return;
//				}
//				IModFile modFile = modFileInfo.getFile();
//				event.addRepositorySource((consumer, constructor) -> {
//					consumer.accept(Pack.create(Create.asResource("legacy_copper").toString(), false, () -> new ModFilePackResources("Create Legacy Copper", modFile, "resourcepacks/legacy_copper"), constructor, Pack.Position.TOP, PackSource.DEFAULT));
//				});
//			}
//		}

	}

}
