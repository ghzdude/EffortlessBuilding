package nl.requios.effortlessbuilding.buildmode.buildmodes;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.buildmode.IBuildMode;
import nl.requios.effortlessbuilding.utilities.BlockEntry;

import java.util.ArrayList;
import java.util.List;

public class Single implements IBuildMode {

	@Override
	public void initialize() {

	}

	@Override
	public boolean onClick(List<BlockEntry> blocks) {
		return true;
	}

	@Override
	public void findCoordinates(List<BlockEntry> blocks) {
		//Do nothing
	}
}
