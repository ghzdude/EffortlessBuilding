package nl.requios.effortlessbuilding.create.foundation;

import nl.requios.effortlessbuilding.create.CreateClient;
//import nl.requios.effortlessbuilding.create.foundation.sound.SoundScapes;
import nl.requios.effortlessbuilding.create.foundation.utility.LangNumberFormat;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public class ClientResourceReloadListener implements ResourceManagerReloadListener {

	@Override
	public void onResourceManagerReload(ResourceManager resourceManager) {
		CreateClient.invalidateRenderers();
//		SoundScapes.invalidateAll();
		LangNumberFormat.numberFormat.update();
	}

}
