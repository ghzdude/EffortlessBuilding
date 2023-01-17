package nl.requios.effortlessbuilding.create.foundation.render;

import com.jozufozu.flywheel.core.model.ModelUtil;
import com.jozufozu.flywheel.util.Pair;
import com.mojang.blaze3d.vertex.BufferBuilder.RenderedBuffer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;

public class BakedModelRenderHelper {

	public static SuperByteBuffer standardBlockRender(BlockState renderedState) {
		BlockRenderDispatcher dispatcher = Minecraft.getInstance()
			.getBlockRenderer();
		return standardModelRender(dispatcher.getBlockModel(renderedState), renderedState);
	}

	public static SuperByteBuffer standardModelRender(BakedModel model, BlockState referenceState) {
		return standardModelRender(model, referenceState, new PoseStack());
	}

	public static SuperByteBuffer standardModelRender(BakedModel model, BlockState referenceState, PoseStack ms) {
		Pair<RenderedBuffer, Integer> pair = ModelUtil.getBufferBuilder(model, referenceState, ms);
		return new SuperByteBuffer(pair.first(), pair.second());
	}

}
