package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Filters baked rod quads so connector geometry only shows on lateral sides that have blades. */
public final class TurbineRodConnectorQuadFilter {

    private static final float INNER_MIN = 4f / 16f;
    private static final float INNER_MAX = 12f / 16f;
    private static final float EPS = 0.02f;
    private static final int INTS_PER_VERTEX = 8;
    private static final Vector3f CENTROID = new Vector3f();

    private TurbineRodConnectorQuadFilter() {}

    public static List<BakedQuad> filter(List<BakedQuad> quads, Direction rodFacing, int visibleMask) {
        if (visibleMask == 0) {
            return filterAllConnectors(quads, rodFacing);
        }
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (shouldRenderQuad(quad, rodFacing, visibleMask)) {
                out.add(quad);
            }
        }
        return out;
    }

    private static List<BakedQuad> filterAllConnectors(List<BakedQuad> quads, Direction rodFacing) {
        List<BakedQuad> out = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (classifyConnectorSide(quad, rodFacing) == null) {
                out.add(quad);
            }
        }
        return out;
    }

    public static boolean shouldRenderQuad(BakedQuad quad, Direction rodFacing, int visibleMask) {
        Direction side = classifyConnectorSide(quad, rodFacing);
        if (side == null) {
            return true;
        }
        return net.unfamily.colossal_reactors.turbine.TurbineRodConnectorVisibility.isSideVisible(visibleMask, side);
    }

    @Nullable
    public static Direction classifyConnectorSide(BakedQuad quad, Direction rodFacing) {
        quadCentroid(quad, CENTROID);
        return classifyConnectorSide(CENTROID.x, CENTROID.y, CENTROID.z, rodFacing.getAxis());
    }

    @Nullable
    static Direction classifyConnectorSide(float cx, float cy, float cz, Axis rodAxis) {
        return switch (rodAxis) {
            case Y -> classifyOnPlane(cx, cz, cx, cz,
                    Direction.WEST, Direction.EAST, Direction.NORTH, Direction.SOUTH);
            case X -> classifyOnPlane(cy, cz, cy, cz,
                    Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH);
            case Z -> classifyOnPlane(cx, cy, cx, cy,
                    Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP);
        };
    }

    @Nullable
    private static Direction classifyOnPlane(
            float a,
            float b,
            float outA,
            float outB,
            Direction lowA,
            Direction highA,
            Direction lowB,
            Direction highB) {
        float overA = outwardExtent(a);
        float overB = outwardExtent(b);
        if (overA <= EPS && overB <= EPS) {
            return null;
        }
        if (overA >= overB) {
            return outA < 0.5f ? lowA : highA;
        }
        return outB < 0.5f ? lowB : highB;
    }

    private static float outwardExtent(float coord) {
        if (coord < INNER_MIN - EPS) {
            return INNER_MIN - coord;
        }
        if (coord > INNER_MAX + EPS) {
            return coord - INNER_MAX;
        }
        return 0f;
    }

    private static void quadCentroid(BakedQuad quad, Vector3f out) {
        int[] vertices = quad.getVertices();
        float x = 0f;
        float y = 0f;
        float z = 0f;
        for (int v = 0; v < 4; v++) {
            int o = v * INTS_PER_VERTEX;
            x += Float.intBitsToFloat(vertices[o]);
            y += Float.intBitsToFloat(vertices[o + 1]);
            z += Float.intBitsToFloat(vertices[o + 2]);
        }
        out.set(x * 0.25f, y * 0.25f, z * 0.25f);
    }
}
