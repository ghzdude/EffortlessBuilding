package nl.requios.effortlessbuilding.buildmode;

import nl.requios.effortlessbuilding.utilities.BlockSet;

public interface IBuildMode {

	//Reset values here, start over
	void initialize();

	//Returns if we should place blocks now
	boolean onClick(BlockSet blocks);

	void findCoordinates(BlockSet blocks);
}
