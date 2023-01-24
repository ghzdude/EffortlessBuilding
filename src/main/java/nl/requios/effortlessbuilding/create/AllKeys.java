package nl.requios.effortlessbuilding.create;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

public class AllKeys {

	public static boolean isKeyDown(int key) {
		return InputConstants.isKeyDown(Minecraft.getInstance()
			.getWindow()
			.getWindow(), key);
	}

	public static boolean isMouseButtonDown(int button) {
		return GLFW.glfwGetMouseButton(Minecraft.getInstance()
			.getWindow()
			.getWindow(), button) == 1;
	}

	public static boolean ctrlDown() {
		return Screen.hasControlDown();
	}

	public static boolean shiftDown() {
		return Screen.hasShiftDown();
	}

	public static boolean altDown() {
		return Screen.hasAltDown();
	}

}
