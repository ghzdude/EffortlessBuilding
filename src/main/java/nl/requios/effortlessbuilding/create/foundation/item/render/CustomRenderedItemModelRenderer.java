package nl.requios.effortlessbuilding.create.foundation.item.render;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public abstract class CustomRenderedItemModelRenderer extends BlockEntityWithoutLevelRenderer {

	public CustomRenderedItemModelRenderer() {
		super(null, null);
	}

	@Override
	public void renderByItem(ItemStack stack, ItemDisplayContext transformType, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
		CustomRenderedItemModel mainModel = (CustomRenderedItemModel) Minecraft.getInstance()
			.getItemRenderer()
			.getModel(stack, null, null, 0);
		PartialItemModelRenderer renderer = PartialItemModelRenderer.of(stack, transformType, ms, buffer, overlay);

		ms.pushPose();
		ms.translate(0.5F, 0.5F, 0.5F);
		render(stack, mainModel, renderer, transformType, ms, buffer, light, overlay);
		ms.popPose();
	}

	protected abstract void render(ItemStack stack, CustomRenderedItemModel model, PartialItemModelRenderer renderer, ItemDisplayContext transformType,
		PoseStack ms, MultiBufferSource buffer, int light, int overlay);

}
