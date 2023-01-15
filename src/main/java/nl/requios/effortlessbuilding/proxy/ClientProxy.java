package nl.requios.effortlessbuilding.proxy;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class ClientProxy implements IProxy {

	public Player getPlayerEntityFromContext(Supplier<NetworkEvent.Context> ctx) {
		return (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT ? Minecraft.getInstance().player : ctx.get().getSender());
	}

	public void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar) {
		EffortlessBuilding.log(Minecraft.getInstance().player, prefix + I18n.get(translationKey) + suffix, actionBar);
	}
}
