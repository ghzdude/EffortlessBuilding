package nl.requios.effortlessbuilding.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.EffortlessBuilding;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
public class DiamondRandomizerBagScreen extends AbstractContainerScreen<DiamondRandomizerBagContainer> {
	private Inventory inventory;

	private static final ResourceLocation guiTextures = new ResourceLocation(EffortlessBuilding.MODID, "textures/gui/container/diamondrandomizerbag.png");

	public DiamondRandomizerBagScreen(DiamondRandomizerBagContainer randomizerBagContainer, Inventory playerInventory, Component title) {
		super(randomizerBagContainer, playerInventory, title);
		this.inventory = playerInventory;
		imageHeight = 167;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		renderBackground(graphics);
		super.render(graphics, mouseX, mouseY, partialTicks);
		this.renderTooltip(graphics, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
		graphics.drawString(this.font, this.title, 8, 6, 0x404040);
		graphics.drawString(this.font, this.playerInventoryTitle, 8, imageHeight - 96 + 2, 0x404040);
	}

	@Override
	protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1f);
		int marginHorizontal = (width - imageWidth) / 2;
		int marginVertical = (height - imageHeight) / 2;
		graphics.blit(guiTextures, marginHorizontal, marginVertical, 0, 0, imageWidth, imageHeight);
	}
}
