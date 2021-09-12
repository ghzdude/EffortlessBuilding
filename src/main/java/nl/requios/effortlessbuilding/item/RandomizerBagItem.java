package nl.requios.effortlessbuilding.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.MenuProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.InteractionResult;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.fmllegacy.network.NetworkHooks;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.capability.ItemHandlerCapabilityProvider;
import nl.requios.effortlessbuilding.gui.RandomizerBagContainer;
import nl.requios.effortlessbuilding.helper.SurvivalHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;

@MethodsReturnNonnullByDefault
@ParametersAreNonnullByDefault
public class RandomizerBagItem extends Item {
	public static final int INV_SIZE = 5;

	private static long currentSeed = 1337;
	private static final Random rand = new Random(currentSeed);

	public RandomizerBagItem() {
		super(new Item.Properties().tab(CreativeModeTab.TAB_TOOLS).stacksTo(1));
	}

	/**
	 * Get the inventory of a randomizer bag by checking the capability.
	 *
	 * @param bag
	 * @return
	 */
	public static IItemHandler getBagInventory(ItemStack bag) {
		return bag.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, null).orElse(null);
	}

	/**
	 * Pick a random slot from the bag. Empty slots will never get chosen.
	 *
	 * @param bagInventory
	 * @return
	 */
	public static ItemStack pickRandomStack(IItemHandler bagInventory) {
		//Find how many stacks are non-empty, and save them in a list
		int nonempty = 0;
		List<ItemStack> nonEmptyStacks = new ArrayList<>(INV_SIZE);
		List<Integer> originalSlots = new ArrayList<>(INV_SIZE);
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

	public static ItemStack findStack(IItemHandler bagInventory, Item item) {
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
	public InteractionResult useOn(UseOnContext ctx) {
		Player player = ctx.getPlayer();
		Level world = ctx.getLevel();
		BlockPos pos = ctx.getClickedPos();
		Direction facing = ctx.getClickedFace();
		ItemStack item = ctx.getItemInHand();
		Vec3 hitVec = ctx.getClickLocation();

		if (player == null) return InteractionResult.FAIL;

		if (ctx.getPlayer() != null && ctx.getPlayer().isShiftKeyDown()) { //ctx.isPlacerSneaking()
			if (world.isClientSide) return InteractionResult.SUCCESS;
			//Open inventory
			NetworkHooks.openGui((ServerPlayer) player, new ContainerProvider(item));
		} else {
			if (world.isClientSide) return InteractionResult.SUCCESS;

			//Only place manually if in normal vanilla mode
			BuildModes.BuildModeEnum buildMode = ModeSettingsManager.getModeSettings(player).getBuildMode();
			ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
			if (buildMode != BuildModes.BuildModeEnum.NORMAL || modifierSettings.doQuickReplace()) {
				return InteractionResult.FAIL;
			}

			//Use item
			//Get bag inventory
			//TODO offhand support
			ItemStack bag = player.getItemInHand(InteractionHand.MAIN_HAND);
			IItemHandler bagInventory = getBagInventory(bag);
			if (bagInventory == null)
				return InteractionResult.FAIL;

			ItemStack toPlace = pickRandomStack(bagInventory);
			if (toPlace.isEmpty()) return InteractionResult.FAIL;

			//Previously: use onItemUse to place block (no synergy)
			//bag.setItemDamage(toPlace.getMetadata());
			//toPlace.onItemUse(player, world, pos, hand, facing, hitX, hitY, hitZ);

			//TODO replaceable
			if (!world.getBlockState(pos).getBlock().canBeReplaced(world.getBlockState(pos), Fluids.EMPTY)) {
				pos = pos.relative(facing);
			}

			BlockPlaceContext blockItemUseContext = new BlockPlaceContext(new UseOnContext(player, InteractionHand.MAIN_HAND, new BlockHitResult(hitVec, facing, pos, false)));
			BlockState blockState = Block.byItem(toPlace.getItem()).getStateForPlacement(blockItemUseContext);

			SurvivalHelper.placeBlock(world, player, pos, blockState, toPlace, facing, hitVec, false, false, true);

			//Synergy
			//Works without calling
//            BlockSnapshot blockSnapshot = new BlockSnapshot(player.world, pos, blockState);
//            BlockEvent.PlaceEvent placeEvent = new BlockEvent.PlaceEvent(blockSnapshot, blockState, player, hand);
//            Mirror.onBlockPlaced(placeEvent);
//            Array.onBlockPlaced(placeEvent);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		ItemStack bag = player.getItemInHand(hand);

		if (player.isShiftKeyDown()) {
			if (world.isClientSide) return new InteractionResultHolder<>(InteractionResult.SUCCESS, bag);
			//Open inventory
			NetworkHooks.openGui((ServerPlayer) player, new ContainerProvider(bag));
		} else {
			//Use item
			//Get bag inventory
			IItemHandler bagInventory = getBagInventory(bag);
			if (bagInventory == null)
				return new InteractionResultHolder<>(InteractionResult.FAIL, bag);

			ItemStack toUse = pickRandomStack(bagInventory);
			if (toUse.isEmpty()) return new InteractionResultHolder<>(InteractionResult.FAIL, bag);

			return toUse.use(world, player, hand);
		}
		return new InteractionResultHolder<>(InteractionResult.PASS, bag);
	}

	@Override
	public int getUseDuration(ItemStack p_77626_1_) {
		return 1;
	}

	@Nullable
	@Override
	public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
		return new ItemHandlerCapabilityProvider();
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(new TextComponent(ChatFormatting.BLUE + "Rightclick" + ChatFormatting.GRAY + " to place a random block"));
		tooltip.add(new TextComponent(ChatFormatting.BLUE + "Sneak + rightclick" + ChatFormatting.GRAY + " to open inventory"));
		if (world != null && world.players().size() > 1) {
			tooltip.add(new TextComponent(ChatFormatting.YELLOW + "Experimental on servers: may lose inventory"));
		}
	}

	@Override
	public String getDescriptionId() {
		return this.getRegistryName().toString();
	}

	public static class ContainerProvider implements MenuProvider {

		private final ItemStack bag;

		public ContainerProvider(ItemStack bag) {
			this.bag = bag;
		}

		@Override
		public Component getDisplayName() {
			return new TranslatableComponent("effortlessbuilding.screen.randomizer_bag");
		}

		@Nullable
		@Override
		public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
			return new RandomizerBagContainer(containerId, playerInventory, RandomizerBagItem.getBagInventory(bag));
		}
	}
}
