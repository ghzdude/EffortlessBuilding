package nl.requios.effortlessbuilding.item;

import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.level.Level;
import nl.requios.effortlessbuilding.CommonConfig;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.utilities.ReachHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReachUpgrade1Item extends Item {

	public ReachUpgrade1Item() {
		super(new Item.Properties().tab(CreativeModeTab.TAB_TOOLS).stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
		if (player.isCreative()) {
			if (world.isClientSide) EffortlessBuilding.log(player, "Reach upgrades are not necessary in creative.");
			if (world.isClientSide) EffortlessBuilding.log(player, "Still want increased reach? Use the config.");
			return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
		}

		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
		int currentLevel = modifierSettings.getReachUpgrade();
		if (currentLevel == 0) {
			modifierSettings.setReachUpgrade(1);

			if (world.isClientSide) EffortlessBuilding.log(player, "Upgraded reach to " + ReachHelper.getMaxReach(player));
			player.setItemInHand(hand, ItemStack.EMPTY);

			SoundEvent soundEvent = new SoundEvent(new ResourceLocation("entity.player.levelup"));
			player.playSound(soundEvent, 1f, 1f);
		} else if (currentLevel > 0) {
			if (world.isClientSide)
				EffortlessBuilding.log(player, "Already used this upgrade! Current reach is " + ReachHelper
					.getMaxReach(player) + ".");

			SoundEvent soundEvent = new SoundEvent(new ResourceLocation("item.armor.equip_leather"));
			player.playSound(soundEvent, 1f, 1f);
		}
		return new InteractionResultHolder<>(InteractionResult.PASS, player.getItemInHand(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(ChatFormatting.GRAY + "Consume to increase reach to " + ChatFormatting.BLUE + CommonConfig.reach.maxReachLevel1.get()));
	}

}
