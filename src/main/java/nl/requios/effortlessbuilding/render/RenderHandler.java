package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.systems.BuilderChain;

/***
 * Main render class for Effortless Building
 */
@EventBusSubscriber(Dist.CLIENT)
public class RenderHandler {

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

		PoseStack ms = event.getPoseStack();
		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(bufferBuilder);

		Player player = Minecraft.getInstance().player;
		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);

		ms.pushPose();
		ms.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		//Mirror and radial mirror lines and areas
		ModifierRenderer.render(ms, buffer, modifierSettings);

		ms.popPose();

		renderSubText(ms);
	}

	@SubscribeEvent
	public static void onRenderGuiEvent(RenderGuiEvent event) {
		renderSubText(event.getPoseStack());
	}

	private static final ChatFormatting highlightColor = ChatFormatting.BLUE;
	private static final ChatFormatting normalColor = ChatFormatting.WHITE;
	private static final Component placingText = Component.literal(
			normalColor + "Left-click to " + highlightColor + "cancel, " +
			normalColor + "Right-click to " + highlightColor + "place");

	private static final Component breakingText = Component.literal(
			normalColor + "Left-click to " + highlightColor + "break, " +
			normalColor + "Right-click to " + highlightColor + "cancel");

	private static void renderSubText(PoseStack ms) {
		var state = EffortlessBuildingClient.BUILDER_CHAIN.getState();
		if (state == BuilderChain.State.IDLE) return;

		var text = state == BuilderChain.State.PLACING ? placingText : breakingText;

		int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
		int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
		var font = Minecraft.getInstance().font;

		ms.pushPose();
		ms.translate((double)(screenWidth / 2), (double)(screenHeight - 54), 0.0D);
		RenderSystem.enableBlend();
		RenderSystem.defaultBlendFunc();
		int l = font.width(text);
		font.drawShadow(ms, text, (float)(-l / 2), -4.0F, 0xffffffff);
		RenderSystem.disableBlend();
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

}
