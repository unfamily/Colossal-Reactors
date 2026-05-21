package net.unfamily.colossal_reactors.turbine;

/**
 * Rod column placement for turbine builder preview/build.
 * Efficient / Productive affect blade ring growth only ({@link #targetBladeRingForLayer}), not which columns get rods.
 */
public final class TurbineRodPatternLogic {

    public static final int PATTERN_EFFICIENT = 0;
    public static final int PATTERN_PRODUCTIVE = 1;

    private TurbineRodPatternLogic() {}

    public static int rodSpaceWidth(int turbineWidth) {
        return TurbineRodSpaceLayout.interiorWidth(turbineWidth);
    }

    public static int rodSpaceHeight(int turbineHeight, int coilLayers) {
        int ih = TurbineRodSpaceLayout.interiorHeight(turbineHeight);
        return TurbineRodSpaceLayout.rodLayerCount(ih, coilLayers);
    }

    public static int rodSpaceDepth(int turbineDepth) {
        return TurbineRodSpaceLayout.interiorDepth(turbineDepth);
    }

    /** Only the geometrically centered rod column gets a rod and blades. */
    public static boolean isRodColumn(int rx, int rz, int rw, int rd, int pattern) {
        if (rw <= 0 || rd <= 0) return false;
        TurbineRodControllerLayout.Center center = TurbineRodControllerLayout.bestPrimaryCenter(rw, rd);
        return rx == center.rx() && rz == center.rz();
    }

    public static boolean isRodForPreview(int rx, int ry, int rz, int rw, int rh, int rd, int pattern) {
        if (ry < 0 || ry >= rh) return false;
        return isRodColumn(rx, rz, rw, rd, pattern);
    }

    /** Target blade ring for layer ry (0 = bottom). Efficient grows rings; Productive uses max. */
    public static int targetBladeRingForLayer(int ry, int rh, int pattern) {
        int maxRing = TurbineBladePlacement.maxRing();
        if (pattern == PATTERN_PRODUCTIVE) {
            return maxRing;
        }
        if (rh <= 1) return 1;
        int step = Math.max(1, maxRing / Math.max(1, rh - 1));
        return Math.min(maxRing, 1 + ry * step);
    }
}
