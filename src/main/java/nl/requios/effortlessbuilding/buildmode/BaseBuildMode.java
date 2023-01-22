package nl.requios.effortlessbuilding.buildmode;

import nl.requios.effortlessbuilding.utilities.BlockSet;

public abstract class BaseBuildMode implements IBuildMode {

	protected int clicks;

	@Override
	public void initialize() {
		clicks = 0;
	}

	@Override
	public boolean onClick(BlockSet blocks) {
		clicks++;
		return false;
	}
}
