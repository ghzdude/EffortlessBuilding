package nl.requios.effortlessbuilding.render;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.*;
import nl.requios.effortlessbuilding.buildmode.BuildModeEnum;
import nl.requios.effortlessbuilding.buildmodifier.BuildModifiers;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.create.AllSpecialTextures;
import nl.requios.effortlessbuilding.create.CreateClient;
import nl.requios.effortlessbuilding.create.foundation.utility.Color;
import nl.requios.effortlessbuilding.item.AbstractRandomizerBagItem;
import nl.requios.effortlessbuilding.systems.BuilderChain;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.ReachHelper;
import nl.requios.effortlessbuilding.utilities.SurvivalHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BlockPreviews {
	private final List<PlacedBlocksEntry> placedBlocksList = new ArrayList<>();

	public void onTick() {
		var player = Minecraft.getInstance().player;

		drawPlacedBlocks(player);
		drawLookAtPreview(player);
		drawOutlinesIfNoBlockInHand(player);
	}

	public void drawPlacedBlocks(Player player) {
		//Render placed blocks with appear animation
		if (ClientConfig.visuals.showBlockPreviews.get()) {
			for (PlacedBlocksEntry placed : placedBlocksList) {

				int totalTime = placed.breaking ? CommonConfig.visuals.breakAnimationLength.get() : CommonConfig.visuals.appearAnimationLength.get();
				if (totalTime <= 0) continue;

				float dissolve = (ClientEvents.ticksInGame - placed.time) / (float) totalTime;
				renderBlockPreviews(placed.blocks, placed.breaking, dissolve);
			}
		}

		//Expire
		placedBlocksList.removeIf(placed -> {
			int totalTime = placed.breaking ? CommonConfig.visuals.breakAnimationLength.get() : CommonConfig.visuals.appearAnimationLength.get();
			return placed.time + totalTime < ClientEvents.ticksInGame;
		});
	}

	public void drawLookAtPreview(Player player) {
		if (EffortlessBuildingClient.BUILD_MODES.getBuildMode() == BuildModeEnum.DISABLED &&
			   !ClientConfig.visuals.alwaysShowBlockPreview.get()) return;

		var blocks = EffortlessBuildingClient.BUILDER_CHAIN.getBlocks();
		var state = EffortlessBuildingClient.BUILDER_CHAIN.getState();

		if (blocks.size() == 0) return;

		var coordinates = blocks.stream().map(block -> block.blockPos).toList();

		//Dont fade out the outline if we are still determining where to place
		//Every outline with same ID will not fade out (because it gets replaced)
		Object outlineID = "single";
		if (blocks.size() > 1) outlineID = blocks.get(0).blockPos;

		if (state != BuilderChain.State.BREAKING) {
			//Use fancy shader if config allows, otherwise outlines
			if (ClientConfig.visuals.showBlockPreviews.get() && blocks.size() < ClientConfig.visuals.maxBlockPreviews.get()) {
				renderBlockPreviews(blocks, false, 0f);

				CreateClient.OUTLINER.showCluster(outlineID, coordinates)
						.withFaceTexture(AllSpecialTextures.CHECKERED)
						.disableNormals()
						.lineWidth(1 / 32f)
						.colored(new Color(1f, 1f, 1f, 1f));
			} else {
				//Thicker outline without block previews
				CreateClient.OUTLINER.showCluster(outlineID, coordinates)
						.withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED)
						.disableNormals()
						.lineWidth(1 / 16f)
						.colored(new Color(1f, 1f, 1f, 1f));
			}

		} else {
			//Breaking
			CreateClient.OUTLINER.showCluster(outlineID, coordinates)
					.withFaceTexture(AllSpecialTextures.THIN_CHECKERED)
					.disableNormals()
					.lineWidth(1 / 16f)
					.colored(new Color(0.8f, 0.1f, 0.1f, 1f));
		}

		//Display block count and dimensions in actionbar
		if (state != BuilderChain.State.IDLE) {

			//Find min and max values (not simply firstPos and secondPos because that doesn't work with circles)
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
			for (BlockPos pos : coordinates) {
				if (pos.getX() < minX) minX = pos.getX();
				if (pos.getX() > maxX) maxX = pos.getX();
				if (pos.getY() < minY) minY = pos.getY();
				if (pos.getY() > maxY) maxY = pos.getY();
				if (pos.getZ() < minZ) minZ = pos.getZ();
				if (pos.getZ() > maxZ) maxZ = pos.getZ();
			}
			BlockPos dim = new BlockPos(maxX - minX + 1, maxY - minY + 1, maxZ - minZ + 1);

			String dimensions = "(";
			if (dim.getX() > 1) dimensions += dim.getX() + "x";
			if (dim.getZ() > 1) dimensions += dim.getZ() + "x";
			if (dim.getY() > 1) dimensions += dim.getY() + "x";
			dimensions = dimensions.substring(0, dimensions.length() - 1);
			if (dimensions.length() > 1) dimensions += ")";

			EffortlessBuilding.log(player, blocks.size() + " blocks " + dimensions, true);
		}
	}

	public void drawOutlinesIfNoBlockInHand(Player player) {
		ItemStack mainhand = player.getMainHandItem();
		HitResult lookingAt = ClientEvents.getLookingAt(player);
		if (EffortlessBuildingClient.BUILD_MODES.getBuildMode() == BuildModeEnum.DISABLED)
			lookingAt = Minecraft.getInstance().hitResult;

		boolean noBlockInHand = !(!mainhand.isEmpty() && CompatHelper.isItemBlockProxy(mainhand));
		if (!noBlockInHand) return;

		//Draw outlines if no block in hand
		//Find proper raytrace: either normal range or increased range depending on canBreakFar
		HitResult objectMouseOver = Minecraft.getInstance().hitResult;
		HitResult breakingRaytrace = ReachHelper.canBreakFar(player) ? lookingAt : objectMouseOver;

		if (player.isCreative() && breakingRaytrace != null && breakingRaytrace.getType() == HitResult.Type.BLOCK) {
			BlockHitResult blockBreakingRaytrace = (BlockHitResult) breakingRaytrace;
			List<BlockPos> breakCoordinates = BuildModifiers.findCoordinates(player, blockBreakingRaytrace.getBlockPos());

			//Only render first outline if further than normal reach
			if (objectMouseOver != null && objectMouseOver.getType() == HitResult.Type.BLOCK)
				breakCoordinates.remove(0);

			breakCoordinates.removeIf(pos -> {
				BlockState blockState = player.level.getBlockState(pos);
				if (blockState.isAir() || blockState.getMaterial().isLiquid()) return true;
				return !SurvivalHelper.canBreak(player.level, player, pos);
			});

			if (!breakCoordinates.isEmpty()) {
				CreateClient.OUTLINER.showCluster("break", breakCoordinates)
						.disableNormals()
						.lineWidth(1 / 64f)
						.colored(0x222222);
			}
		}
	}

	protected void renderBlockPreviews(List<BlockEntry> blocks, boolean breaking, float dissolve) {

		var firstPos = blocks.get(0).blockPos;
		var lastPos = blocks.get(blocks.size() - 1).blockPos;

		for (BlockEntry blockEntry : blocks) {
			renderBlockPreview(blockEntry, breaking, dissolve, firstPos, lastPos);
		}
	}

	protected void renderBlockPreview(BlockEntry blockEntry, boolean breaking, float dissolve, BlockPos firstPos, BlockPos lastPos) {
		if (blockEntry.blockState == null) return;

		var blockPos = blockEntry.blockPos;
		var blockState = blockEntry.blockState;
		if (breaking) {
			blockState = Minecraft.getInstance().level.getBlockState(blockPos);
		}

		float scale = 0.5f;
		float alpha = 0.7f;
		if (dissolve > 0f) {
			float animationLength = 0.8f;

			double firstToSecond = lastPos.distSqr(firstPos);
			double place = 0;
			if (firstToSecond > 0.5) {
				double placeFromFirst = firstPos.distSqr(blockPos) / firstToSecond;
				double placeFromSecond = lastPos.distSqr(blockPos) / firstToSecond;
				place = (placeFromFirst + (1.0 - placeFromSecond)) / 2.0;
			} //else only one block

			//Scale place so we start first animation at 0 and end last animation at 1
			place *= 1f - animationLength;
			float diff = dissolve - (float) place;
			float t = diff / animationLength;
			t = Mth.clamp(t, 0, 1);
			//Now we got a usable t value for this block

			t = (float) Mth.smoothstep(t);
//			t = (float) bezier(t, .58,-0.08,.23,1.33);

			if (!breaking) {
				scale = 0.5f + (t * 0.3f);
				alpha = 0.7f + (t * 0.3f);
			} else {
				t = 1f - t;
				scale = 0.5f + (t * 0.5f);
				alpha = 0.7f + (t * 0.3f);
			}
			alpha = Mth.clamp(alpha, 0, 1);
		}

		CreateClient.GHOST_BLOCKS.showGhostState(blockPos.toShortString(), blockState)
				.at(blockPos)
				.alpha(alpha)
				.scale(scale);
	}

	//A bezier easing function where implicit first and last control points are (0,0) and (1,1).
	public double bezier(double t, double x1, double y1, double x2, double y2) {
		double t2 = t * t;
		double t3 = t2 * t;

		double cx = 3.0 * x1;
		double bx = 3.0 * (x2 - x1) - cx;
		double ax = 1.0 - cx -bx;

		double cy = 3.0 * y1;
		double by = 3.0 * (y2 - y1) - cy;
		double ay = 1.0 - cy - by;

		// Calculate the curve point at parameter value t
		return (ay * t3) + (by * t2) + (cy * t) + 0;
	}

	public void onBlocksPlaced(List<BlockEntry> blocks) {
		if (!ClientConfig.visuals.showBlockPreviews.get()) return;
		if (blocks.size() <= 1 || blocks.size() > ClientConfig.visuals.maxBlockPreviews.get()) return;

		placedBlocksList.add(new PlacedBlocksEntry(ClientEvents.ticksInGame, false, blocks));

		CreateClient.OUTLINER.keep(blocks.get(0).blockPos, CommonConfig.visuals.appearAnimationLength.get());
	}

	public void onBlocksBroken(List<BlockEntry> blocks) {
		if (!ClientConfig.visuals.showBlockPreviews.get()) return;
		if (blocks.size() <= 1 || blocks.size() > ClientConfig.visuals.maxBlockPreviews.get()) return;

		placedBlocksList.add(new PlacedBlocksEntry(ClientEvents.ticksInGame, true, blocks));

		CreateClient.OUTLINER.keep(blocks.get(0).blockPos, CommonConfig.visuals.breakAnimationLength.get());
	}

	private void sortOnDistanceToPlayer(List<BlockPos> coordinates, Player player) {

		Collections.sort(coordinates, (lhs, rhs) -> {
			// -1 - less than, 1 - greater than, 0 - equal
			double lhsDistanceToPlayer = Vec3.atLowerCornerOf(lhs).subtract(player.getEyePosition(1f)).lengthSqr();
			double rhsDistanceToPlayer = Vec3.atLowerCornerOf(rhs).subtract(player.getEyePosition(1f)).lengthSqr();
			return (int) Math.signum(lhsDistanceToPlayer - rhsDistanceToPlayer);
		});

	}

	public static class PlacedBlocksEntry {
		float time;
		boolean breaking;
		List<BlockEntry> blocks;

		public PlacedBlocksEntry(float time, boolean breaking, List<BlockEntry> blocks) {
			this.time = time;
			this.breaking = breaking;
			this.blocks = blocks;
		}
	}
}
