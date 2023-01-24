package nl.requios.effortlessbuilding.utilities;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.event.ForgeEventFactory;
import nl.requios.effortlessbuilding.CommonConfig;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.systems.ServerBuildState;

import javax.annotation.Nullable;

public class SurvivalHelper {

	//Used for all placing of blocks in this mod.
	//Checks if area is loaded, if player has the right permissions, if existing block can be replaced (drops it if so) and consumes an item from the stack.
	//Based on ItemBlock#onItemUse
	public static boolean placeBlock(Level world, Player player, BlockPos pos, BlockState blockState,
									 ItemStack origstack, boolean skipPlaceCheck,
									 boolean skipCollisionCheck, boolean playSound) {
		if (!world.isLoaded(pos)) return false;
		ItemStack itemstack = origstack;

		if (blockState.isAir() || itemstack.isEmpty()) {
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
		if (skipPlaceCheck || canPlace(world, player, pos, blockState, itemstack, skipCollisionCheck)) {
			//Drop existing block
			dropBlock(world, player, pos);

			//TryPlace sets block with offset and reduces itemstack count in creative, so we copy only parts of it
//            BlockItemUseContext blockItemUseContext = new BlockItemUseContext(world, player, itemstack, pos, facing, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z);
//            EnumActionResult result = ((ItemBlock) itemstack.getItem()).tryPlace(blockItemUseContext);
			if (!world.setBlock(pos, blockState, 3)) return false;
			BlockItem.updateCustomBlockEntityTag(world, player, pos, itemstack); //Actually BlockItem::onBlockPlaced but that is protected
			block.setPlacedBy(world, pos, blockState, player, itemstack);
			if (player instanceof ServerPlayer) {
				CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, pos, itemstack);
			}

			BlockState afterState = world.getBlockState(pos);

			if (playSound) {
				SoundType soundtype = afterState.getBlock().getSoundType(afterState, world, pos, player);
				world.playSound(null, pos, soundtype.getPlaceSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
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
	public static boolean breakBlock(Level world, Player player, BlockPos pos, boolean skipChecks) {
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
	public static void dropBlock(Level world, Player player, BlockPos pos) {
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
	 * @return Whether the player may place the block at pos with itemstack
	 */
	public static boolean canPlace(Level world, Player player, BlockPos pos, BlockState newBlockState, ItemStack itemStack, boolean skipCollisionCheck) {

		if (!player.isCreative()) {
			//Check if itemstack is correct
			if (itemStack.isEmpty() || !(itemStack.getItem() instanceof BlockItem) ||
				Block.byItem(itemStack.getItem()) != newBlockState.getBlock()) {
				return false;
			}
		}

		Block block = null;
		if (itemStack != null && !itemStack.isEmpty() && itemStack.getItem() instanceof BlockItem)
			block = ((BlockItem) itemStack.getItem()).getBlock();
		else //In creative we might not have an itemstack
			block = newBlockState.getBlock();

		return canPlayerEdit(player, world, pos, itemStack) &&
			mayPlace(world, block, newBlockState, pos, skipCollisionCheck, player) &&
			canReplace(world, player, pos);
	}

	//Can be harvested with hand? (or in creative)
	private static boolean canReplace(Level world, Player player, BlockPos pos) {
		if (player.isCreative()) return true;

		BlockState state = world.getBlockState(pos);

		int miningLevel = CommonConfig.survivalBalancers.quickReplaceMiningLevel.get();
		switch (miningLevel) {
			case -1:
				return !state.requiresCorrectToolForDrops();
			case 0:
				return !state.is(BlockTags.NEEDS_STONE_TOOL) &&
					   !state.is(BlockTags.NEEDS_IRON_TOOL) &&
					   !state.is(BlockTags.NEEDS_DIAMOND_TOOL);
			case 1:
				return !state.is(BlockTags.NEEDS_IRON_TOOL) &&
					   !state.is(BlockTags.NEEDS_DIAMOND_TOOL);
			case 2:
				return !state.is(BlockTags.NEEDS_DIAMOND_TOOL);
			case 3:
			case 4:
				return true;
		}

		return false;
	}

	//From Player#mayUseItemAt
	private static boolean canPlayerEdit(Player player, Level world, BlockPos pos, ItemStack stack) {
		if (!world.mayInteract(player, pos)) return false;

		if (player.getAbilities().mayBuild) {
			//True in creative and survival mode
			return true;
		} else {
			//Adventure mode
			BlockInWorld blockinworld = new BlockInWorld(world, pos, false);
			return stack.hasAdventureModePlaceTagForBlock(world.registryAccess().registryOrThrow(Registry.BLOCK_REGISTRY), blockinworld);
		}
	}

	//From World#mayPlace
	private static boolean mayPlace(Level world, Block blockIn, BlockState newBlockState, BlockPos pos, boolean skipCollisionCheck, @Nullable Entity placer) {
		BlockState currentBlockState = world.getBlockState(pos);
		VoxelShape voxelShape = skipCollisionCheck ? null : blockIn.defaultBlockState().getCollisionShape(world, pos);

		if (voxelShape != null && !world.isUnobstructed(placer, voxelShape)) {
			return false;
		}

		//Check if double slab
		if (placer != null && doesBecomeDoubleSlab(((Player) placer), pos)) {
			return true;
		}

		//Check if same block
		//Necessary otherwise extra items will be dropped
		if (currentBlockState == newBlockState) {
			return false;
		}

		if (currentBlockState.getMaterial() == Material.BUILDABLE_GLASS && blockIn == Blocks.ANVIL) {
			return true;
		}

		//Check quickreplace
		if (placer instanceof Player player) {
			boolean isQuickReplacing = world.isClientSide ? EffortlessBuildingClient.BUILD_SETTINGS.isQuickReplacing()
														  : ServerBuildState.isQuickReplacing(player);
			if (isQuickReplacing) return true;
		}

		return currentBlockState.getMaterial().isReplaceable() /*&& canPlaceBlockOnSide(world, pos, sidePlacedOn)*/;
	}


	//Can break using held tool? (or in creative)
	public static boolean canBreak(Level world, Player player, BlockPos pos) {
		BlockState blockState = world.getBlockState(pos);
		if (!world.getFluidState(pos).isEmpty()) return false;

		if (player.isCreative()) return true;

		return ForgeEventFactory.doPlayerHarvestCheck(player, blockState, true);
	}

	public static boolean doesBecomeDoubleSlab(Player player, BlockPos pos) {
		BlockState placedBlockState = player.level.getBlockState(pos);

		ItemStack itemstack = player.getItemInHand(InteractionHand.MAIN_HAND);
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