package nl.requios.effortlessbuilding.systems;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.PacketDistributor;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import nl.requios.effortlessbuilding.network.PacketHandler;
import nl.requios.effortlessbuilding.network.PowerLevelPacket;

public class ServerPowerLevel {
    public static final int MAX_POWER_LEVEL = 3; //Common access
    private static final String POWER_LEVEL_KEY = EffortlessBuilding.MODID + ":powerLevel";

    public int getPowerLevel(Player player) {
        if (!player.getPersistentData().contains(POWER_LEVEL_KEY)) return 0;
        return player.getPersistentData().getInt(POWER_LEVEL_KEY);
    }

    public void setPowerLevel(Player player, int powerLevel) {
        player.getPersistentData().putInt(POWER_LEVEL_KEY, powerLevel);
    }

    public void sendToClient(Player player) {
        PacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> (ServerPlayer) player), new PowerLevelPacket(getPowerLevel(player)));
    }

    public boolean canBreakFar(Player player) {
        return player.isCreative();
    }

    public boolean canReplaceBlocks(Player player) {
        return player.isCreative();
    }
}
