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
import nl.requios.effortlessbuilding.EffortlessBuildingClient;

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
public class ReachUpgrade2Item extends Item {

	public ReachUpgrade2Item() {
		super(new Item.Properties().stacksTo(1));
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {

		if (!world.isClientSide) return InteractionResultHolder.consume(player.getItemInHand(hand));

		int currentLevel = EffortlessBuildingClient.POWER_LEVEL.getPowerLevel();
		if (currentLevel == 1) {

			EffortlessBuildingClient.POWER_LEVEL.increasePowerLevel();
			EffortlessBuilding.log(player, "Upgraded power level to " + EffortlessBuildingClient.POWER_LEVEL.getPowerLevel());
			player.setItemInHand(hand, ItemStack.EMPTY);

			SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(new ResourceLocation("entity.player.levelup"));
			player.playSound(soundEvent, 1f, 1f);

			return InteractionResultHolder.consume(player.getItemInHand(hand));

		} else if (currentLevel < 1) {

			EffortlessBuilding.log(player, "Use Reach Upgrade 1 first.");

			SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(new ResourceLocation("item.armor.equip_leather"));
			player.playSound(soundEvent, 1f, 1f);


		} else if (currentLevel > 1) {

			EffortlessBuilding.log(player, "Already used this upgrade! Current power level is " + EffortlessBuildingClient.POWER_LEVEL.getPowerLevel() + ".");

			SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(new ResourceLocation("item.armor.equip_leather"));
			player.playSound(soundEvent, 1f, 1f);

		}

		return InteractionResultHolder.fail(player.getItemInHand(hand));
	}

	@Override
	public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag flag) {
		tooltip.add(Component.literal(ChatFormatting.GRAY + "Consume to increase reach to " + ChatFormatting.BLUE + CommonConfig.reach.level2.get()));
		tooltip.add(Component.literal(ChatFormatting.GRAY + "Previous upgrades need to be consumed first"));
	}
}
