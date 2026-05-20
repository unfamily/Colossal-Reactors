package net.unfamily.colossal_reactors.turbine;

import net.unfamily.colossal_reactors.Config;

/**
 * Interior layout for turbine builder: rod space below, coil layers at top of interior.
 */
public final class TurbineRodSpaceLayout {

    private static final int FRAME_INSET = 1;

    private TurbineRodSpaceLayout() {}

    public static int interiorWidth(int turbineWidth) {
        return Math.max(0, turbineWidth - 2 * FRAME_INSET);
    }

    public static int interiorHeight(int turbineHeight) {
        return Math.max(0, turbineHeight - 2 * FRAME_INSET);
    }

    public static int interiorDepth(int turbineDepth) {
        return Math.max(0, turbineDepth - 2 * FRAME_INSET);
    }

    public static int defaultCoilLayerCount() {
        return Config.TURBINE_DEFAULT_COIL_LAYER_COUNT.get();
    }

    /** Maximum coil layers that fit (at least one interior layer remains for rods). */
    public static int maxCoilLayersForInterior(int interiorAlong) {
        if (interiorAlong <= 1) {
            return 1;
        }
        return interiorAlong - 1;
    }

    /** Y layers in interior reserved for elec coils (top of interior). */
    public static int coilLayerCount(int interiorHeight, int requestedCoilLayers) {
        int def = defaultCoilLayerCount();
        int layers = requestedCoilLayers > 0 ? requestedCoilLayers : def;
        return Math.min(Math.max(1, layers), maxCoilLayersForInterior(interiorHeight));
    }

    /** First interior Y index (0-based) for coil zone. */
    public static int coilZoneStartY(int interiorHeight, int coilLayers) {
        return Math.max(0, interiorHeight - coilLayerCount(interiorHeight, coilLayers));
    }

    /** True if interior Y is in coil zone. */
    public static boolean isCoilLayerY(int interiorY, int interiorHeight, int coilLayers) {
        return interiorY >= coilZoneStartY(interiorHeight, coilLayers);
    }

    /**
     * Extra inset inside the interior for rod-space indices. Zero: {@link #interiorWidth} already excludes the shell.
     */
    public static int rodSpaceInset() {
        return 0;
    }
}
