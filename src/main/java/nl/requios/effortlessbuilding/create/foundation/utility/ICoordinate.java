package nl.requios.effortlessbuilding.create.foundation.utility;

import net.minecraft.core.BlockPos;

@FunctionalInterface
public interface ICoordinate {
	float get(BlockPos from);
}
