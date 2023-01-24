package nl.requios.effortlessbuilding.create.foundation.gui.container;

//import nl.requios.effortlessbuilding.create.foundation.networking.AllPackets;

public interface IClearableContainer {

	default void sendClearPacket() {
//		AllPackets.channel.sendToServer(new ClearContainerPacket());
	}

	@Deprecated //warning: does not work
	public void clearContents();

}
