package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;

/***
 * Main render class for Effortless Building
 */
@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class RenderHandler {

	@SubscribeEvent
	public static void onRender(RenderWorldLastEvent event) {
		if (event.getPhase() != EventPriority.NORMAL)
			return;

		MatrixStack matrixStack = event.getMatrixStack();
		BufferBuilder bufferBuilder = Tessellator.getInstance().getBuilder();
		IRenderTypeBuffer.Impl renderTypeBuffer = IRenderTypeBuffer.immediate(bufferBuilder);
		if (renderTypeBuffer == null)
			return;

		PlayerEntity player = Minecraft.getInstance().player;
		ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);

		Vector3d projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

		matrixStack.pushPose();
		matrixStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);

		//Mirror and radial mirror lines and areas
		ModifierRenderer.render(matrixStack, renderTypeBuffer, modifierSettings);

		//Render block previews
		BlockPreviewRenderer.render(matrixStack, renderTypeBuffer, player, modifierSettings, modeSettings);

		matrixStack.popPose();
	}

	protected static IVertexBuilder beginLines(IRenderTypeBuffer.Impl renderTypeBuffer) {
		return renderTypeBuffer.getBuffer(BuildRenderTypes.LINES);
	}

	protected static void endLines(IRenderTypeBuffer.Impl renderTypeBuffer) {
		renderTypeBuffer.endBatch();
	}

	protected static IVertexBuilder beginPlanes(IRenderTypeBuffer.Impl renderTypeBuffer) {
		return renderTypeBuffer.getBuffer(BuildRenderTypes.PLANES);
	}

	protected static void endPlanes(IRenderTypeBuffer.Impl renderTypeBuffer) {
		renderTypeBuffer.endBatch();
	}

	protected static void renderBlockPreview(MatrixStack matrixStack, IRenderTypeBuffer.Impl renderTypeBuffer, BlockRendererDispatcher dispatcher,
											 BlockPos blockPos, BlockState blockState, float dissolve, BlockPos firstPos, BlockPos secondPos, boolean red) {
		if (blockState == null) return;

		matrixStack.pushPose();
		matrixStack.translate(blockPos.getX(), blockPos.getY(), blockPos.getZ());
//        matrixStack.rotate(Vector3f.YP.rotationDegrees(-90f));
		matrixStack.translate(-0.01f, -0.01f, -0.01f);
		matrixStack.scale(1.02f, 1.02f, 1.02f);

		//Begin block preview rendering
		RenderType blockPreviewRenderType = BuildRenderTypes.getBlockPreviewRenderType(dissolve, blockPos, firstPos, secondPos, red);
		IVertexBuilder buffer = renderTypeBuffer.getBuffer(blockPreviewRenderType);

//        MinecraftServer server = Minecraft.getInstance().getIntegratedServer();
//        World world = DimensionManager.getWorld(server, DimensionType.OVERWORLD, false, true);

		try {
			IBakedModel model = dispatcher.getBlockModel(blockState);
			dispatcher.getModelRenderer().renderModel(matrixStack.last(), buffer,
				blockState, model, 1f, 1f, 1f, 0, OverlayTexture.NO_OVERLAY);
//        blockRendererDispatcher.getBlockModelRenderer().renderModel(world, blockRendererDispatcher.getModelForState(blockState),
//                blockState, logicPos, matrixStack, renderTypeBuffer.getBuffer(renderType), true, new Random(), blockState.getPositionRandom(logicPos), i);
		} catch (NullPointerException e) {
			EffortlessBuilding.logger.warn("RenderHandler::renderBlockPreview cannot render " + blockState.getBlock().toString());

			//Render outline as backup, escape out of the current renderstack
			matrixStack.popPose();
			renderTypeBuffer.endBatch();
			IVertexBuilder lineBuffer = beginLines(renderTypeBuffer);
			renderBlockOutline(matrixStack, lineBuffer, blockPos, new Vector3d(1f, 1f, 1f));
			endLines(renderTypeBuffer);
			buffer = renderTypeBuffer.getBuffer(Atlases.translucentCullBlockSheet()); //any type will do, as long as we have something on the stack
			matrixStack.pushPose();
		}

		renderTypeBuffer.endBatch();
		matrixStack.popPose();
	}

	protected static void renderBlockOutline(MatrixStack matrixStack, IVertexBuilder buffer, BlockPos pos, Vector3d color) {
		renderBlockOutline(matrixStack, buffer, pos, pos, color);
	}

	//Renders outline. Pos1 has to be minimal x,y,z and pos2 maximal x,y,z
	protected static void renderBlockOutline(MatrixStack matrixStack, IVertexBuilder buffer, BlockPos pos1, BlockPos pos2, Vector3d color) {
		AxisAlignedBB aabb = new AxisAlignedBB(pos1, pos2.offset(1, 1, 1)).inflate(0.0020000000949949026);

		WorldRenderer.renderLineBox(matrixStack, buffer, aabb, (float) color.x, (float) color.y, (float) color.z, 0.4f);
//        WorldRenderer.drawSelectionBoundingBox(aabb, (float) color.x, (float) color.y, (float) color.z, 0.4f);
	}

	//Renders outline with given bounding box
	protected static void renderBlockOutline(MatrixStack matrixStack, IVertexBuilder buffer, BlockPos pos, VoxelShape collisionShape, Vector3d color) {
//        WorldRenderer.drawShape(collisionShape, pos.getX(), pos.getY(), pos.getZ(), (float) color.x, (float) color.y, (float) color.z, 0.4f);
		WorldRenderer.renderVoxelShape(matrixStack, buffer, collisionShape, pos.getX(), pos.getY(), pos.getZ(), (float) color.x, (float) color.y, (float) color.z, 0.4f);
	}

	//TODO 1.14
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
