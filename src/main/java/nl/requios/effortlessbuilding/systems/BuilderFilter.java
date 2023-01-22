package nl.requios.effortlessbuilding.systems;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.utilities.BlockEntry;
import nl.requios.effortlessbuilding.utilities.BlockSet;
import nl.requios.effortlessbuilding.utilities.PlaceChecker;

@OnlyIn(Dist.CLIENT)
public class BuilderFilter {
    public static void filterOnCoordinates(BlockSet blocks, Player player) {

    }

    public static void filterOnExistingBlockStates(BlockSet blocks, Player player) {
        var buildSettings = EffortlessBuildingClient.BUILD_SETTINGS;
        boolean placing = EffortlessBuildingClient.BUILDER_CHAIN.getState() == BuilderChain.State.PLACING;

        var iter = blocks.entrySet().iterator();
        while (iter.hasNext()) {
            var blockEntry = iter.next().getValue();
            var blockState = blockEntry.existingBlockState;
            boolean remove = false;

            if (!buildSettings.shouldReplaceTileEntities() && blockState.hasBlockEntity()) remove = true;

            if (placing) {
                if (!buildSettings.shouldReplaceAir() && blockState.isAir()) remove = true;
                boolean isSolid = blockState.isRedstoneConductor(player.level, blockEntry.blockPos);
                if (!buildSettings.shouldReplaceSolid() && isSolid) remove = true;
            }

            if (buildSettings.shouldReplaceFiltered()) {
                var offhandItem = player.getOffhandItem();
                if (!CompatHelper.containsBlock(offhandItem, blockState.getBlock())) remove = true;
            }

            if (remove) iter.remove();
        }
    }

    public static void filterOnNewBlockStates(BlockSet blocks, Player player) {

        var iter = blocks.entrySet().iterator();
        while (iter.hasNext()) {
            var blockEntry = iter.next().getValue();
            boolean remove = false;

            if (!PlaceChecker.shouldPlaceBlock(player.level, blockEntry)) remove = true;

            if (remove) iter.remove();
        }
    }
}
