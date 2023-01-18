package nl.requios.effortlessbuilding.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import nl.requios.effortlessbuilding.ClientEvents;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.utilities.SurvivalHelper;

/***
 * Main render class for Effortless Building
 */
@EventBusSubscriber(Dist.CLIENT)
public class RenderHandler {

	@SubscribeEvent
	public static void onTick(TickEvent.ClientTickEvent event) {
		if (!nl.requios.effortlessbuilding.create.events.ClientEvents.isGameActive()) return;

		var player = Minecraft.getInstance().player;
		var modeSettings = ModeSettingsManager.getModeSettings(player);
		var modifierSettings = ModifierSettingsManager.getModifierSettings(player);

		BlockPreviews.drawPlacedBlocks(player, modifierSettings);


		HitResult lookingAt = ClientEvents.getLookingAt(player);
		if (modeSettings.getBuildMode() == BuildModes.BuildModeEnum.DISABLED)
			lookingAt = Minecraft.getInstance().hitResult;

		ItemStack mainhand = player.getMainHandItem();
		boolean noBlockInHand = !(!mainhand.isEmpty() && CompatHelper.isItemBlockProxy(mainhand));

		//Find start position, side hit and hit vector
		BlockPos startPos = null;
		Direction sideHit = null;
		Vec3 hitVec = null;

		//Checking for null is necessary! Even in vanilla when looking down ladders it is occasionally null (instead of Type MISS)
		if (lookingAt != null && lookingAt.getType() == HitResult.Type.BLOCK) {
			BlockHitResult blockLookingAt = (BlockHitResult) lookingAt;
			startPos = blockLookingAt.getBlockPos();

			//Check if tool (or none) in hand
			boolean replaceable = player.level.getBlockState(startPos).getMaterial().isReplaceable();
			boolean becomesDoubleSlab = SurvivalHelper.doesBecomeDoubleSlab(player, startPos);
			if (!modifierSettings.doQuickReplace() && !noBlockInHand && !replaceable && !becomesDoubleSlab) {
				startPos = startPos.relative(blockLookingAt.getDirection());
			}

			//Get under tall grass and other replaceable blocks
			if (modifierSettings.doQuickReplace() && !noBlockInHand && replaceable) {
				startPos = startPos.below();
			}

			sideHit = blockLookingAt.getDirection();
			hitVec = blockLookingAt.getLocation();
		}


		BlockPreviews.drawLookAtPreview(player, modeSettings, modifierSettings, startPos, sideHit, hitVec);

		if (noBlockInHand) BlockPreviews.drawOutlinesIfNoBlockInHand(player, lookingAt);
	}

	@SubscribeEvent
	public static void onRender(RenderLevelStageEvent event) {
		if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

		Vec3 cameraPos = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();

		PoseStack ms = event.getPoseStack();
		BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
		MultiBufferSource.BufferSource buffer = MultiBufferSource.immediate(bufferBuilder);

		Player player = Minecraft.getInstance().player;
		ModeSettingsManager.ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);
		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);

		ms.pushPose();
		ms.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

		//Mirror and radial mirror lines and areas
		ModifierRenderer.render(ms, buffer, modifierSettings);

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
