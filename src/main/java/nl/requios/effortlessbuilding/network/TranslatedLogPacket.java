package nl.requios.effortlessbuilding.network;

import net.minecraft.world.entity.player.Player;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;

import java.util.function.Supplier;

/**
 * Send packet to client to translate and log the containing message
 */
public class TranslatedLogPacket {
	private final String prefix;
	private final String translationKey;
	private final String suffix;
	private final boolean actionBar;

	public TranslatedLogPacket() {
		prefix = "";
		translationKey = "";
		suffix = "";
		actionBar = false;
	}

	public TranslatedLogPacket(String prefix, String translationKey, String suffix, boolean actionBar) {
		this.prefix = prefix;
		this.translationKey = translationKey;
		this.suffix = suffix;
		this.actionBar = actionBar;
	}

	public static void encode(TranslatedLogPacket message, FriendlyByteBuf buf) {
		buf.writeUtf(message.prefix);
		buf.writeUtf(message.translationKey);
		buf.writeUtf(message.suffix);
		buf.writeBoolean(message.actionBar);
	}

	public static TranslatedLogPacket decode(FriendlyByteBuf buf) {
		return new TranslatedLogPacket(buf.readUtf(), buf.readUtf(), buf.readUtf(), buf.readBoolean());
	}

	public String getPrefix() {
		return prefix;
	}

	public String getTranslationKey() {
		return translationKey;
	}

	public String getSuffix() {
		return suffix;
	}

	public boolean isActionBar() {
		return actionBar;
	}

	public static class Handler {
		public static void handle(TranslatedLogPacket message, Supplier<NetworkEvent.Context> ctx) {
			ctx.get().enqueueWork(() -> {
				if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
					//Received clientside

					Player player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);
					EffortlessBuilding.logTranslate(player, message.prefix, message.translationKey, message.suffix, message.actionBar);
				}
			});
			ctx.get().setPacketHandled(true);
		}
	}
}
