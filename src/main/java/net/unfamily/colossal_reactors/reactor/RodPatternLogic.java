package net.unfamily.colossal_reactors.reactor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes whether a position in the reactor rod space is a rod, based on pattern and mode.
 * Rod space is the interior volume where rods can be placed (inset by 1 for OPTIMIZED/ECONOMY, full for PRODUCTION).
 */
public final class RodPatternLogic {

    /** Cache: for Frame pattern, which variant (rod at center vs heat sink at center) yields more rods per (rw,rh,rd). */
    private static final Map<Long, Boolean> FRAME_ROD_AT_CENTER_CACHE = new ConcurrentHashMap<>();
    private static long frameCacheKey(int rw, int rh, int rd) {
        return (long) rw << 24 | (rh & 0xFFF) << 12 | (rd & 0xFFF);
    }

    public static final int PATTERN_DOTS = 0;
    public static final int PATTERN_CHECKERBOARD = 1;
    public static final int PATTERN_EXPANSION = 2;

    public static final int MODE_OPTIMIZED = 0;
    public static final int MODE_PRODUCTION = 1;
    public static final int MODE_ECONOMY = 2;

    private RodPatternLogic() {}

    /**
     * Rod space dimensions: for OPTIMIZED/ECONOMY use (w-2, h-2, d-2), for PRODUCTION use (w, h, d).
     * Reactor volume in blocks is width w = sizeLeft+sizeRight+1, height h = sizeHeight+1, depth d = sizeDepth+1.
     */
    public static int rodSpaceWidth(int reactorWidth, int patternMode) {
        return patternMode == MODE_PRODUCTION ? reactorWidth : Math.max(0, reactorWidth - 2);
    }

    public static int rodSpaceHeight(int reactorHeight, int patternMode) {
        return patternMode == MODE_PRODUCTION ? reactorHeight : Math.max(0, reactorHeight - 2);
    }

    public static int rodSpaceDepth(int reactorDepth, int patternMode) {
        return patternMode == MODE_PRODUCTION ? reactorDepth : Math.max(0, reactorDepth - 2);
    }

    /**
     * Returns true if the position (rx, ry, rz) in rod space should be a rod.
     * For placement/build: Frame uses the variant (rod at center vs heat sink at center) that yields more rods.
     */
    public static boolean isRod(int rx, int ry, int rz, int rw, int rh, int rd, int pattern) {
        return isRod(rx, ry, rz, rw, rh, rd, pattern, false);
    }

    /**
     * Same as {@link #isRod(int, int, int, int, int, int, int)} with preview flag.
     * When forPreview is true (e.g. preview markers): Frame always uses "rod at center" variant for display.
     * When forPreview is false (actual placement): Frame uses the variant that creates more rods.
     */
    public static boolean isRod(int rx, int ry, int rz, int rw, int rh, int rd, int pattern, boolean forPreview) {
        if (rw <= 0 || rh <= 0 || rd <= 0) return false;
        if (rx < 0 || rx >= rw || ry < 0 || ry >= rh || rz < 0 || rz >= rd) return false;
        return switch (pattern) {
            case PATTERN_DOTS -> isRodDots(rx, ry, rz, rw, rh, rd);
            case PATTERN_CHECKERBOARD -> isRodCheckerboard(rx, ry, rz, rw, rh, rd);
            case PATTERN_EXPANSION -> isRodExpansion(rx, ry, rz, rw, rh, rd, forPreview);
            default -> false;
        };
    }

    /** Dots: rods every 2 blocks in a grid; avoid 2x2 block in center for even dimensions. */
    private static boolean isRodDots(int rx, int ry, int rz, int rw, int rh, int rd) {
        // Place at (even, even, even) in some parity, but avoid 2x2 center when all dims even
        int cx = rw / 2;
        int cy = rh / 2;
        int cz = rd / 2;
        boolean evenW = (rw & 1) == 0;
        boolean evenH = (rh & 1) == 0;
        boolean evenD = (rd & 1) == 0;
        // Standard grid: rod at (2i, 2j, 2k)
        boolean onGrid = (rx % 2 == 0) && (ry % 2 == 0) && (rz % 2 == 0);
        if (!onGrid) return false;
        // If all even, exclude the central 2x2x2 (or 2x2 in the plane) to avoid 2x2 rod block at center
        if (evenW && evenH && evenD) {
            if (rx >= cx - 1 && rx <= cx && ry >= cy - 1 && ry <= cy && rz >= cz - 1 && rz <= cz)
                return false;
        } else if (evenW && evenH) {
            if (rx >= cx - 1 && rx <= cx && ry >= cy - 1 && ry <= cy) return false;
        } else if (evenW && evenD) {
            if (rx >= cx - 1 && rx <= cx && rz >= cz - 1 && rz <= cz) return false;
        } else if (evenH && evenD) {
            if (ry >= cy - 1 && ry <= cy && rz >= cz - 1 && rz <= cz) return false;
        }
        return true;
    }

    /** Checkerboard: alternating so no two rods share a face (no rods touching). */
    private static boolean isRodCheckerboard(int rx, int ry, int rz, int rw, int rh, int rd) {
        // 3D checkerboard: rod at (i+j+k) % 2 == 0 gives no face-adjacent rods
        return (rx + ry + rz) % 2 == 0;
    }

    /**
     * Expansion/Frame: two variants — (A) rod at center, (B) heat sink at center.
     * For placement we use whichever variant yields more rods for this dimension.
     * For preview (forPreview=true) we always show variant A (rod at center).
     * Even dimensions: center can be 2x2 or 2x2x2 (rod block or heat sink block).
     */
    private static boolean isRodExpansion(int rx, int ry, int rz, int rw, int rh, int rd, boolean forPreview) {
        int minDim = Math.min(rw, Math.min(rh, rd));
        if (minDim < 3) return false;
        int layers = expansionLayerCount(rw, rh, rd);
        boolean rodAtCenter = forPreview
                || FRAME_ROD_AT_CENTER_CACHE.computeIfAbsent(frameCacheKey(rw, rh, rd),
                        k -> countExpansionRods(rw, rh, rd, layers, true) >= countExpansionRods(rw, rh, rd, layers, false));
        return isRodExpansionWithLayers(rx, ry, rz, rw, rh, rd, layers, rodAtCenter);
    }

    private static boolean isRodExpansionWithLayers(int rx, int ry, int rz, int rw, int rh, int rd, int layers, boolean rodAtCenter) {
        // L=0: full 6 faces (outer perimeter)
        if (rx == 0 || rx == rw - 1 || ry == 0 || ry == rh - 1 || rz == 0 || rz == rd - 1)
            return true;
        // L>=1: only the 12 edges of the inner box(es), not the face interiors
        for (int L = 1; L < layers; L++) {
            boolean onEdgeX = (rx == L || rx == rw - 1 - L) && (ry == L || ry == rh - 1 - L);
            boolean onEdgeY = (rx == L || rx == rw - 1 - L) && (rz == L || rz == rd - 1 - L);
            boolean onEdgeZ = (ry == L || ry == rh - 1 - L) && (rz == L || rz == rd - 1 - L);
            if (onEdgeX || onEdgeY || onEdgeZ) return true;
        }
        // Center: variant A = rod at center, variant B = heat sink at center (no rods here).
        if (!rodAtCenter) return false;
        int cx = rw / 2, cy = rh / 2, cz = rd / 2;
        boolean oddW = (rw & 1) == 1, oddH = (rh & 1) == 1, oddD = (rd & 1) == 1;
        if (oddW && oddH && oddD)
            return rx == cx && ry == cy && rz == cz;
        // Even: center 2x2 or 2x2x2 block (e.g. rw=4 -> cx=2, center indices 1,2)
        boolean inCenterX = oddW ? (rx == cx) : (rx >= cx - 1 && rx <= cx);
        boolean inCenterY = oddH ? (ry == cy) : (ry >= cy - 1 && ry <= cy);
        boolean inCenterZ = oddD ? (rz == cz) : (rz >= cz - 1 && rz <= cz);
        return inCenterX && inCenterY && inCenterZ;
    }

    /**
     * Choose expansion style (2 or 3 layers) by which places more rods. Called with rod space dimensions.
     */
    public static int expansionLayerCount(int rw, int rh, int rd) {
        int minDim = Math.min(rw, Math.min(rh, rd));
        if (minDim < 5) return 1;
        if (minDim < 7) return 2;
        int count2 = countExpansionRods(rw, rh, rd, 2, true);
        int count3 = countExpansionRods(rw, rh, rd, 3, true);
        return count3 >= count2 ? 3 : 2;
    }

    private static int countExpansionRods(int rw, int rh, int rd, int layers, boolean rodAtCenter) {
        int count = 0;
        for (int rx = 0; rx < rw; rx++) {
            for (int ry = 0; ry < rh; ry++) {
                for (int rz = 0; rz < rd; rz++) {
                    if (isRodExpansionWithLayers(rx, ry, rz, rw, rh, rd, layers, rodAtCenter)) count++;
                }
            }
        }
        return count;
    }
}
