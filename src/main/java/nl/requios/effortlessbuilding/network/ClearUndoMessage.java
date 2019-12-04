package nl.requios.effortlessbuilding.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedo;

import java.util.function.Supplier;

/***
 * Sends a message to the client asking to clear the undo and redo stacks.
 */
public class ClearUndoMessage {

    public ClearUndoMessage() {
    }

    public static void encode(ClearUndoMessage message, PacketBuffer buf) {

    }

    public static ClearUndoMessage decode(PacketBuffer buf) {
        return new ClearUndoMessage();
    }

    public static class Handler
    {
        public static void handle(ClearUndoMessage message, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                EffortlessBuilding.log("ClearUndoMessage");

                if (ctx.get().getDirection().getReceptionSide() == LogicalSide.CLIENT) {
                    //Received clientside
                    EntityPlayer player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);

                    //Add to undo stack clientside
                    UndoRedo.clear(player);
                }
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
