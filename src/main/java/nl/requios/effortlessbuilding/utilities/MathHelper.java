package nl.requios.effortlessbuilding.utilities;

import net.minecraft.core.Vec3i;

public class MathHelper {

    public static Vec3i with(Vec3i vec, int index, int value) {
        return switch (index) {
            case 0 -> new Vec3i(value, vec.getY(), vec.getZ());
            case 1 -> new Vec3i(vec.getX(), value, vec.getZ());
            case 2 -> new Vec3i(vec.getX(), vec.getY(), value);
            default -> throw new IllegalArgumentException("Index must be between 0 and 2");
        };
    }

    public static int get(Vec3i vec, int index) {
        return switch (index) {
            case 0 -> vec.getX();
            case 1 -> vec.getY();
            case 2 -> vec.getZ();
            default -> throw new IllegalArgumentException("Index must be between 0 and 2");
        };
    }
}
