package nl.requios.effortlessbuilding.proxy;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.TranslatedLogPacket;

import java.util.function.Supplier;

public class ServerProxy implements IProxy {
	//Only physical server! Singleplayer server is seen as clientproxy

	public Player getPlayerEntityFromContext(Supplier<NetworkEvent.Context> ctx) {
		return ctx.get().getSender();
	}

	public void logTranslate(Player player, String prefix, String translationKey, String suffix, boolean actionBar) {
		PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new TranslatedLogPacket(prefix, translationKey, suffix, actionBar));
	}
}
