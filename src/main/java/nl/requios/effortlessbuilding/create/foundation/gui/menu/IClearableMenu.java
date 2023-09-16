package nl.requios.effortlessbuilding.create.foundation.gui.menu;

//import nl.requios.effortlessbuilding.create.AllPackets;

public interface IClearableMenu {

	default void sendClearPacket() {
//		AllPackets.getChannel().sendToServer(new ClearMenuPacket());
	}

	public void clearContents();

}
