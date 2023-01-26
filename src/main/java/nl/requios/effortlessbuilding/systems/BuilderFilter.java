package nl.requios.effortlessbuilding.systems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.compatibility.CompatHelper;
import nl.requios.effortlessbuilding.utilities.BlockSet;
import nl.requios.effortlessbuilding.utilities.PlaceChecker;
import nl.requios.effortlessbuilding.utilities.SurvivalHelper;

@OnlyIn(Dist.CLIENT)
public class BuilderFilter {
    public static void filterOnCoordinates(BlockSet blocks, Player player) {

    }

    public static void filterOnExistingBlockStates(BlockSet blocks, Player player) {
        var buildSettings = EffortlessBuildingClient.BUILD_SETTINGS;
        var buildingState = EffortlessBuildingClient.BUILDER_CHAIN.getPretendBuildingState();
        boolean placing = buildingState == BuilderChain.BuildingState.PLACING;

        var iter = blocks.entrySet().iterator();
        while (iter.hasNext()) {
            var blockEntry = iter.next().getValue();
            var blockState = blockEntry.existingBlockState;
            boolean remove = false;

            if (buildSettings.shouldProtectTileEntities() && blockState.hasBlockEntity()) remove = true;

            if (placing && !buildSettings.shouldReplaceFiltered()) {
                if (!buildSettings.shouldReplaceAir() && blockState.isAir()) remove = true;
                boolean isReplaceable = blockState.getMaterial().isReplaceable();
//                boolean isSolid = blockState.isRedstoneConductor(player.level, blockEntry.blockPos);
                if (!buildSettings.shouldReplaceBlocks() && !isReplaceable) remove = true;
            }

            if (buildSettings.shouldReplaceFiltered()) {
                var offhandItem = player.getOffhandItem();
                if (!CompatHelper.containsBlock(offhandItem, blockState.getBlock())) remove = true;
            }

            if (remove) iter.remove();
        }


        //If the player is going to instabreak grass or a plant, only break other instabreaking things
//        boolean onlyInstaBreaking = !player.isCreative() &&
//                                    world.getBlockState(startCoordinates.get(0)).getDestroySpeed(world, startCoordinates.get(0)) == 0f;
//
//        //break all those blocks
//        for (int i = breakStartPos ? 0 : 1; i < coordinates.size(); i++) {
//            BlockPos coordinate = coordinates.get(i);
//            if (world.isLoaded(coordinate) && !world.isEmptyBlock(coordinate)) {
//                if (!onlyInstaBreaking || world.getBlockState(coordinate).getDestroySpeed(world, coordinate) == 0f) {
//                    SurvivalHelper.breakBlock(world, player, coordinate, false);
//                }
//            }
//        }
    }

    public static void filterOnNewBlockStates(BlockSet blocks, Player player) {
        var buildSettings = EffortlessBuildingClient.BUILD_SETTINGS;
        var buildingState = EffortlessBuildingClient.BUILDER_CHAIN.getPretendBuildingState();
        boolean placing = buildingState == BuilderChain.BuildingState.PLACING;

        var iter = blocks.entrySet().iterator();
        while (iter.hasNext()) {
            var blockEntry = iter.next().getValue();
            boolean remove = false;

            if (placing && !PlaceChecker.shouldPlaceBlock(player.level, blockEntry)) remove = true;

            if (remove) iter.remove();
        }
    }
}
