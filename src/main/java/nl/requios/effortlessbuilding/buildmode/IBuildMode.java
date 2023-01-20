package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import nl.requios.effortlessbuilding.utilities.BlockEntry;

import java.util.List;

public interface IBuildMode {

	//Fired when a player selects a buildmode and when it needs to initializeMode
	void initialize();

	//Fired when a block would be placed
	//Return a list of coordinates where you want to place blocks
	@Deprecated
	List<BlockPos> onRightClick(Player player, BlockPos blockPos, Direction sideHit, Vec3 hitVec, boolean skipRaytrace);

	//Returns if we should place blocks now
	boolean onClick(List<BlockEntry> blocks);

	//Fired continuously for visualization purposes
	@Deprecated
	List<BlockPos> findCoordinates(Player player, BlockPos blockPos, boolean skipRaytrace);

	void findCoordinates(List<BlockEntry> blocks);

	@Deprecated
    Direction getSideHit(Player player);

	@Deprecated
	Vec3 getHitVec(Player player);
}
