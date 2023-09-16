package nl.requios.effortlessbuilding.create;

import nl.requios.effortlessbuilding.create.foundation.render.SuperByteBufferCache;
import nl.requios.effortlessbuilding.create.foundation.utility.ghost.GhostBlocks;
import nl.requios.effortlessbuilding.create.foundation.outliner.Outliner;

public class CreateClient {
    public static final SuperByteBufferCache BUFFER_CACHE = new SuperByteBufferCache();
    public static final Outliner OUTLINER = new Outliner();
    public static final GhostBlocks GHOST_BLOCKS = new GhostBlocks();

    public static void invalidateRenderers() {
        CreateClient.BUFFER_CACHE.invalidate();
    }
}
