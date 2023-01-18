package nl.requios.effortlessbuilding.render;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.ClientConfig;
import nl.requios.effortlessbuilding.ClientEvents;
import nl.requios.effortlessbuilding.CommonConfig;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.IBuildMode;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager.ModeSettings;
import nl.requios.effortlessbuilding.buildmodifier.BuildModifiers;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager.ModifierSettings;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.create.AllSpecialTextures;
import nl.requios.effortlessbuilding.create.CreateClient;
import nl.requios.effortlessbuilding.create.foundation.utility.Color;
import nl.requios.effortlessbuilding.utilities.ReachHelper;
import nl.requios.effortlessbuilding.utilities.SurvivalHelper;
import nl.requios.effortlessbuilding.item.AbstractRandomizerBagItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BlockPreviews {
	private static final List<PlacedData> placedDataList = new ArrayList<>();
	private static List<BlockPos> previousCoordinates;
	private static List<BlockState> previousBlockStates;
	private static List<ItemStack> previousItemStacks;
	private static BlockPos previousFirstPos;
	private static BlockPos previousSecondPos;
	private static int soundTime = 0;

	public static void drawPlacedBlocks(Player player, ModifierSettings modifierSettings) {
		//Render placed blocks with appear animation
		if (ClientConfig.visuals.showBlockPreviews.get()) {
			for (PlacedData placed : placedDataList) {
				if (placed.coordinates != null && !placed.coordinates.isEmpty()) {

					int totalTime = placed.breaking ? CommonConfig.visuals.breakAnimationLength.get() : CommonConfig.visuals.appearAnimationLength.get();
					if (totalTime <= 0) continue;

					float dissolve = (ClientEvents.ticksInGame - placed.time) / (float) totalTime;
					renderBlockPreviews(player, modifierSettings, placed.coordinates, placed.blockStates, placed.itemStacks, dissolve, placed.firstPos, placed.secondPos, false, placed.breaking);
				}
			}
		}

		//Expire
		placedDataList.removeIf(placed -> {
			int totalTime = placed.breaking ? CommonConfig.visuals.breakAnimationLength.get() : CommonConfig.visuals.appearAnimationLength.get();
			return placed.time + totalTime < ClientEvents.ticksInGame;
		});
	}

	public static void drawLookAtPreview(Player player, ModeSettings modeSettings, ModifierSettings modifierSettings, BlockPos startPos, Direction sideHit, Vec3 hitVec) {
		if (!doShowBlockPreviews(modifierSettings, modeSettings, startPos)) return;

		//Keep blockstate the same for every block in the buildmode
		//So dont rotate blocks when in the middle of placing wall etc.
		if (BuildModes.isActive(player)) {
			IBuildMode buildModeInstance = modeSettings.getBuildMode().instance;
			if (buildModeInstance.getSideHit(player) != null) sideHit = buildModeInstance.getSideHit(player);
			if (buildModeInstance.getHitVec(player) != null) hitVec = buildModeInstance.getHitVec(player);
		}

		if (sideHit == null) return;

		//Should be red?
		boolean breaking = BuildModes.currentlyBreakingClient.get(player) != null && BuildModes.currentlyBreakingClient.get(player);

		//get coordinates
		List<BlockPos> startCoordinates = BuildModes.findCoordinates(player, startPos, breaking || modifierSettings.doQuickReplace());

		//Remember first and last point for the shader
		BlockPos firstPos = BlockPos.ZERO, secondPos = BlockPos.ZERO;
		if (!startCoordinates.isEmpty()) {
			firstPos = startCoordinates.get(0);
			secondPos = startCoordinates.get(startCoordinates.size() - 1);
		}

		//Limit number of blocks you can place
		int limit = ReachHelper.getMaxBlocksPlacedAtOnce(player);
		if (startCoordinates.size() > limit) {
			startCoordinates = startCoordinates.subList(0, limit);
		}

		List<BlockPos> newCoordinates = BuildModifiers.findCoordinates(player, startCoordinates);

		sortOnDistanceToPlayer(newCoordinates, player);

		hitVec = new Vec3(Math.abs(hitVec.x - ((int) hitVec.x)), Math.abs(hitVec.y - ((int) hitVec.y)),
			Math.abs(hitVec.z - ((int) hitVec.z)));

		//Get blockstates
		List<ItemStack> itemStacks = new ArrayList<>();
		List<BlockState> blockStates = new ArrayList<>();
		if (breaking) {
			//Find blockstate of world
			for (BlockPos coordinate : newCoordinates) {
				blockStates.add(player.level.getBlockState(coordinate));
			}
		} else {
			blockStates = BuildModifiers.findBlockStates(player, startCoordinates, hitVec, sideHit, itemStacks);
		}


		//Check if they are different from previous
		//TODO fix triggering when moving player
		if (!BuildModifiers.compareCoordinates(previousCoordinates, newCoordinates)) {
			previousCoordinates = newCoordinates;
			//remember the rest for placed blocks
			previousBlockStates = blockStates;
			previousItemStacks = itemStacks;
			previousFirstPos = firstPos;
			previousSecondPos = secondPos;

			//if so, renew randomness of randomizer bag
			AbstractRandomizerBagItem.renewRandomness();
			//and play sound (max once every tick)
			if (newCoordinates.size() > 1 && blockStates.size() > 1 && soundTime < ClientEvents.ticksInGame - 0) {
				soundTime = ClientEvents.ticksInGame;

				if (blockStates.get(0) != null) {
					SoundType soundType = blockStates.get(0).getBlock().getSoundType(blockStates.get(0), player.level,
						newCoordinates.get(0), player);
					player.level.playSound(player, player.blockPosition(), breaking ? soundType.getBreakSound() : soundType.getPlaceSound(),
						SoundSource.BLOCKS, 0.3f, 0.8f);
				}
			}
		}

		if (blockStates.size() == 0 || newCoordinates.size() != blockStates.size()) return;

		int blockCount;

		Object outlineID = firstPos;
		//Dont fade out the outline if we are still determining where to place
		//Every outline with same ID will not fade out (because it gets replaced)
		if (newCoordinates.size() == 1 || BuildModifiers.isEnabled(modifierSettings, firstPos)) outlineID = "single";

		if (!breaking) {
			//Use fancy shader if config allows, otherwise outlines
			if (ClientConfig.visuals.showBlockPreviews.get() && newCoordinates.size() < ClientConfig.visuals.maxBlockPreviews.get()) {
				blockCount = renderBlockPreviews(player, modifierSettings, newCoordinates, blockStates, itemStacks, 0f, firstPos, secondPos, !breaking, breaking);

				CreateClient.OUTLINER.showCluster(outlineID, newCoordinates)
						.withFaceTexture(AllSpecialTextures.CHECKERED)
						.disableNormals()
						.lineWidth(1 / 32f)
						.colored(new Color(1f, 1f, 1f, 1f));
			} else {
				//Thicker outline without block previews
				CreateClient.OUTLINER.showCluster(outlineID, newCoordinates)
						.withFaceTexture(AllSpecialTextures.HIGHLIGHT_CHECKERED)
						.disableNormals()
						.lineWidth(1 / 16f)
						.colored(new Color(1f, 1f, 1f, 1f));

				blockCount = newCoordinates.size();
			}

		} else {
			//Breaking
			CreateClient.OUTLINER.showCluster(outlineID, newCoordinates)
					.withFaceTexture(AllSpecialTextures.THIN_CHECKERED)
					.disableNormals()
					.lineWidth(1 / 16f)
					.colored(new Color(0.8f, 0.1f, 0.1f, 1f));
			blockCount = newCoordinates.size();
		}

		//Display block count and dimensions in actionbar
		if (BuildModes.isActive(player)) {

			//Find min and max values (not simply firstPos and secondPos because that doesn't work with circles)
			int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
			int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
			int minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE;
			for (BlockPos pos : startCoordinates) {
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

			EffortlessBuilding.log(player, blockCount + " blocks " + dimensions, true);
		}
	}

	public static void drawOutlinesIfNoBlockInHand(Player player, HitResult lookingAt) {
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

	//Whether to draw any block previews or outlines
	public static boolean doShowBlockPreviews(ModifierSettings modifierSettings, ModeSettings modeSettings, BlockPos startPos) {
		if (!ClientConfig.visuals.showBlockPreviews.get()) return false;
		return modeSettings.getBuildMode() != BuildModes.BuildModeEnum.DISABLED ||
			   (startPos != null && BuildModifiers.isEnabled(modifierSettings, startPos)) ||
			   ClientConfig.visuals.alwaysShowBlockPreview.get();
	}

	protected static int renderBlockPreviews(Player player, ModifierSettings modifierSettings, List<BlockPos> coordinates,
											 List<BlockState> blockStates, List<ItemStack> itemStacks, float dissolve,
											 BlockPos firstPos, BlockPos secondPos, boolean checkCanPlace, boolean red) {
		int blocksValid = 0;

		if (coordinates.isEmpty()) return blocksValid;

		for (int i = coordinates.size() - 1; i >= 0; i--) {
			BlockPos blockPos = coordinates.get(i);
			BlockState blockState = blockStates.get(i);
			ItemStack itemstack = itemStacks.isEmpty() ? ItemStack.EMPTY : itemStacks.get(i);
			if (CompatHelper.isItemBlockProxy(itemstack))
				itemstack = CompatHelper.getItemBlockByState(itemstack, blockState);

			//Check if we can place
			boolean canPlace = true;
			if (checkCanPlace) {
				canPlace = SurvivalHelper.canPlace(player.level, player, blockPos, blockState, itemstack, modifierSettings.doQuickReplace());
			} else {
				//If check is turned off, check if blockstate is the same (for dissolve effect)
				canPlace = player.level.getBlockState(blockPos) != blockState;
			}

			if (canPlace) {
				renderBlockPreview(blockPos, blockState, dissolve, firstPos, secondPos, red);
				blocksValid++;
			}
		}
		return blocksValid;
	}

	protected static void renderBlockPreview(BlockPos blockPos, BlockState blockState, float dissolve, BlockPos firstPos, BlockPos secondPos, boolean breaking) {
		if (blockState == null) return;

		float scale = 0.5f;
		float alpha = 0.7f;
		if (dissolve > 0f) {
			float animationLength = 0.8f;

			double firstToSecond = secondPos.distSqr(firstPos);
			double place = 0;
			if (firstToSecond > 0.5) {
				double placeFromFirst = firstPos.distSqr(blockPos) / firstToSecond;
				double placeFromSecond = secondPos.distSqr(blockPos) / firstToSecond;
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
	public static double bezier(double t, double x1, double y1, double x2, double y2) {
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

	public static void onBlocksPlaced() {
		onBlocksPlaced(previousCoordinates, previousItemStacks, previousBlockStates, previousFirstPos, previousSecondPos);
	}

	public static void onBlocksPlaced(List<BlockPos> coordinates, List<ItemStack> itemStacks, List<BlockState> blockStates,
									  BlockPos firstPos, BlockPos secondPos) {
		LocalPlayer player = Minecraft.getInstance().player;
		ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
		ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);

		//Check if block previews are enabled
		if (doShowBlockPreviews(modifierSettings, modeSettings, firstPos)) {

			//Save current coordinates, blockstates and itemstacks
			if (!coordinates.isEmpty() && blockStates.size() == coordinates.size() &&
				coordinates.size() > 1 && coordinates.size() < ClientConfig.visuals.maxBlockPreviews.get()) {

				placedDataList.add(new PlacedData(ClientEvents.ticksInGame, coordinates, blockStates,
					itemStacks, firstPos, secondPos, false));
			}

			CreateClient.OUTLINER.keep(firstPos, CommonConfig.visuals.appearAnimationLength.get());
		}

	}

	public static void onBlocksBroken() {
		onBlocksBroken(previousCoordinates, previousItemStacks, previousBlockStates, previousFirstPos, previousSecondPos);
	}

	public static void onBlocksBroken(List<BlockPos> coordinates, List<ItemStack> itemStacks, List<BlockState> blockStates,
									  BlockPos firstPos, BlockPos secondPos) {
		LocalPlayer player = Minecraft.getInstance().player;
		ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
		ModeSettings modeSettings = ModeSettingsManager.getModeSettings(player);

		//Check if block previews are enabled
		if (doShowBlockPreviews(modifierSettings, modeSettings, firstPos)) {

			//Save current coordinates, blockstates and itemstacks
			if (!coordinates.isEmpty() && blockStates.size() == coordinates.size() &&
				coordinates.size() > 1 && coordinates.size() < ClientConfig.visuals.maxBlockPreviews.get()) {

				sortOnDistanceToPlayer(coordinates, player);

				placedDataList.add(new PlacedData(ClientEvents.ticksInGame, coordinates, blockStates,
					itemStacks, firstPos, secondPos, true));
			}

			CreateClient.OUTLINER.keep(firstPos, CommonConfig.visuals.breakAnimationLength.get());
		}

	}

	private static void sortOnDistanceToPlayer(List<BlockPos> coordinates, Player player) {

		Collections.sort(coordinates, (lhs, rhs) -> {
			// -1 - less than, 1 - greater than, 0 - equal
			double lhsDistanceToPlayer = Vec3.atLowerCornerOf(lhs).subtract(player.getEyePosition(1f)).lengthSqr();
			double rhsDistanceToPlayer = Vec3.atLowerCornerOf(rhs).subtract(player.getEyePosition(1f)).lengthSqr();
			return (int) Math.signum(lhsDistanceToPlayer - rhsDistanceToPlayer);
		});

	}

	static class PlacedData {
		float time;
		List<BlockPos> coordinates;
		List<BlockState> blockStates;
		List<ItemStack> itemStacks;
		BlockPos firstPos;
		BlockPos secondPos;
		boolean breaking;

		public PlacedData(float time, List<BlockPos> coordinates, List<BlockState> blockStates,
						  List<ItemStack> itemStacks, BlockPos firstPos, BlockPos secondPos, boolean breaking) {
			this.time = time;
			this.coordinates = coordinates;
			this.blockStates = blockStates;
			this.itemStacks = itemStacks;
			this.firstPos = firstPos;
			this.secondPos = secondPos;
			this.breaking = breaking;
		}
	}
}
