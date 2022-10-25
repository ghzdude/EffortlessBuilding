package nl.requios.effortlessbuilding;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nl.requios.effortlessbuilding.render.BuildRenderTypes;

import java.io.IOException;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class ModClientEventHandler {

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceManager(),
				new ResourceLocation(EffortlessBuilding.MODID, "dissolve"),
				DefaultVertexFormat.BLOCK),
				shaderInstance -> BuildRenderTypes.dissolveShaderInstance = shaderInstance);
	}
}
