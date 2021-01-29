package nl.requios.effortlessbuilding.gui.elements;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
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
	List<ITextComponent> tooltip = new ArrayList<>();
	private boolean useAltIcon = false;

	public GuiIconButton(int x, int y, int iconX, int iconY, ResourceLocation resourceLocation, Button.IPressable onPress) {
		this(x, y, 20, 20, iconX, iconY, 20, 20, 20, 0, resourceLocation, onPress);
	}

	public GuiIconButton(int x, int y, int width, int height, int iconX, int iconY, int iconWidth, int iconHeight, int iconAltX, int iconAltY, ResourceLocation resourceLocation, Button.IPressable onPress) {
		super(x, y, width, height, StringTextComponent.EMPTY, onPress);
		this.iconX = iconX;
		this.iconY = iconY;
		this.iconWidth = iconWidth;
		this.iconHeight = iconHeight;
		this.iconAltX = iconAltX;
		this.iconAltY = iconAltY;
		this.resourceLocation = resourceLocation;
	}

	public void setTooltip(ITextComponent tooltip) {
		setTooltip(Collections.singletonList(tooltip));
	}

	public void setTooltip(List<ITextComponent> tooltip) {
		this.tooltip = tooltip;
	}

	public void setUseAlternateIcon(boolean useAlternateIcon) {
		this.useAltIcon = useAlternateIcon;
	}

	@Override
	public void render(MatrixStack ms, int mouseX, int mouseY, float partialTicks) {
		super.render(ms, mouseX, mouseY, partialTicks);
		if (this.visible) {
			this.isHovered = mouseX >= this.x && mouseY >= this.y && mouseX < this.x + this.width && mouseY < this.y + this.height;
			Minecraft.getInstance().getTextureManager().bindTexture(this.resourceLocation);
			int currentIconX = this.iconX;
			int currentIconY = this.iconY;

			if (useAltIcon) {
				currentIconX += iconAltX;
				currentIconY += iconAltY;
			}

			//Draws a textured rectangle at the current z-value. Used to be drawTexturedModalRect in Gui.
			this.blit(ms, this.x, this.y, currentIconX, currentIconY, this.iconWidth, this.iconHeight);
		}
	}

	public void drawTooltip(MatrixStack ms, Screen screen, int mouseX, int mouseY) {
		boolean flag = mouseX >= x && mouseX < x + width && mouseY >= y && mouseY < y + height;

		if (flag) {
			screen.func_243308_b(ms, tooltip, mouseX - 10, mouseY + 25);
		}
	}
}
