package nl.requios.effortlessbuilding.utilities;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class BlockSet extends HashMap<BlockPos, BlockEntry> implements Iterable<BlockEntry> {
    public static boolean logging = true;

    public BlockPos firstPos;
    public BlockPos lastPos;

    public BlockSet() {
        super();
    }

    public BlockSet(BlockSet blockSet) {
        super(blockSet);
        this.firstPos = blockSet.firstPos;
        this.lastPos = blockSet.lastPos;
    }

    public BlockSet(List<BlockEntry> blockEntries) {
        super();
        for (BlockEntry blockEntry : blockEntries) {
            add(blockEntry);
        }
    }

    public void add(BlockEntry blockEntry) {
        if (!containsKey(blockEntry.blockPos)) {

            //Limit number of blocks you can place
            int limit = ReachHelper.getMaxBlocksPlacedAtOnce(Minecraft.getInstance().player);
            if (size() >= limit) {
                if (logging) EffortlessBuilding.log("BlockSet limit reached, not adding block at " + blockEntry.blockPos);
                return;
            }

            put(blockEntry.blockPos, blockEntry);

        } else {

            if (logging) EffortlessBuilding.log("BlockSet already contains block at " + blockEntry.blockPos);
        }
    }

    public HashSet<BlockPos> getCoordinates() {
        return new HashSet<>(keySet());
    }

    public BlockEntry getFirstBlockEntry() {
        return get(firstPos);
    }

    public BlockEntry getLastBlockEntry() {
        return get(lastPos);
    }

    @NotNull
    @Override
    public Iterator<BlockEntry> iterator() {
        return this.values().iterator();
    }

    public static void encode(FriendlyByteBuf buf, BlockSet block) {
        buf.writeCollection(block.values(), BlockEntry::encode);
    }

    public static BlockSet decode(FriendlyByteBuf buf) {
        return new BlockSet(buf.readList(BlockEntry::decode));
    }
}
