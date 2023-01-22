package nl.requios.effortlessbuilding.systems;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedoBlockSet;
import nl.requios.effortlessbuilding.buildmodifier.UndoRedo;
import nl.requios.effortlessbuilding.utilities.InventoryHelper;
import nl.requios.effortlessbuilding.utilities.SurvivalHelper;

import java.util.*;

public class DelayedBlockPlacer {

    private final Set<Entry> entries = Collections.synchronizedSet(new HashSet<>());
    private final Set<Entry> entriesView = Collections.unmodifiableSet(entries);

    public void placeBlocksDelayed(Entry entry) {
        if (entry.world.isClientSide) return;

        entries.add(entry);
    }

    public void tick() {
        for (Entry entry : entries) {
            entry.ticksTillPlacement--;
            if (entry.ticksTillPlacement <= 0) {
                entry.place();
                entries.remove(entry);
            }
        }
    }

    public Set<Entry> getEntries() {
        return entriesView;
    }

    public static class Entry {
        private Level world;
        private Player player;
        private List<BlockPos> coordinates;
        private List<BlockState> blockStates;
        private List<ItemStack> itemStacks;
        private boolean placeStartPos;
        private int ticksTillPlacement;

        public Entry(Level world, Player player, List<BlockPos> coordinates, List<BlockState> blockStates,
                     List<ItemStack> itemStacks, boolean placeStartPos, int ticksTillPlacement) {
            this.world = world;
            this.player = player;
            this.coordinates = coordinates;
            this.blockStates = blockStates;
            this.itemStacks = itemStacks;
            this.placeStartPos = placeStartPos;
            this.ticksTillPlacement = ticksTillPlacement;
        }

        public void place() {
            //remember previous blockstates for undo
            List<BlockState> previousBlockStates = new ArrayList<>(coordinates.size());
            for (BlockPos coordinate : coordinates) {
                previousBlockStates.add(world.getBlockState(coordinate));
            }

			for (int i = placeStartPos ? 0 : 1; i < coordinates.size(); i++) {
				BlockPos blockPos = coordinates.get(i);
				BlockState blockState = blockStates.get(i);
				ItemStack itemStack = itemStacks.get(i);

				if (world.isLoaded(blockPos)) {
					//check itemstack empty
					if (itemStack.isEmpty()) {
						//try to find new stack, otherwise continue
						itemStack = InventoryHelper.findItemStackInInventory(player, blockState.getBlock());
						if (itemStack.isEmpty()) continue;
					}
					SurvivalHelper.placeBlock(world, player, blockPos, blockState, itemStack, false, false, false);
				}
			}

            //find actual new blockstates for undo
            List<BlockState> newBlockStates = new ArrayList<>(coordinates.size());
            for (BlockPos coordinate : coordinates) {
                newBlockStates.add(world.getBlockState(coordinate));
            }

            //Set first previousBlockState to empty if in NORMAL mode, to make undo/redo work
            //(Block is placed by the time it gets here, and unplaced after this)
            if (!placeStartPos) previousBlockStates.set(0, Blocks.AIR.defaultBlockState());

            //If all new blockstates are air then no use in adding it, no block was actually placed
            //Can happen when e.g. placing one block in yourself
            if (Collections.frequency(newBlockStates, Blocks.AIR.defaultBlockState()) != newBlockStates.size()) {
                //add to undo stack
                BlockPos firstPos = coordinates.get(0);
                BlockPos secondPos = coordinates.get(coordinates.size() - 1);
                UndoRedo.addUndo(player, new UndoRedoBlockSet(coordinates, previousBlockStates, newBlockStates, firstPos, secondPos));
            }
        }

    }
}
