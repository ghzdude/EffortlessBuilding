package nl.requios.effortlessbuilding;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RegisterShadersEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nl.requios.effortlessbuilding.proxy.ClientProxy;
import nl.requios.effortlessbuilding.render.BuildRenderTypes;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class ModClientEventHandler {

	@SubscribeEvent
	public static void registerKeyMappings(RegisterKeyMappingsEvent event) {
		EffortlessBuilding.log("Registering KeyMappings!");

		// register key bindings
		ClientProxy.keyBindings = new KeyMapping[6];

		// instantiate the key bindings
		ClientProxy.keyBindings[0] = new KeyMapping("key.effortlessbuilding.hud.desc", KeyConflictContext.IN_GAME, InputConstants.getKey(GLFW.GLFW_KEY_KP_ADD, 0), "key.effortlessbuilding.category");
		ClientProxy.keyBindings[1] = new KeyMapping("key.effortlessbuilding.replace.desc", KeyConflictContext.IN_GAME, InputConstants.getKey(GLFW.GLFW_KEY_KP_SUBTRACT, 0), "key.effortlessbuilding.category");
		ClientProxy.keyBindings[2] = new KeyMapping("key.effortlessbuilding.mode.desc", KeyConflictContext.IN_GAME, InputConstants.getKey(GLFW.GLFW_KEY_LEFT_ALT, 0), "key.effortlessbuilding.category");
		ClientProxy.keyBindings[3] = new KeyMapping("key.effortlessbuilding.undo.desc", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputConstants.getKey(GLFW.GLFW_KEY_Z, 0), "key.effortlessbuilding.category");
		ClientProxy.keyBindings[4] = new KeyMapping("key.effortlessbuilding.redo.desc", KeyConflictContext.IN_GAME, KeyModifier.CONTROL, InputConstants.getKey(GLFW.GLFW_KEY_Y, 0), "key.effortlessbuilding.category");
		ClientProxy.keyBindings[5] = new KeyMapping("key.effortlessbuilding.altplacement.desc", KeyConflictContext.IN_GAME, InputConstants.getKey(GLFW.GLFW_KEY_LEFT_CONTROL, 0), "key.effortlessbuilding.category");

		for (KeyMapping keyBinding : ClientProxy.keyBindings) {
			event.register(keyBinding);
		}
	}

	@SubscribeEvent
	public static void registerShaders(RegisterShadersEvent event) throws IOException {
		event.registerShader(new ShaderInstance(event.getResourceProvider(),
				new ResourceLocation(EffortlessBuilding.MODID, "dissolve"),
				DefaultVertexFormat.BLOCK),
				shaderInstance -> BuildRenderTypes.dissolveShaderInstance = shaderInstance);
	}
}
