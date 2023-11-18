package nl.requios.effortlessbuilding.create.foundation.gui.container;

public interface IClearableMenu {

	default void sendClearPacket() {
//		PacketHandler.INSTANCE.sendToServer(new ClearMenuPacket());
	}

	public void clearContents();

}
