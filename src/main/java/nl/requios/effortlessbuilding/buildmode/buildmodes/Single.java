package nl.requios.effortlessbuilding.buildmode.buildmodes;

import nl.requios.effortlessbuilding.buildmode.IBuildMode;
import nl.requios.effortlessbuilding.utilities.BlockSet;

public class Single implements IBuildMode {

	@Override
	public void initialize() {

	}

	@Override
	public boolean onClick(BlockSet blocks) {
		return true;
	}

	@Override
	public void findCoordinates(BlockSet blocks) {
		//Do nothing
	}
}
