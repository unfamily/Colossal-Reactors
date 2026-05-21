package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.unfamily.colossal_reactors.ClientConfig;
import org.jetbrains.annotations.Nullable;

/**
 * Distance checks for turbine rotor BER.
 * Uses the player's render distance (chunks × 16), capped at 64 blocks.
 */
public final class TurbineRotorVisibility {

    private TurbineRotorVisibility() {}

    public static boolean isWithinRenderDistance(AABB bounds) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        double maxDist = ClientConfig.getTurbineRotorRenderDistanceBlocks();
        double maxDistSq = maxDist * maxDist;
        Vec3 center = new Vec3(
                (bounds.minX + bounds.maxX) * 0.5,
                (bounds.minY + bounds.maxY) * 0.5,
                (bounds.minZ + bounds.maxZ) * 0.5);
        return player.position().distanceToSqr(center) <= maxDistSq;
    }

    public static boolean shouldRenderAssembly(@Nullable TurbineRotorGeometry geometry) {
        if (geometry == null || geometry.isEmpty()) {
            return false;
        }
        return isWithinRenderDistance(geometry.bounds());
    }
}
