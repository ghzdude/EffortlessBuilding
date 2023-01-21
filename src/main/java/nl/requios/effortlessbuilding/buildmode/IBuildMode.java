package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.utilities.BlockEntry;

import java.util.List;

public interface IBuildMode {

	//Reset values here, start over
	void initialize();

	//Returns if we should place blocks now
	boolean onClick(List<BlockEntry> blocks);

	void findCoordinates(List<BlockEntry> blocks);
}
