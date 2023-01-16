package nl.requios.effortlessbuilding.create.foundation.render;

import com.jozufozu.flywheel.core.PartialModel;
import com.jozufozu.flywheel.util.transform.TransformStack;
import com.mojang.blaze3d.vertex.PoseStack;
import nl.requios.effortlessbuilding.EffortlessBuildingClient;
import nl.requios.effortlessbuilding.create.CreateClient;
import nl.requios.effortlessbuilding.create.foundation.render.SuperByteBufferCache.Compartment;
import nl.requios.effortlessbuilding.create.foundation.utility.AngleHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.commons.lang3.tuple.Pair;

import java.util.function.Supplier;

import static net.minecraft.world.level.block.state.properties.BlockStateProperties.FACING;

public class CachedBufferer {

	public static final Compartment<BlockState> GENERIC_TILE = new Compartment<>();
	public static final Compartment<PartialModel> PARTIAL = new Compartment<>();
	public static final Compartment<Pair<Direction, PartialModel>> DIRECTIONAL_PARTIAL = new Compartment<>();

	public static SuperByteBuffer block(BlockState toRender) {
		return block(GENERIC_TILE, toRender);
	}

	public static SuperByteBuffer block(Compartment<BlockState> compartment, BlockState toRender) {
		return CreateClient.BUFFER_CACHE.get(compartment, toRender, () -> BakedModelRenderHelper.standardBlockRender(toRender));
	}

	public static SuperByteBuffer partial(PartialModel partial, BlockState referenceState) {
		return CreateClient.BUFFER_CACHE.get(PARTIAL, partial,
				() -> BakedModelRenderHelper.standardModelRender(partial.get(), referenceState));
	}

	public static SuperByteBuffer partial(PartialModel partial, BlockState referenceState,
			Supplier<PoseStack> modelTransform) {
		return CreateClient.BUFFER_CACHE.get(PARTIAL, partial,
				() -> BakedModelRenderHelper.standardModelRender(partial.get(), referenceState, modelTransform.get()));
	}

	public static SuperByteBuffer partialFacing(PartialModel partial, BlockState referenceState) {
		Direction facing = referenceState.getValue(FACING);
		return partialFacing(partial, referenceState, facing);
	}

	public static SuperByteBuffer partialFacing(PartialModel partial, BlockState referenceState, Direction facing) {
		return partialDirectional(partial, referenceState, facing,
			rotateToFace(facing));
	}

	public static SuperByteBuffer partialFacingVertical(PartialModel partial, BlockState referenceState, Direction facing) {
		return partialDirectional(partial, referenceState, facing,
			rotateToFaceVertical(facing));
	}

	public static SuperByteBuffer partialDirectional(PartialModel partial, BlockState referenceState, Direction dir,
			Supplier<PoseStack> modelTransform) {
		return CreateClient.BUFFER_CACHE.get(DIRECTIONAL_PARTIAL, Pair.of(dir, partial),
			() -> BakedModelRenderHelper.standardModelRender(partial.get(), referenceState, modelTransform.get()));
	}

	public static Supplier<PoseStack> rotateToFace(Direction facing) {
		return () -> {
			PoseStack stack = new PoseStack();
			TransformStack.cast(stack)
				.centre()
				.rotateY(AngleHelper.horizontalAngle(facing))
				.rotateX(AngleHelper.verticalAngle(facing))
				.unCentre();
			return stack;
		};
	}

	public static Supplier<PoseStack> rotateToFaceVertical(Direction facing) {
		return () -> {
			PoseStack stack = new PoseStack();
			TransformStack.cast(stack)
				.centre()
				.rotateY(AngleHelper.horizontalAngle(facing))
				.rotateX(AngleHelper.verticalAngle(facing) + 90)
				.unCentre();
			return stack;
		};
	}

}