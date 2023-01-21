package nl.requios.effortlessbuilding.buildmode.buildmodes;

import nl.requios.effortlessbuilding.buildmode.IBuildMode;
import nl.requios.effortlessbuilding.utilities.BlockEntry;

import java.util.List;

public class Disabled implements IBuildMode {

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
