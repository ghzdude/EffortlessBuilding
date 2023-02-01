package nl.requios.effortlessbuilding.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import nl.requios.effortlessbuilding.EffortlessBuilding;

import java.util.Optional;

public class PacketHandler {
	private static final String PROTOCOL_VERSION = "1";
	public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
			new ResourceLocation(EffortlessBuilding.MODID, "main"),
			() -> PROTOCOL_VERSION,
			PROTOCOL_VERSION::equals,
			PROTOCOL_VERSION::equals
			);

	public static void register() {
		int id = 0;

		INSTANCE.registerMessage(id++, IsUsingBuildModePacket.class, IsUsingBuildModePacket::encode, IsUsingBuildModePacket::decode,
				IsUsingBuildModePacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
		INSTANCE.registerMessage(id++, IsQuickReplacingPacket.class, IsQuickReplacingPacket::encode, IsQuickReplacingPacket::decode,
				IsQuickReplacingPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
		INSTANCE.registerMessage(id++, ServerPlaceBlocksPacket.class, ServerPlaceBlocksPacket::encode, ServerPlaceBlocksPacket::decode,
				ServerPlaceBlocksPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
		INSTANCE.registerMessage(id++, ServerBreakBlocksPacket.class, ServerBreakBlocksPacket::encode, ServerBreakBlocksPacket::decode,
				ServerBreakBlocksPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
		INSTANCE.registerMessage(id++, PerformUndoPacket.class, PerformUndoPacket::encode, PerformUndoPacket::decode,
				PerformUndoPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
		INSTANCE.registerMessage(id++, PerformRedoPacket.class, PerformRedoPacket::encode, PerformRedoPacket::decode,
				PerformRedoPacket.Handler::handle, Optional.of(NetworkDirection.PLAY_TO_SERVER));
		INSTANCE.registerMessage(id++, ModifierSettingsPacket.class, ModifierSettingsPacket::encode, ModifierSettingsPacket::decode,
				ModifierSettingsPacket.Handler::handle);
	}

}
