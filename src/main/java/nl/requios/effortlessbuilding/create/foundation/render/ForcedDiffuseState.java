package nl.requios.effortlessbuilding.create.foundation.render;

import com.jozufozu.flywheel.util.DiffuseLightCalculator;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import javax.annotation.Nullable;

public final class ForcedDiffuseState {
	private static final ThreadLocal<ObjectArrayList<DiffuseLightCalculator>> FORCED_DIFFUSE = ThreadLocal.withInitial(ObjectArrayList::new);

	private ForcedDiffuseState() {
	}

	public static void pushCalculator(DiffuseLightCalculator calculator) {
		FORCED_DIFFUSE.get().push(calculator);
	}

	public static void popCalculator() {
		FORCED_DIFFUSE.get().pop();
	}

	@Nullable
	public static DiffuseLightCalculator getForcedCalculator() {
		ObjectArrayList<DiffuseLightCalculator> stack = FORCED_DIFFUSE.get();
		if (stack.isEmpty()) {
			return null;
		}
		return stack.top();
	}
}
