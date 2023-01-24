package nl.requios.effortlessbuilding.utilities;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import nl.requios.effortlessbuilding.EffortlessBuilding;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

//Common
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

    public void setStartPos(BlockEntry startPos) {
        clear();
        add(startPos);
        firstPos = startPos.blockPos;
        lastPos = startPos.blockPos;
    }

    public void add(BlockEntry blockEntry) {
        if (!containsKey(blockEntry.blockPos)) {
            if (!DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> ClientSide.isFull(this))) {
                put(blockEntry.blockPos, blockEntry);
            }
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

    @OnlyIn(Dist.CLIENT)
    public static class ClientSide {
        public static boolean isFull(BlockSet blockSet) {
            //Limit number of blocks you can place
            int limit = ReachHelper.getMaxBlocksPlacedAtOnce(net.minecraft.client.Minecraft.getInstance().player);
            if (blockSet.size() >= limit) {
                if (logging) EffortlessBuilding.log("BlockSet limit reached, not adding block.");
                return true;
            }
            return false;
        }
    }
}
