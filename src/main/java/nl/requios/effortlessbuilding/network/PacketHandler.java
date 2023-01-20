package nl.requios.effortlessbuilding.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import nl.requios.effortlessbuilding.EffortlessBuilding;

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

		INSTANCE.registerMessage(id++, ModifierSettingsMessage.class, ModifierSettingsMessage::encode, ModifierSettingsMessage::decode, ModifierSettingsMessage.Handler::handle);
		INSTANCE.registerMessage(id++, ModeSettingsMessage.class, ModeSettingsMessage::encode, ModeSettingsMessage::decode, ModeSettingsMessage.Handler::handle);
		INSTANCE.registerMessage(id++, ModeActionMessage.class, ModeActionMessage::encode, ModeActionMessage::decode, ModeActionMessage.Handler::handle);
		INSTANCE.registerMessage(id++, OnBlockPlacedMessage.class, OnBlockPlacedMessage::encode, OnBlockPlacedMessage::decode, OnBlockPlacedMessage.Handler::handle);
		INSTANCE.registerMessage(id++, AddUndoMessage.class, AddUndoMessage::encode, AddUndoMessage::decode, AddUndoMessage.Handler::handle);
		INSTANCE.registerMessage(id++, ClearUndoMessage.class, ClearUndoMessage::encode, ClearUndoMessage::decode, ClearUndoMessage.Handler::handle);
		INSTANCE.registerMessage(id++, ServerPlaceBlocksMessage.class, ServerPlaceBlocksMessage::encode, ServerPlaceBlocksMessage::decode, ServerPlaceBlocksMessage.Handler::handle);

	}

}
