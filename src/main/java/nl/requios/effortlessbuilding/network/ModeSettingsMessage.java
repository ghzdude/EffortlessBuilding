package nl.requios.effortlessbuilding.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.network.NetworkEvent;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.buildmode.BuildModes;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager;
import nl.requios.effortlessbuilding.buildmode.ModeSettingsManager.ModeSettings;

import java.util.function.Supplier;

/**
 * Shares mode settings (see ModeSettingsManager) between server and client
 */
public class ModeSettingsMessage {

    private ModeSettings modeSettings;

    public ModeSettingsMessage() {
    }

    public ModeSettingsMessage(ModeSettings modeSettings) {
        this.modeSettings = modeSettings;
    }

    public static void encode(ModeSettingsMessage message, PacketBuffer buf) {
        buf.writeInt(message.modeSettings.getBuildMode().ordinal());
    }

    public static ModeSettingsMessage decode(PacketBuffer buf) {
        BuildModes.BuildModeEnum buildMode = BuildModes.BuildModeEnum.values()[buf.readInt()];

        return new ModeSettingsMessage(new ModeSettings(buildMode));
    }

    public static class Handler
    {
        public static void handle(ModeSettingsMessage message, Supplier<NetworkEvent.Context> ctx)
        {
            ctx.get().enqueueWork(() -> {
                EffortlessBuilding.log("ModeSettingsMessage");

                EntityPlayer player = EffortlessBuilding.proxy.getPlayerEntityFromContext(ctx);

                // Sanitize
                ModeSettingsManager.sanitize(message.modeSettings, player);

                ModeSettingsManager.setModeSettings(player, message.modeSettings);
            });
            ctx.get().setPacketHandled(true);
        }
    }
}
