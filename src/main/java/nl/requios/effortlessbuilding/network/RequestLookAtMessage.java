package nl.requios.effortlessbuilding.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmodifier.BlockSet;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedo;
import nl.requios.effortlessbuilding.proxy.ClientProxy;

import java.util.ArrayList;

/***
 * Sends a message to the client asking for its lookat (objectmouseover) data.
 * This is then sent back with a BlockPlacedMessage.
 */
public class RequestLookAtMessage implements IMessage {
    private boolean placeStartPos;

    public RequestLookAtMessage() {
        placeStartPos = false;
    }

    public RequestLookAtMessage(boolean placeStartPos) {
        this.placeStartPos = placeStartPos;
    }

    public boolean getPlaceStartPos() {
        return placeStartPos;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(this.placeStartPos);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        placeStartPos = buf.readBoolean();
    }

    // The params of the IMessageHandler are <REQ, REPLY>
    public static class MessageHandler implements IMessageHandler<RequestLookAtMessage, IMessage> {
        // Do note that the default constructor is required, but implicitly defined in this case

        @Override
        public IMessage onMessage(RequestLookAtMessage message, MessageContext ctx) {
            //EffortlessBuilding.log("message received on " + ctx.side + " side");

            if (ctx.side == Side.CLIENT){
                //Received clientside
                //Send back your info

//                EffortlessBuilding.proxy.getThreadListenerFromContext(ctx).addScheduledTask(() -> {
//                    EntityPlayer player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);
//
//                });

                //Prevent double placing in normal mode with placeStartPos false.
                //Unless QuickReplace is on, then we do need to place start pos.
                return new BlockPlacedMessage(ClientProxy.previousLookAt, message.getPlaceStartPos());
            }
            return null;
        }
    }
}
