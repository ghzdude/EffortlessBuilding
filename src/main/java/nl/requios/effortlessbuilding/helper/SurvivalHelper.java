package nl.requios.effortlessbuilding.helper;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import nl.requios.effortlessbuilding.BuildConfig;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SurvivalHelper {

    //Used for all placing of blocks in this mod.
    //Checks if area is loaded, if player has the right permissions, if existing block can be replaced (drops it if so) and consumes an item from the stack.
    //Based on ItemBlock#onItemUse
    public static boolean placeBlock(World world, EntityPlayer player, BlockPos pos, IBlockState blockState,
                                     ItemStack origstack, EnumFacing facing, Vec3d hitVec, boolean skipPlaceCheck,
                                     boolean skipCollisionCheck, boolean playSound) {
        if (!world.isBlockLoaded(pos, true)) return false;
        ItemStack itemstack = origstack;

        if (blockState.getBlock().isAir(blockState, world, pos) || itemstack.isEmpty()) {
            dropBlock(world, player, pos);
            world.setBlockToAir(pos);
            return true;
        }

        //Randomizer bag, other proxy item synergy
        //Preliminary compatibility code for other items that hold blocks
        if (CompatHelper.isItemBlockProxy(itemstack))
            itemstack = CompatHelper.getItemBlockByState(itemstack, blockState);

        if (!(itemstack.getItem() instanceof ItemBlock))
            return false;
        Block block = ((ItemBlock) itemstack.getItem()).getBlock();


        //More manual with ItemBlock#placeBlockAt
        if (skipPlaceCheck || canPlace(world, player, pos, blockState, itemstack, skipCollisionCheck, facing.getOpposite())) {
            //Drop existing block
            dropBlock(world, player, pos);

            boolean placed = ((ItemBlock) itemstack.getItem()).placeBlockAt(itemstack, player, world, pos, facing, (float) hitVec.x, (float) hitVec.y, (float) hitVec.z, blockState);
            if (!placed) return false;

            IBlockState afterState = world.getBlockState(pos);

            if (playSound) {
                SoundType soundtype = afterState.getBlock().getSoundType(afterState, world, pos, player);
                world.playSound(null, pos, soundtype.getPlaceSound(), SoundCategory.BLOCKS, (soundtype.getVolume() + 1.0F) / 2.0F, soundtype.getPitch() * 0.8F);
            }

            if (!player.isCreative() && Block.getBlockFromItem(itemstack.getItem()) == block) {
                CompatHelper.shrinkStack(origstack, itemstack, player);
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
    public static boolean breakBlock(World world, EntityPlayer player, BlockPos pos, boolean skipChecks) {
        if (!world.isBlockLoaded(pos, false)) return false;

        //Check if can break
        if (skipChecks || canBreak(world, player, pos)) {
//            player.addStat(StatList.getBlockStats(world.getNewBlockState(pos).getBlock()));
//            player.addExhaustion(0.005F);

            //Drop existing block
            dropBlock(world, player, pos);

            //Damage tool
            player.getHeldItemMainhand().onBlockDestroyed(world, world.getBlockState(pos), pos, player);

            world.setBlockToAir(pos);
            return true;
        }
        return false;
    }

    //Gives items directly to player
    public static void dropBlock(World world, EntityPlayer player, BlockPos pos){
        if (player.isCreative()) return;

        IBlockState blockState = world.getBlockState(pos);
        Block block = blockState.getBlock();

        block.harvestBlock(world, player, pos, blockState, world.getTileEntity(pos), player.getHeldItemMainhand());

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
     * @param world
     * @param player
     * @param pos
     * @param newBlockState the blockstate that is going to be placed
     * @param itemStack the itemstack used for placing
     * @param skipCollisionCheck skips collision check with entities
     * @param sidePlacedOn
     * @return Whether the player may place the block at pos with itemstack
     */
    public static boolean canPlace(World world, EntityPlayer player, BlockPos pos, IBlockState newBlockState, ItemStack itemStack, boolean skipCollisionCheck, EnumFacing sidePlacedOn) {

        //Check if itemstack is correct
        if (!(itemStack.getItem() instanceof ItemBlock) || Block.getBlockFromItem(itemStack.getItem()) != newBlockState.getBlock()) {
//            EffortlessBuilding.log(player, "Cannot (re)place block", true);
//            EffortlessBuilding.log("SurvivalHelper#canPlace: itemstack " + itemStack.toString() + " does not match blockstate " + newBlockState.toString());
            //Happens when breaking blocks, no need to notify in that case
            return false;
        }

        Block block = ((ItemBlock) itemStack.getItem()).getBlock();

        return !itemStack.isEmpty() && canPlayerEdit(player, world, pos, itemStack) &&
               mayPlace(world, block, newBlockState, pos, skipCollisionCheck, sidePlacedOn, player) &&
               canReplace(world, player, pos);
    }

    //Can be harvested with hand? (or in creative)
    private static boolean canReplace(World world, EntityPlayer player, BlockPos pos){
        if (player.isCreative()) return true;

        IBlockState state = world.getBlockState(pos);
        state = state.getBlock().getActualState(state, world, pos);

        switch (BuildConfig.survivalBalancers.quickReplaceMiningLevel) {
            case -1: return state.getMaterial().isToolNotRequired();
            case 0: return state.getBlock().getHarvestLevel(state) <= 0;
            case 1: return state.getBlock().getHarvestLevel(state) <= 1;
            case 2: return state.getBlock().getHarvestLevel(state) <= 2;
            case 3: return state.getBlock().getHarvestLevel(state) <= 3;
        }

        return false;
    }

    //From EntityPlayer#canPlayerEdit
    private static boolean canPlayerEdit(EntityPlayer player, World world, BlockPos pos, ItemStack stack)
    {
        if (player.capabilities.allowEdit)
        {
            //True in creative and survival mode
            return true;
        }
        else
        {
            //Adventure mode
            Block block = world.getBlockState(pos).getBlock();
            return stack.canPlaceOn(block) || stack.canEditBlocks();
        }
    }

    //From World#mayPlace
    private static boolean mayPlace(World world, Block blockIn, IBlockState newBlockState, BlockPos pos, boolean skipCollisionCheck, EnumFacing sidePlacedOn, @Nullable Entity placer)
    {
        IBlockState iblockstate1 = world.getBlockState(pos);
        AxisAlignedBB axisalignedbb = skipCollisionCheck ? Block.NULL_AABB : blockIn.getDefaultState().getCollisionBoundingBox(world, pos);

        if (axisalignedbb != Block.NULL_AABB && !world.checkNoEntityCollision(axisalignedbb.offset(pos)))
        {
            return false;
        }

        //Check if double slab
        if (placer != null && doesBecomeDoubleSlab(((EntityPlayer) placer), pos, sidePlacedOn)) {
            return true;
        }

        //Check if same block
        //Necessary otherwise extra items will be dropped
        if (iblockstate1 == newBlockState) {
            return false;
        }

        if (iblockstate1.getMaterial() == Material.CIRCUITS && blockIn == Blocks.ANVIL)
        {
            return true;
        }

        //Check quickreplace
        if (placer instanceof EntityPlayer && ModifierSettingsManager.getModifierSettings(((EntityPlayer) placer)).doQuickReplace()) {
            return true;
        }

        return iblockstate1.getBlock().isReplaceable(world, pos) && blockIn.canPlaceBlockOnSide(world, pos, sidePlacedOn);
    }



    //Can break using held tool? (or in creative)
    public static boolean canBreak(World world, EntityPlayer player, BlockPos pos) {
        IBlockState blockState = world.getBlockState(pos);
        if (blockState.getBlock() instanceof BlockLiquid) return false;

        if (player.isCreative()) return true;

        return canHarvestBlock(blockState.getBlock(), player, world, pos);
    }

    //From ForgeHooks#canHarvestBlock
    public static boolean canHarvestBlock(@Nonnull Block block, @Nonnull EntityPlayer player, @Nonnull IBlockAccess world, @Nonnull BlockPos pos)
    {
        IBlockState state = world.getBlockState(pos);
        state = state.getBlock().getActualState(state, world, pos);

        //Dont break bedrock
        if (state.getBlockHardness((World) world, pos) < 0) {
            return false;
        }

        if (state.getMaterial().isToolNotRequired())
        {
            return true;
        }

        ItemStack stack = player.getHeldItemMainhand();
        String tool = block.getHarvestTool(state);
        if (stack.isEmpty() || tool == null)
        {
            return player.canHarvestBlock(state);
        }

        if (stack.getItemDamage() >= stack.getMaxDamage()) return false;

        int toolLevel = stack.getItem().getHarvestLevel(stack, tool, player, state);
        if (toolLevel < 0)
        {
            return player.canHarvestBlock(state);
        }

        return toolLevel >= block.getHarvestLevel(state);
    }

    public static boolean doesBecomeDoubleSlab(EntityPlayer player, BlockPos pos, EnumFacing facing) {
        IBlockState placedBlockState = player.world.getBlockState(pos);

        ItemStack itemstack = player.getHeldItem(EnumHand.MAIN_HAND);
        if (CompatHelper.isItemBlockProxy(itemstack))
            itemstack = CompatHelper.getItemBlockFromStack(itemstack);

        if (itemstack.isEmpty() || !(itemstack.getItem() instanceof ItemSlab)) return false;
        BlockSlab heldSlab = (BlockSlab) ((ItemSlab) itemstack.getItem()).getBlock();

        if (placedBlockState.getBlock() == heldSlab) {
            IProperty<?> variantProperty = heldSlab.getVariantProperty();
            Comparable<?> placedVariant = placedBlockState.getValue(variantProperty);
            BlockSlab.EnumBlockHalf placedHalf = placedBlockState.getValue(BlockSlab.HALF);

            Comparable<?> heldVariant = heldSlab.getTypeForItem(itemstack);

            if ((facing == EnumFacing.UP && placedHalf == BlockSlab.EnumBlockHalf.BOTTOM || facing == EnumFacing.DOWN && placedHalf == BlockSlab.EnumBlockHalf.TOP) && placedVariant == heldVariant)
            {
                return true;
            }
        }
        return false;
    }
}
