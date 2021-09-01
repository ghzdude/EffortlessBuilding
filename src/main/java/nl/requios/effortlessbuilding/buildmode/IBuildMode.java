package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.world.entity.player.Player;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public interface IBuildMode {

	//Fired when a player selects a buildmode and when it needs to initializeMode
	void initialize(Player player);

	//Fired when a block would be placed
	//Return a list of coordinates where you want to place blocks
	List<BlockPos> onRightClick(Player player, BlockPos blockPos, Direction sideHit, Vec3 hitVec, boolean skipRaytrace);

	//Fired continuously for visualization purposes
	List<BlockPos> findCoordinates(Player player, BlockPos blockPos, boolean skipRaytrace);

	Direction getSideHit(Player player);

	Vec3 getHitVec(Player player);
}
