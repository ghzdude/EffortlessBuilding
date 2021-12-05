package nl.requios.effortlessbuilding.helper;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.CachedBlockInfo;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.common.ToolType;
import nl.requios.effortlessbuilding.BuildConfig;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SurvivalHelper {

	//Used for all placing of blocks in this mod.
	//Checks if area is loaded, if player has the right permissions, if existing block can be replaced (drops it if so) and consumes an item from the stack.
	//Based on ItemBlock#onItemUse
	public static boolean placeBlock(World world, PlayerEntity player, BlockPos pos, BlockState blockState,
									 ItemStack origstack, Direction facing, Vector3d hitVec, boolean skipPlaceCheck,
									 boolean skipCollisionCheck, boolean playSound) {
		if (!world.isLoaded(pos)) return false;
		ItemStack itemstack = origstack;

		if (blockState.getBlock().isAir(blockState, world, pos) || itemstack.isEmpty()) {
			dropBlock(world, player, pos);
			world.removeBlock(pos, false);
			return true;
		}

		//Randomizer bag, other proxy item synergy
		//Preliminary compatibility code for other items that hold blocks
		if (CompatHelper.isItemBlockProxy(itemstack))
			itemstack = CompatHelper.getItemBlockByState(itemstack, blockState);

		if (!(itemstack.getItem() instanceof BlockItem))
			return false;
		Block block = ((BlockItem) itemstack.getItem()).getBlock();


		//More manual with ItemBlock#placeBlockAt
		if (skipPlaceCheck || canPlace(world, player, pos, blockState, itemstack, skipCollisionCheck, facing.getOpposite())) {
			//Drop existing block
			dropBlock(world, player, pos);

			//TryPlace sets block with offset and reduces itemstack count in creative, so we copy only parts of it
//            BlockItemUseContext blockItemUseContext = new BlockItemUseContext(world, player, itemstack, pos, facing, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z);
//            EnumActionResult result = ((ItemBlock) itemstack.getItem()).tryPlace(blockItemUseContext);
			if (!world.setBlock(pos, blockState, 3)) return false;
			BlockItem.updateCustomBlockEntityTag(world, player, pos, itemstack); //Actually BlockItem::onBlockPlaced but that is protected
			block.setPlacedBy(world, pos, blockState, player, itemstack);
			if (player instanceof ServerPlayerEntity) {
				CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) player, pos, itemstack);
			}

			BlockState afterState = world.getBlockState(pos);

			if (playSound) {
				SoundType soundtype = afterState.getBlock().getSoundType(afterState, world, pos, player);
				world.playSound(null, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
			}

			if (!player.isCreative() && Block.byItem(itemstack.getItem()) == block) {
				itemstack.shrink(1);
			}

			return true;
		}
		return false;

		//Using ItemBlock#onItemUse
//        EnumActionResult result;
//        PlayerInteractEvent.RightClickBlock event = ForgeHooks.onRightClickBlock(player, EnumHand.MAIN_HAND, pos, facing, net.minecraftforge.common.ForgeHooks.rayTraceEyeHitVec(player, ReachHelper.getPlacementReach(player)));
//        if (player.isCreative())
//        {
//            int i = itemstack.getMetadata();
//            int j = itemstack.getCount();
//            if (event.getUseItem() != net.minecraftforge.fml.common.eventhandler.Event.Result.DENY) {
//                EnumActionResult enumactionresult = itemstack.getItem().onItemUse(player, world, pos, EnumHand.MAIN_HAND, facing, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z);
//                itemstack.setItemDamage(i);
//                itemstack.setCount(j);
//                return enumactionresult == EnumActionResult.SUCCESS;
//            } else return false;
//        }
//        else
//        {
//            ItemStack copyForUse = itemstack.copy();
//            if (event.getUseItem() != net.minecraftforge.fml.common.eventhandler.Event.Result.DENY)
//                result = itemstack.getItem().onItemUse(player, world, pos, EnumHand.MAIN_HAND, facing, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z);
//            if (itemstack.isEmpty()) net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, copyForUse, EnumHand.MAIN_HAND);
//            return false;
//        }

	}

	//Used for all breaking of blocks in this mod.
	//Checks if area is loaded, if appropriate tool is used in survival mode, and drops the block directly into the players inventory
	public static boolean breakBlock(World world, PlayerEntity player, BlockPos pos, boolean skipChecks) {
		if (!world.isLoaded(pos) && !world.isEmptyBlock(pos)) return false;

		//Check if can break
		if (skipChecks || canBreak(world, player, pos)) {
//            player.addStat(StatList.getBlockStats(world.getNewBlockState(pos).getBlock()));
//            player.addExhaustion(0.005F);

			//Drop existing block
			dropBlock(world, player, pos);

			//Damage tool
			player.getMainHandItem().mineBlock(world, world.getBlockState(pos), pos, player);

			world.removeBlock(pos, false);
			return true;
		}
		return false;
	}

	//Gives items directly to player
	public static void dropBlock(World world, PlayerEntity player, BlockPos pos) {
		if (player.isCreative()) return;

		BlockState blockState = world.getBlockState(pos);
		Block block = blockState.getBlock();

		block.playerDestroy(world, player, pos, blockState, world.getBlockEntity(pos), player.getMainHandItem());

		//TODO drop items in inventory instead of world

//        List<ItemStack> drops = new ArrayList<>();
//
//        //From Block#harvestBlock
//        int silktouch = EnchantmentHelper.getEnchantmentLevel(Enchantments.SILK_TOUCH, player.getHeldItemMainhand());
//        if (block.canSilkHarvest(world, pos, blockState, player) && silktouch > 0) {
//
//            //From Block#getSilkTouchDrop (protected)
//            Item item = Item.getItemFromBlock(block);
//            int i = 0;
//
//            if (item.getHasSubtypes())
//            {
//                i = block.getMetaFromState(blockState);
//            }
//
//            drops.add(new ItemStack(item, 1, i));
//
//            net.minecraftforge.event.ForgeEventFactory.fireBlockHarvesting(drops, world, pos, blockState, 0, 1.0f, true, player);
//        }
//
//        int fortune = EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, player.getHeldItemMainhand());
//        drops.addAll(block.getDrops(world, pos, blockState, fortune));
//        for (ItemStack drop : drops)
//        {
//            ItemHandlerHelper.giveItemToPlayer(player, drop);
//        }
	}

	/**
	 * Check if player can place a block.
	 * Turn randomizer bag into itemstack inside before.
	 *
	 * @param world
	 * @param player
	 * @param pos
	 * @param newBlockState      the blockstate that is going to be placed
	 * @param itemStack          the itemstack used for placing
	 * @param skipCollisionCheck skips collision check with entities
	 * @param sidePlacedOn
	 * @return Whether the player may place the block at pos with itemstack
	 */
	public static boolean canPlace(World world, PlayerEntity player, BlockPos pos, BlockState newBlockState, ItemStack itemStack, boolean skipCollisionCheck, Direction sidePlacedOn) {

		//Check if itemstack is correct
		if (!(itemStack.getItem() instanceof BlockItem) || Block.byItem(itemStack.getItem()) != newBlockState.getBlock()) {
//            EffortlessBuilding.log(player, "Cannot (re)place block", true);
//            EffortlessBuilding.log("SurvivalHelper#canPlace: itemstack " + itemStack.toString() + " does not match blockstate " + newBlockState.toString());
			//Happens when breaking blocks, no need to notify in that case
			return false;
		}

		Block block = ((BlockItem) itemStack.getItem()).getBlock();

		return !itemStack.isEmpty() && canPlayerEdit(player, world, pos, itemStack) &&
			mayPlace(world, block, newBlockState, pos, skipCollisionCheck, sidePlacedOn, player) &&
			canReplace(world, player, pos);
	}

	//Can be harvested with hand? (or in creative)
	private static boolean canReplace(World world, PlayerEntity player, BlockPos pos) {
		if (player.isCreative()) return true;

		BlockState state = world.getBlockState(pos);

		switch (BuildConfig.survivalBalancers.quickReplaceMiningLevel.get()) {
			case -1:
				return !state.requiresCorrectToolForDrops();
			case 0:
				return state.getBlock().getHarvestLevel(state) <= 0;
			case 1:
				return state.getBlock().getHarvestLevel(state) <= 1;
			case 2:
				return state.getBlock().getHarvestLevel(state) <= 2;
			case 3:
				return state.getBlock().getHarvestLevel(state) <= 3;
		}

		return false;
	}

	//From EntityPlayer#canPlayerEdit
	private static boolean canPlayerEdit(PlayerEntity player, World world, BlockPos pos, ItemStack stack) {
		if (!world.mayInteract(player, pos)) return false;

		if (player.abilities.mayBuild) {
			//True in creative and survival mode
			return true;
		} else {
			//Adventure mode
			CachedBlockInfo blockworldstate = new CachedBlockInfo(world, pos, false);
			return stack.hasAdventureModePlaceTagForBlock(world.getTagManager(), blockworldstate);

		}
	}

	//From World#mayPlace
	private static boolean mayPlace(World world, Block blockIn, BlockState newBlockState, BlockPos pos, boolean skipCollisionCheck, Direction sidePlacedOn, @Nullable Entity placer) {
		BlockState iblockstate1 = world.getBlockState(pos);
		VoxelShape voxelShape = skipCollisionCheck ? null : blockIn.defaultBlockState().getCollisionShape(world, pos);

		if (voxelShape != null && !world.isUnobstructed(placer, voxelShape)) {
			return false;
		}

		//Check if double slab
		if (placer != null && doesBecomeDoubleSlab(((PlayerEntity) placer), pos, sidePlacedOn)) {
			return true;
		}

		//Check if same block
		//Necessary otherwise extra items will be dropped
		if (iblockstate1 == newBlockState) {
			return false;
		}

		//TODO 1.14 check what Material.CIRCUITS has become
		if (iblockstate1.getMaterial() == Material.BUILDABLE_GLASS && blockIn == Blocks.ANVIL) {
			return true;
		}

		//Check quickreplace
		if (placer instanceof PlayerEntity && ModifierSettingsManager.getModifierSettings(((PlayerEntity) placer)).doQuickReplace()) {
			return true;
		}

		//TODO 1.13 replaceable
		return iblockstate1.getMaterial().isReplaceable() /*&& canPlaceBlockOnSide(world, pos, sidePlacedOn)*/;
	}


	//Can break using held tool? (or in creative)
	public static boolean canBreak(World world, PlayerEntity player, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		if (!world.getFluidState(pos).isEmpty()) return false;

		if (player.isCreative()) return true;

		return canHarvestBlock(blockState.getBlock(), player, world, pos);
	}

	//From ForgeHooks#canHarvestBlock
	public static boolean canHarvestBlock(@Nonnull Block block, @Nonnull PlayerEntity player, @Nonnull World world, @Nonnull BlockPos pos) {
		BlockState state = world.getBlockState(pos);

		//Dont break bedrock
		if (state.getDestroySpeed(world, pos) < 0) {
			return false;
		}

		if (!state.requiresCorrectToolForDrops()) {
			return true;
		}

		ItemStack stack = player.getMainHandItem();
		ToolType tool = block.getHarvestTool(state);
		if (stack.isEmpty() || tool == null) {
			return player.hasCorrectToolForDrops(state);
		}

		if (stack.getDamageValue() >= stack.getMaxDamage()) return false;

		int toolLevel = stack.getItem().getHarvestLevel(stack, tool, player, state);
		if (toolLevel < 0) {
			return player.hasCorrectToolForDrops(state);
		}

		return toolLevel >= block.getHarvestLevel(state);
	}

	public static boolean doesBecomeDoubleSlab(PlayerEntity player, BlockPos pos, Direction facing) {
		BlockState placedBlockState = player.level.getBlockState(pos);

		ItemStack itemstack = player.getItemInHand(Hand.MAIN_HAND);
		if (CompatHelper.isItemBlockProxy(itemstack))
			itemstack = CompatHelper.getItemBlockFromStack(itemstack);

		if (itemstack.isEmpty() || !(itemstack.getItem() instanceof BlockItem) || !(((BlockItem) itemstack.getItem()).getBlock() instanceof SlabBlock))
			return false;
		SlabBlock heldSlab = (SlabBlock) ((BlockItem) itemstack.getItem()).getBlock();

		if (placedBlockState.getBlock() == heldSlab) {
			//TODO 1.13
//            IProperty<?> variantProperty = heldSlab.getVariantProperty();
//            Comparable<?> placedVariant = placedBlockState.getValue(variantProperty);
//            BlockSlab.EnumBlockHalf placedHalf = placedBlockState.getValue(BlockSlab.HALF);
//
//            Comparable<?> heldVariant = heldSlab.getTypeForItem(itemstack);
//
//            if ((facing == EnumFacing.UP && placedHalf == BlockSlab.EnumBlockHalf.BOTTOM || facing == EnumFacing.DOWN && placedHalf == BlockSlab.EnumBlockHalf.TOP) && placedVariant == heldVariant)
//            {
//                return true;
//            }
		}
		return false;
	}
}
