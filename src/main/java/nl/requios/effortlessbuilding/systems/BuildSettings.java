package nl.requios.effortlessbuilding.systems;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.network.IsQuickReplacingPacket;
import nl.requios.effortlessbuilding.network.PacketHandler;

@OnlyIn(Dist.CLIENT)
public class BuildSettings {
    public enum ReplaceMode {
        ONLY_AIR,
        SOLID_AND_AIR,
        SOLID_ONLY,
        FILTERED_BY_OFFHAND
    }

    private boolean quickReplace = false;
    public ReplaceMode replaceMode = ReplaceMode.ONLY_AIR;
    private boolean replaceTileEntities;

    public boolean isQuickReplacing() {
        return quickReplace;
    }

    public void toggleQuickReplace() {
        setQuickReplace(!quickReplace);
    }

    public void setQuickReplace(boolean quickReplace) {
        this.quickReplace = quickReplace;

        EffortlessBuilding.log(Minecraft.getInstance().player, "Set " + ChatFormatting.GOLD + "Quick Replace " +
            ChatFormatting.RESET + (this.quickReplace ? "on" : "off"));
        PacketHandler.INSTANCE.sendToServer(new IsQuickReplacingPacket(this.quickReplace));
    }

    public void setReplaceMode(ReplaceMode replaceMode) {
        this.replaceMode = replaceMode;
    }

    public void setReplaceTileEntities(boolean replaceTileEntities) {
        this.replaceTileEntities = replaceTileEntities;
    }

    public boolean shouldReplaceAir() {
        return replaceMode == ReplaceMode.ONLY_AIR || replaceMode == ReplaceMode.SOLID_AND_AIR;
    }

    public boolean shouldReplaceSolid() {
        return replaceMode == ReplaceMode.SOLID_ONLY || replaceMode == ReplaceMode.SOLID_AND_AIR;
    }

    public boolean shouldReplaceFiltered() {
        return replaceMode == ReplaceMode.FILTERED_BY_OFFHAND;
    }

    public boolean shouldReplaceTileEntities() {
        return replaceTileEntities;
    }

    public boolean shouldOffsetStartPosition() {
        return shouldReplaceSolid() || shouldReplaceFiltered();
    }
}
