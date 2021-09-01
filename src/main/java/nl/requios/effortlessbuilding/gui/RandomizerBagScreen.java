package nl.requios.effortlessbuilding.gui;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.EffortlessBuilding;

import javax.annotation.ParametersAreNonnullByDefault;

@OnlyIn(Dist.CLIENT)
@ParametersAreNonnullByDefault
public class RandomizerBagScreen extends AbstractContainerScreen<RandomizerBagContainer> {
	private static final ResourceLocation guiTextures =
		new ResourceLocation(EffortlessBuilding.MODID, "textures/gui/container/randomizerbag.png");

	public RandomizerBagScreen(RandomizerBagContainer randomizerBagContainer, Inventory playerInventory, Component title) {
		super(randomizerBagContainer, playerInventory, title);//new TranslationTextComponent("effortlessbuilding.screen.randomizer_bag"));
		imageHeight = 134;
	}

	@Override
	public void render(PoseStack ms, int mouseX, int mouseY, float partialTicks) {
		renderBackground(ms);
		super.render(ms, mouseX, mouseY, partialTicks);
		this.renderTooltip(ms, mouseX, mouseY);
	}

	@Override
	protected void renderLabels(PoseStack ms, int mouseX, int mouseY) {
		font.drawShadow(ms, this.title, 8, 6, 0x404040);
		font.drawShadow(ms, inventory.getDisplayName(), 8, imageHeight - 96 + 2, 0x404040);
	}

	@Override
	protected void renderBg(PoseStack ms, float partialTicks, int mouseX, int mouseY) {
		RenderSystem.color3f(1.0F, 1.0F, 1.0F);
		minecraft.getTextureManager().bind(guiTextures);
		int marginHorizontal = (width - imageWidth) / 2;
		int marginVertical = (height - imageHeight) / 2;
		blit(ms, marginHorizontal, marginVertical, 0, 0, imageWidth, imageHeight);
	}
}
