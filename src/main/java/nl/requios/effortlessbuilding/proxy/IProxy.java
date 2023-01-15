package nl.requios.effortlessbuilding.proxy;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public interface IProxy {
	Player getPlayerEntityFromContext(Supplier<NetworkEvent.Context> ctx);

	void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar);
}
