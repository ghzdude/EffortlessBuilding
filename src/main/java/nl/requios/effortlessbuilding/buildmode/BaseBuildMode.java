package nl.requios.effortlessbuilding.buildmode;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

public abstract class BaseBuildMode implements IBuildMode {
	//In singleplayer client and server variables are shared
	//Split everything that needs separate values and may not be called twice in one click
	protected Dictionary<UUID, Integer> rightClickClientTable = new Hashtable<>();
	protected Dictionary<UUID, Integer> rightClickServerTable = new Hashtable<>();
	protected Dictionary<UUID, BlockPos> firstPosTable = new Hashtable<>();
	protected Dictionary<UUID, Direction> sideHitTable = new Hashtable<>();
	protected Dictionary<UUID, Vector3d> hitVecTable = new Hashtable<>();

	@Override
	public void initialize(PlayerEntity player) {
		rightClickClientTable.put(player.getUUID(), 0);
		rightClickServerTable.put(player.getUUID(), 0);
		firstPosTable.put(player.getUUID(), BlockPos.ZERO);
		sideHitTable.put(player.getUUID(), Direction.UP);
		hitVecTable.put(player.getUUID(), Vector3d.ZERO);
	}

	@Override
	public Direction getSideHit(PlayerEntity player) {
		return sideHitTable.get(player.getUUID());
	}

	@Override
	public Vector3d getHitVec(PlayerEntity player) {
		return hitVecTable.get(player.getUUID());
	}
}
