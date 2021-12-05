package nl.requios.effortlessbuilding.item;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fml.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.capability.ItemHandlerCapabilityProvider;
import nl.requios.effortlessbuilding.helper.SurvivalHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public abstract class AbstractRandomizerBagItem extends Item {

    private static long currentSeed = 1337;
    private static final Random rand = new Random(currentSeed);

    public AbstractRandomizerBagItem() {
        super(new Item.Properties().tab(ItemGroup.TAB_TOOLS).stacksTo(1));
    }

    public abstract int getInventorySize();

    public abstract INamedContainerProvider getContainerProvider(ItemStack item);

    /**
     * Get the inventory of a randomizer bag by checking the capability.
     */
    public IItemHandler getBagInventory(ItemStack bag) {
        return bag.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElse(null);
    }

    /**
     * Pick a random slot from the bag. Empty slots will never get chosen.
     */
    public ItemStack pickRandomStack(IItemHandler bagInventory) {
        //Find how many stacks are non-empty, and save them in a list
        int nonempty = 0;
        List<ItemStack> nonEmptyStacks = new ArrayList<>();
        List<Integer> originalSlots = new ArrayList<>(getInventorySize());
        for (int i = 0; i < bagInventory.getSlots(); i++) {
            ItemStack stack = bagInventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                nonempty++;
                nonEmptyStacks.add(stack);
                originalSlots.add(i);
            }
        }

        if (nonEmptyStacks.size() != originalSlots.size())
            throw new Error("NonEmptyStacks and OriginalSlots not same size");

        if (nonempty == 0) return ItemStack.EMPTY;

        //Pick random slot
        int randomSlot = rand.nextInt(nonempty);
        if (randomSlot < 0 || randomSlot > bagInventory.getSlots()) return ItemStack.EMPTY;

        int originalSlot = originalSlots.get(randomSlot);
        if (originalSlot < 0 || originalSlot > bagInventory.getSlots()) return ItemStack.EMPTY;

        return bagInventory.getStackInSlot(originalSlot);
    }

    public ItemStack findStack(IItemHandler bagInventory, Item item) {
        for (int i = 0; i < bagInventory.getSlots(); i++) {
            ItemStack stack = bagInventory.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == item) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static void resetRandomness() {
        rand.setSeed(currentSeed);
    }

    public static void renewRandomness() {
        currentSeed = Calendar.getInstance().getTimeInMillis();
        rand.setSeed(currentSeed);
    }

    @Override
    public ActionResultType useOn(ItemUseContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        World world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        Direction facing = ctx.getClickedFace();
        ItemStack item = ctx.getItemInHand();
        Vector3d hitVec = ctx.getClickLocation();

        if (player == null) return ActionResultType.FAIL;

        if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) { //ctx.isPlacerSneaking()
            if (world.isClientSide) return ActionResultType.SUCCESS;
            //Open inventory
            NetworkHooks.openGui((ServerPlayerEntity) player, getContainerProvider(item));
        } else {
            if (world.isClientSide) return ActionResultType.SUCCESS;

            //Only place manually if in normal vanilla mode
            BuildModes.BuildModeEnum buildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();
            ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
            if (buildMode != BuildModes.BuildModeEnum.NORMAL || modifierSettings.doQuickReplace()) {
                return ActionResultType.FAIL;
            }

            //Use item
            //Get bag inventory
            //TODO offhand support
            ItemStack bag = player.getItemInHand(Hand.MAIN_HAND);
            IItemHandler bagInventory = getBagInventory(bag);
            if (bagInventory == null)
                return ActionResultType.FAIL;

            ItemStack toPlace = pickRandomStack(bagInventory);
            if (toPlace.isEmpty()) return ActionResultType.FAIL;

            //Previously: use onItemUse to place block (no synergy)
            //bag.setItemDamage(toPlace.getMetadata());
            //toPlace.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);

            //TODO replaceable
            if (!world.getBlockState(pos).getBlock().canBeReplaced(world.getBlockState(pos), Fluids.EMPTY)) {
                pos = pos.relative(facing);
            }

            BlockItemUseContext blockItemUseContext = new BlockItemUseContext(new ItemUseContext(player, Hand.MAIN_HAND, new BlockRayTraceResult(hitVec, facing, pos, false)));
            BlockState blockState = Block.byItem(toPlace.getItem()).getStateForPlacement(blockItemUseContext);

            SurvivalHelper.placeBlock(world, player, pos, blockState, toPlace, facing, hitVec, false, false, true);

            //Synergy
            //Works without calling
//            BlockSnapshot blockSnapshot = new BlockSnapshot(player.world, pos, blockState);
//            BlockEvent.PlaceEvent placeEvent = new BlockEvent.PlaceEvent(blockSnapshot, blockState, player, hand);
//            Mirror.onBlockPlaced(placeEvent);
//            Array.onBlockPlaced(placeEvent);
        }
        return ActionResultType.SUCCESS;
    }

    @Override
    public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack bag = player.getItemInHand(hand);

        if (player.isShiftKeyDown()) {
            if (world.isClientSide) return new ActionResult<>(ActionResultType.SUCCESS, bag);
            //Open inventory
            NetworkHooks.openGui((ServerPlayerEntity) player, getContainerProvider(bag));
        } else {
            //Use item
            //Get bag inventory
            IItemHandler bagInventory = getBagInventory(bag);
            if (bagInventory == null)
                return new ActionResult<>(ActionResultType.FAIL, bag);

            ItemStack toUse = pickRandomStack(bagInventory);
            if (toUse.isEmpty()) return new ActionResult<>(ActionResultType.FAIL, bag);

            return toUse.use(world, player, hand);
        }
        return new ActionResult<>(ActionResultType.PASS, bag);
    }

    @Override
    public int getUseDuration(ItemStack p_77626_1_) {
        return 1;
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundNBT nbt) {
        return new ItemHandlerCapabilityProvider(getInventorySize());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
        tooltip.add(new StringTextComponent(TextFormatting.BLUE + "Rightclick" + TextFormatting.GRAY + " to place a random block"));
        tooltip.add(new StringTextComponent(TextFormatting.BLUE + "Sneak + rightclick" + TextFormatting.GRAY + " to open inventory"));
        if (world != null && world.players().size() > 1) {
            tooltip.add(new StringTextComponent(TextFormatting.YELLOW + "Experimental on servers: may lose inventory"));
        }
    }

    @Override
    public String getDescriptionId() {
        return this.getRegistryName().toString();
    }
}
