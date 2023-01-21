package nl.requios.effortlessbuilding.systems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.network.IsQuickReplacingPacket;
import nl.requios.effortlessbuilding.network.PacketHandler;

@OnlyIn(Dist.CLIENT)
public class QuickReplace {
    private boolean isQuickReplacing = false;

    public boolean isQuickReplacing() {
        return isQuickReplacing;
    }

    public void toggleQuickReplacing() {
        setIsQuickReplacing(!isQuickReplacing);
    }

    public void setIsQuickReplacing(boolean isQuickReplacing) {
        this.isQuickReplacing = isQuickReplacing;

        EffortlessBuilding.log(Minecraft.getInstance().player, "Set " + ChatFormatting.GOLD + "Quick Replace " +
            ChatFormatting.RESET + (this.isQuickReplacing ? "on" : "off"));
        PacketHandler.INSTANCE.sendToServer(new IsQuickReplacingPacket(this.isQuickReplacing));
    }
}
