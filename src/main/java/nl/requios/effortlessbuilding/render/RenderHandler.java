package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.create.CreateClient;
import nl.requios.effortlessbuilding.create.foundation.render.SuperRenderTypeBuffer;
import nl.requios.effortlessbuilding.create.foundation.utility.AnimationTickHolder;

import static net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_PARTICLES;

/***
 * Main render class for Effortless Building
 */
@EventBusSubscriber(Dist.CLIENT)
public class RenderHandler {

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent event) {
		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
//		float pt = AnimationTickHolder.getPartialTicks();

		PoseStack ms = event.getPoseStack();
		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(bufferBuilder);

//		SuperRenderTypeBuffer superBuffer = SuperRenderTypeBuffer.getInstance();

		Player player = Minecraft.getInstance().player;
		ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);

		ms.pushPose();
		ms.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		//Mirror and radial mirror lines and areas
		ModifierRenderer.render(ms, buffer, modifierSettings);

		//Render block previews
		BlockPreviewRenderer.render(ms, buffer, player, modifierSettings, modeSettings);

		//Create
//		CreateClient.GHOST_BLOCKS.renderAll(ms, superBuffer);
//		EffortlessBuildingClient.OUTLINER.renderOutlines(ms, superBuffer, pt);
//		superBuffer.draw();
//		RenderSystem.enableCull();

		ms.popPose();
	}

	protected static VertexConsumer beginLines(MultiBufferSource.BufferSource renderTypeBuffer) {
		return renderTypeBuffer.getBuffer(BuildRenderTypes.LINES);
	}

	protected static void endLines(MultiBufferSource.BufferSource renderTypeBuffer) {
		renderTypeBuffer.endBatch();
	}

	protected static VertexConsumer beginPlanes(MultiBufferSource.BufferSource renderTypeBuffer) {
		return renderTypeBuffer.getBuffer(BuildRenderTypes.PLANES);
	}

	protected static void endPlanes(MultiBufferSource.BufferSource renderTypeBuffer) {
		renderTypeBuffer.endBatch();
	}

	protected static void renderBlockPreview(PoseStack matrixStack, MultiBufferSource.BufferSource renderTypeBuffer, BlockRenderDispatcher dispatcher,
											 BlockPos blockPos, BlockState blockState, float dissolve, BlockPos firstPos, BlockPos secondPos, boolean red) {
		if (blockState == null) return;

		matrixStack.pushPose();
		matrixStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
//        matrixStack.rotate(Vector3f.YP.rotationDegrees(-90f));
		matrixStack.translate(-0.01f, -0.01f, -0.01f);
		matrixStack.scale(1.02f, 1.02f, 1.02f);

		//Begin block preview rendering
		RenderType blockPreviewRenderType = BuildRenderTypes.getBlockPreviewRenderType(dissolve, blockPos, firstPos, secondPos, red);
		VertexConsumer buffer = renderTypeBuffer.getBuffer(blockPreviewRenderType);

		try {
			BakedModel model = dispatcher.getBlockModel(blockState);
			dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer, blockState, model,
					1f, 1f, 1f, 0, OverlayTexture.NO_OVERLAY, ModelData.EMPTY, blockPreviewRenderType);
		} catch (NullPointerException e) {
			EffortlessBuilding.logger.warn("RenderHandler::renderBlockPreview cannot render " + blockState.getBlock().toString());

			//Render outline as backup, escape out of the current renderstack
			matrixStack.popPose();
			renderTypeBuffer.endBatch();
			VertexConsumer lineBuffer = beginLines(renderTypeBuffer);
			renderBlockOutline(matrixStack, lineBuffer, blockPos, new Vec3(1f, 1f, 1f));
			endLines(renderTypeBuffer);
			buffer = renderTypeBuffer.getBuffer(Sheets.translucentCullBlockSheet()); //any type will do, as long as we have something on the stack
			matrixStack.pushPose();
		}

		renderTypeBuffer.endBatch();
		matrixStack.popPose();
	}

	protected static void renderBlockOutline(PoseStack matrixStack, VertexConsumer buffer, BlockPos pos, Vec3 color) {
		renderBlockOutline(matrixStack, buffer, pos, pos, color);
	}

	//Renders outline. Pos1 has to be minimal x,y,z and pos2 maximal x,y,z
	protected static void renderBlockOutline(PoseStack matrixStack, VertexConsumer buffer, BlockPos pos1, BlockPos pos2, Vec3 color) {
		AABB aabb = new AABB(pos1, pos2.offset(1, 1, 1)).inflate(0.0020000000949949026);

		LevelRenderer.renderLineBox(matrixStack, buffer, aabb, (float) color.x, (float) color.y, (float) color.z, 0.4f);
//        WorldRenderer.drawSelectionBoundingBox(aabb, (float) color.x, (float) color.y, (float) color.z, 0.4f);
	}

	//Renders outline with given bounding box
	protected static void renderBlockOutline(PoseStack matrixStack, VertexConsumer buffer, BlockPos pos, VoxelShape collisionShape, Vec3 color) {
//        WorldRenderer.drawShape(collisionShape, pos.getX(), pos.getY(), pos.getZ(), (float) color.x, (float) color.y, (float) color.z, 0.4f);
		LevelRenderer.renderVoxelShape(matrixStack, buffer, collisionShape, pos.getX(), pos.getY(), pos.getZ(), (float) color.x, (float) color.y, (float) color.z, 0.4f);
	}

	//TODO
	//Sends breaking progress for all coordinates to renderglobal, so all blocks get visually broken
//    @Override
//    public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress) {
//        Minecraft mc = Minecraft.getInstance();
//
//        ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(mc.player);
//        if (!BuildModifiers.isEnabled(modifierSettings, pos)) return;
//
//        List<BlockPos> coordinates = BuildModifiers.findCoordinates(mc.player, pos);
//        for (int i = 1; i < coordinates.size(); i++) {
//            BlockPos coordinate = coordinates.get(i);
//            if (SurvivalHelper.canBreak(mc.world, mc.player, coordinate)) {
//                //Send i as entity id because only one block can be broken per id
//                //Unless i happens to be the player id, then take something else
//                int fakeId = mc.player.getEntityId() != i ? i : coordinates.size();
//                mc.renderGlobal.sendBlockBreakProgress(fakeId, coordinate, progress);
//            }
//        }
//    }
}
