package nl.requios.effortlessbuilding.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;

import java.util.function.Supplier;

/**
 * Sync build modifiers between server and client, for saving and loading.
 */
public class ModifierSettingsPacket {

	private static final String DATA_KEY = EffortlessBuilding.MODID + ":buildModifiers";
	private CompoundTag modifiersTag;

	public ModifierSettingsPacket() {
	}

	public ModifierSettingsPacket(CompoundTag modifiersTag) {
		this.modifiersTag = modifiersTag;
	}

	//Only on server
	public ModifierSettingsPacket(Player player) {
		this.modifiersTag = player.getPersistentData().getCompound(DATA_KEY);
	}

	public static void encode(ModifierSettingsPacket message, FriendlyByteBuf buf) {
		buf.writeNbt(message.modifiersTag);
	}

	public static ModifierSettingsPacket decode(FriendlyByteBuf buf) {
		return new ModifierSettingsPacket(buf.readNbt());
	}

	public static class Handler {
		public static void handle(ModifierSettingsPacket message, Supplier<NetworkEvent.Context> ctx) {
			if (ctx.get().getDirection().getReceptionSide().isServer()) {
				ctx.get().enqueueWork(() -> {
					var player = ctx.get().getSender();
					//To server, save to persistent player data
					player.getPersistentData().put(DATA_KEY, message.modifiersTag);
				});
			} else {
				ctx.get().enqueueWork(() -> {
					//To client, load into system
					EffortlessBuildingClient.BUILD_MODIFIERS.deserializeNBT(message.modifiersTag);
				});
			}
			ctx.get().setPacketHandled(true);
		}
	}
}
