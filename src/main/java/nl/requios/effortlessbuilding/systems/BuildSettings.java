package nl.requios.effortlessbuilding.systems;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.buildmode.ModeOptions;
import nl.requios.effortlessbuilding.network.IsQuickReplacingPacket;
import nl.requios.effortlessbuilding.network.PacketHandler;

@OnlyIn(Dist.CLIENT)
public class BuildSettings {
    public enum ReplaceMode {
        ONLY_AIR,
        BLOCKS_AND_AIR,
        ONLY_BLOCKS,
        FILTERED_BY_OFFHAND
    }

    private ReplaceMode replaceMode = ReplaceMode.ONLY_AIR;
    private boolean protectTileEntities = true;

    public boolean isQuickReplacing() {
        return replaceMode != ReplaceMode.ONLY_AIR;
    }

    public void setReplaceMode(ReplaceMode replaceMode) {
        this.replaceMode = replaceMode;
        PacketHandler.INSTANCE.sendToServer(new IsQuickReplacingPacket(isQuickReplacing()));
    }

    public ReplaceMode getReplaceMode() {
        return replaceMode;
    }

    public ModeOptions.ActionEnum getReplaceModeActionEnum() {
        return switch (replaceMode) {
            case ONLY_AIR -> ModeOptions.ActionEnum.REPLACE_ONLY_AIR;
            case BLOCKS_AND_AIR -> ModeOptions.ActionEnum.REPLACE_BLOCKS_AND_AIR;
            case ONLY_BLOCKS -> ModeOptions.ActionEnum.REPLACE_ONLY_BLOCKS;
            case FILTERED_BY_OFFHAND -> ModeOptions.ActionEnum.REPLACE_FILTERED_BY_OFFHAND;
        };
    }

    public void toggleProtectTileEntities() {
        protectTileEntities = !protectTileEntities;
    }

    public boolean shouldReplaceAir() {
        return replaceMode == ReplaceMode.ONLY_AIR || replaceMode == ReplaceMode.BLOCKS_AND_AIR;
    }

    public boolean shouldReplaceBlocks() {
        return replaceMode == ReplaceMode.ONLY_BLOCKS || replaceMode == ReplaceMode.BLOCKS_AND_AIR;
    }

    public boolean shouldReplaceFiltered() {
        return replaceMode == ReplaceMode.FILTERED_BY_OFFHAND;
    }

    public boolean shouldProtectTileEntities() {
        return protectTileEntities;
    }

    public boolean shouldOffsetStartPosition() {
        return replaceMode != ReplaceMode.ONLY_AIR;
    }
}
