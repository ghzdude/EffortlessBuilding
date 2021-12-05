package nl.requios.effortlessbuilding.item;

import mcp.MethodsReturnNonnullByDefault;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import nl.requios.effortlessbuilding.BuildConfig;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.ModifierSettingsManager;
import nl.requios.effortlessbuilding.helper.ReachHelper;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class ReachUpgrade3Item extends Item {

	public ReachUpgrade3Item() {
		super(new Item.Properties().tab(ItemGroup.TAB_TOOLS).stacksTo(1));
	}

	@Override
	public ActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
		if (player.isCreative()) {
			if (world.isClientSide) EffortlessBuilding.log(player, "Reach upgrades are not necessary in creative.");
			if (world.isClientSide) EffortlessBuilding.log(player, "Still want increased reach? Use the config.");
			return new ActionResult<>(ActionResultType.PASS, player.getItemInHand(hand));
		}

		ModifierSettingsManager.ModifierSettings modifierSettings = ModifierSettingsManager.getModifierSettings(player);
		int currentLevel = modifierSettings.getReachUpgrade();
		if (currentLevel == 2) {
			modifierSettings.setReachUpgrade(3);
			if (world.isClientSide) EffortlessBuilding.log(player, "Upgraded reach to " + ReachHelper.getMaxReach(player));
			player.setItemInHand(hand, ItemStack.EMPTY);

			SoundEvent soundEvent = new SoundEvent(new ResourceLocation("entity.player.levelup"));
			player.playSound(soundEvent, 1f, 1f);
		} else if (currentLevel < 2) {
			if (currentLevel == 0)
				if (world.isClientSide) EffortlessBuilding.log(player, "Use Reach Upgrade 1 and 2 first.");
			if (currentLevel == 1)
				if (world.isClientSide) EffortlessBuilding.log(player, "Use Reach Upgrade 2 first.");

			SoundEvent soundEvent = new SoundEvent(new ResourceLocation("item.armor.equip_leather"));
			player.playSound(soundEvent, 1f, 1f);
		} else if (currentLevel > 2) {
			if (world.isClientSide)
				EffortlessBuilding.log(player, "Already used this upgrade! Current reach is " + ReachHelper
					.getMaxReach(player) + ".");

			SoundEvent soundEvent = new SoundEvent(new ResourceLocation("item.armor.equip_leather"));
			player.playSound(soundEvent, 1f, 1f);
		}
		return new ActionResult<>(ActionResultType.PASS, player.getItemInHand(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag flag) {
		tooltip.add(new StringTextComponent(TextFormatting.GRAY + "Consume to increase reach to " + TextFormatting.BLUE + BuildConfig.reach.maxReachLevel3.get()));
		tooltip.add(new StringTextComponent(TextFormatting.GRAY + "Previous upgrades need to be consumed first"));
	}

	@Override
	public String getDescriptionId() {
		return this.getRegistryName().toString();
	}
}
