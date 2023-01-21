package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.utilities.BlockEntry;

import java.util.List;

public abstract class BaseBuildMode implements IBuildMode {

	protected int clicks;

	@Override
	public void initialize() {
		clicks = 0;
	}

	@Override
	public boolean onClick(List<BlockEntry> blocks) {
		clicks++;
		return false;
	}
}
