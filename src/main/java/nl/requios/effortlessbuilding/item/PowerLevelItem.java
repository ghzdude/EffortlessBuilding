package nl.requios.effortlessbuilding.item;

import net.minecraft.ChatFormatting;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import java.util.List;

@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
public class PowerLevelItem extends Item {
    public PowerLevelItem() {
        super(new Item.Properties().tab(CreativeModeTab.TAB_TOOLS));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {

        if (world.isClientSide){

            if (EffortlessBuildingClient.POWER_LEVEL.canIncreasePowerLevel()) {

                EffortlessBuildingClient.POWER_LEVEL.increasePowerLevel();
                EffortlessBuilding.log(player, "Upgraded power level to " + EffortlessBuildingClient.POWER_LEVEL.getPowerLevel());
                player.setItemInHand(hand, ItemStack.EMPTY);

                SoundEvent soundEvent = new SoundEvent(new ResourceLocation("entity.player.levelup"));
                player.playSound(soundEvent, 1f, 1f);

                return InteractionResultHolder.consume(player.getItemInHand(hand));

            } else {

                EffortlessBuilding.log(player, "Already reached maximum power level!");

                SoundEvent soundEvent = new SoundEvent(new ResourceLocation("item.armor.equip_leather"));
                player.playSound(soundEvent, 1f, 1f);

                return InteractionResultHolder.fail(player.getItemInHand(hand));
            }

        } else {
            return InteractionResultHolder.consume(player.getItemInHand(hand));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag isAdvanced) {
        tooltip.add(Component.translatable(getDescriptionId() + ".desc").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("key.effortlessbuilding.upgrade_power_level").withStyle(ChatFormatting.BLUE));
    }
}
