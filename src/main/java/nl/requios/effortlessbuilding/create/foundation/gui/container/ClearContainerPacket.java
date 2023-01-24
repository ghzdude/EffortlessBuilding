package nl.requios.effortlessbuilding.create.foundation.gui.container;

import nl.requios.effortlessbuilding.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent.Context;

import java.util.function.Supplier;

public class ClearContainerPacket extends SimplePacketBase {

	public ClearContainerPacket() {}

	public ClearContainerPacket(FriendlyByteBuf buffer) {}

	@Override
	public void write(FriendlyByteBuf buffer) {}

	@Override
	public void handle(Supplier<Context> context) {
		context.get()
			.enqueueWork(() -> {
				ServerPlayer player = context.get()
					.getSender();
				if (player == null)
					return;
				if (!(player.containerMenu instanceof IClearableContainer))
					return;
				((IClearableContainer) player.containerMenu).clearContents();
			});
		context.get()
			.setPacketHandled(true);
	}

}
