package net.unfamily.colossal_reactors.turbine;

import net.unfamily.colossal_reactors.reactor.RodPatternLogic;

/**
 * Rod column placement for turbine builder preview/build.
 * Efficient uses checkerboard; Productive fills all rod-space columns.
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
        int coils = TurbineRodSpaceLayout.coilLayerCount(ih, coilLayers);
        return Math.max(0, ih - coils);
    }

    public static int rodSpaceDepth(int turbineDepth) {
        return TurbineRodSpaceLayout.interiorDepth(turbineDepth);
    }

    public static boolean isRodColumn(int rx, int rz, int rw, int rd, int pattern) {
        if (rw <= 0 || rd <= 0) return false;
        if (rx < 0 || rx >= rw || rz < 0 || rz >= rd) return false;
        return switch (pattern) {
            case PATTERN_EFFICIENT -> RodPatternLogic.isRodColumn(rx, rz, rw, rd, RodPatternLogic.PATTERN_CHECKERBOARD, false);
            case PATTERN_PRODUCTIVE -> true;
            default -> RodPatternLogic.isRodColumn(rx, rz, rw, rd, RodPatternLogic.PATTERN_CHECKERBOARD, false);
        };
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
