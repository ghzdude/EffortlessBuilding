package nl.requios.effortlessbuilding.create.foundation.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import nl.requios.effortlessbuilding.create.Create;
import nl.requios.effortlessbuilding.create.foundation.gui.element.ScreenElement;
import nl.requios.effortlessbuilding.create.foundation.utility.Color;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public enum AllGuiTextures implements ScreenElement {

	// Renderables
	BUTTON("widgets", 18, 18),
	BUTTON_HOVER("widgets", 18, 0, 18, 18),
	BUTTON_DOWN("widgets", 36, 0, 18, 18),
	INDICATOR("widgets", 0, 18, 18, 6),
	INDICATOR_WHITE("widgets", 18, 18, 18, 6),
	INDICATOR_GREEN("widgets", 36, 18, 18, 6),
	INDICATOR_YELLOW("widgets", 54, 18, 18, 6),
	INDICATOR_RED("widgets", 72, 18, 18, 6),

	HOTSLOT_ARROW("widgets", 24, 51, 20, 12),
	HOTSLOT("widgets", 0, 68, 22, 22),
	HOTSLOT_ACTIVE("widgets", 0, 46, 22, 22),
	HOTSLOT_SUPER_ACTIVE("widgets", 27, 67, 24, 24),

	SPEECH_TOOLTIP_BACKGROUND("widgets", 0, 24, 8, 8),
	SPEECH_TOOLTIP_COLOR("widgets", 8, 24, 8, 8),
	
	TRAIN_HUD_SPEED_BG("widgets", 0, 190, 182, 5),
	TRAIN_HUD_SPEED("widgets", 0, 185, 182, 5),
	TRAIN_HUD_THROTTLE("widgets", 0, 195, 182, 5),
	TRAIN_HUD_THROTTLE_POINTER("widgets", 0, 209, 6, 9),
	TRAIN_HUD_FRAME("widgets", 0, 200, 186, 7),
	TRAIN_HUD_DIRECTION("widgets", 77, 165, 28, 20),
	TRAIN_PROMPT_L("widgets", 8, 209, 3, 16),
	TRAIN_PROMPT_R("widgets", 11, 209, 3, 16),
	TRAIN_PROMPT("widgets", 0, 230, 256, 16),

	;

	public static final int FONT_COLOR = 0x575F7A;

	public final ResourceLocation location;
	public int width, height;
	public int startX, startY;

	private AllGuiTextures(String location, int width, int height) {
		this(location, 0, 0, width, height);
	}

	private AllGuiTextures(int startX, int startY) {
		this("icons", startX * 16, startY * 16, 16, 16);
	}

	private AllGuiTextures(String location, int startX, int startY, int width, int height) {
		this(Create.ID, location, startX, startY, width, height);
	}

	private AllGuiTextures(String namespace, String location, int startX, int startY, int width, int height) {
		this.location = new ResourceLocation(namespace, "textures/gui/" + location + ".png");
		this.width = width;
		this.height = height;
		this.startX = startX;
		this.startY = startY;
	}

	@OnlyIn(Dist.CLIENT)
	public void bind() {
		RenderSystem.setShaderTexture(0, location);
	}

	@OnlyIn(Dist.CLIENT)
	@Override
	public void render(PoseStack ms, int x, int y) {
		bind();
		GuiComponent.blit(ms, x, y, 0, startX, startY, width, height, 256, 256);
	}

	@OnlyIn(Dist.CLIENT)
	public void render(PoseStack ms, int x, int y, GuiComponent component) {
		bind();
		component.blit(ms, x, y, startX, startY, width, height);
	}

	@OnlyIn(Dist.CLIENT)
	public void render(PoseStack ms, int x, int y, Color c) {
		bind();
		UIRenderHelper.drawColoredTexture(ms, c, x, y, startX, startY, width, height);
	}

}
