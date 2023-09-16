package nl.requios.effortlessbuilding.gui.elements;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.components.Button;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
public class GuiIconButton extends Button {

	private final ResourceLocation resourceLocation;
	private final int iconX, iconY, iconWidth, iconHeight, iconAltX, iconAltY;
	List<Component> tooltip = new ArrayList<>();
	private boolean useAltIcon = false;

	public GuiIconButton(int x, int y, int iconX, int iconY, ResourceLocation resourceLocation, Button.OnPress onPress) {
		this(x, y, 20, 20, iconX, iconY, 20, 20, 20, 0, resourceLocation, onPress);
	}

	public GuiIconButton(int x, int y, int width, int height, int iconX, int iconY, int iconWidth, int iconHeight, int iconAltX, int iconAltY, ResourceLocation resourceLocation, Button.OnPress onPress) {
		super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
		this.iconX = iconX;
		this.iconY = iconY;
		this.iconWidth = iconWidth;
		this.iconHeight = iconHeight;
		this.iconAltX = iconAltX;
		this.iconAltY = iconAltY;
		this.resourceLocation = resourceLocation;
	}

	public void setTooltip(Component tooltip) {
		setTooltip(Collections.singletonList(tooltip));
	}

	public void setTooltip(List<Component> tooltip) {
		this.tooltip = tooltip;
	}

	public void setUseAlternateIcon(boolean useAlternateIcon) {
		this.useAltIcon = useAlternateIcon;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		super.render(graphics, mouseX, mouseY, partialTicks);
		if (this.visible) {
			this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.width && mouseY < this.getY() + this.height;

			int currentIconX = this.iconX;
			int currentIconY = this.iconY;

			if (useAltIcon) {
				currentIconX += iconAltX;
				currentIconY += iconAltY;
			}

			//Draws a textured rectangle at the current z-value. Used to be drawTexturedModalRect in Gui.
			graphics.blit(this.resourceLocation, this.getX(), this.getY(), currentIconX, currentIconY, this.iconWidth, this.iconHeight);
		}
	}

	public void drawTooltip(GuiGraphics graphics, Screen screen, int mouseX, int mouseY) {
		boolean flag = mouseX >= getX() && mouseX < getY() + width && mouseY >= getY() && mouseY < getY() + height;

		if (flag) {
			graphics.renderComponentTooltip(Minecraft.getInstance().font, tooltip, mouseX - 10, mouseY + 25);
		}
	}
}
