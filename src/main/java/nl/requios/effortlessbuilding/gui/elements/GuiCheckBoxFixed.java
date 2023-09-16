package nl.requios.effortlessbuilding.gui.elements;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.gui.ScreenUtils;

import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This class provides a checkbox style control.
 */
@ParametersAreNonnullByDefault
public class GuiCheckBoxFixed extends Button {
	private final int boxWidth;
	private boolean isChecked;

	public GuiCheckBoxFixed(int xPos, int yPos, String displayString, boolean isChecked) {
		super(xPos, yPos, Minecraft.getInstance().font.width(displayString) + 2 + 11, 11, Component.literal(displayString), b -> {
		});
		this.isChecked = isChecked;
		this.boxWidth = 11;
		this.height = 11;
		this.width = this.boxWidth + 2 + Minecraft.getInstance().font.width(displayString);
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partial) {
		if (this.visible) {
			Minecraft mc = Minecraft.getInstance();
			this.isHovered = mouseX >= this.getX() && mouseY >= this.getY() && mouseX < this.getX() + this.boxWidth && mouseY < this.getY() + this.height;
			graphics.blitWithBorder(WIDGETS_LOCATION, this.getX(), this.getY(), 0, 46, this.boxWidth, this.height, 200, 20, 2, 3, 2, 2);
			this.renderBg(graphics, mc, mouseX, mouseY);
			int color = 14737632;

			if (packedFGColor != 0) {
				color = packedFGColor;
			} else if (!this.active) {
				color = 10526880;
			}

			if (this.isChecked)
				graphics.drawCenteredString(mc.font, "x", this.getX() + this.boxWidth / 2 + 1, this.getY() + 1, 14737632);

			graphics.drawString(mc.font, getMessage(), this.getX() + this.boxWidth + 2, this.getY() + 2, color);
		}
	}

	@Override
	public void onPress() {
		this.isChecked = !this.isChecked;
	}

	public boolean isChecked() {
		return this.isChecked;
	}

	public void setIsChecked(boolean isChecked) {
		this.isChecked = isChecked;
	}
}