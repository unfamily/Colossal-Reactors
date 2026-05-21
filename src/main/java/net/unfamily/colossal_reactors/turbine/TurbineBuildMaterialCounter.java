package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.RegistryAccess;

/**
 * Client-safe material estimate for the Turbine Builder.
 */
public final class TurbineBuildMaterialCounter {

    private TurbineBuildMaterialCounter() {}

    public record BuildMaterialCounts(
            int frameCasings,
            int faceCasings,
            int closureDeckCasings,
            int rods,
            int rodControllers,
            int blades,
            int coilBlocks
    ) {
        /** Total shell blocks (casing + glass faces); buffer may supply either as alias. */
        public int frameShellTotal() {
            return frameCasings + faceCasings;
        }
    }

    public static BuildMaterialCounts estimate(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int rodPattern, int coilIndex, int coilLayerCount, boolean openTop) {
        return estimate(registryAccess, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                TurbinePlacementAxis.DEFAULT_INDEX, rodPattern, coilIndex, coilLayerCount, openTop);
    }

    public static BuildMaterialCounts estimate(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int placementAxisIndex,
            int rodPattern, int coilIndex, int coilLayerCount, boolean openTop) {

        int w = sizeLeft + sizeRight + 1;
        int h = sizeHeight + 1;
        int d = sizeDepth + 1;
        TurbinePlacementAxis placement = TurbinePlacementAxis.fromIndex(placementAxisIndex);
        int interiorAlong = switch (placement.facing().getAxis()) {
            case Y -> TurbineRodSpaceLayout.interiorHeight(h);
            case Z -> TurbineRodSpaceLayout.interiorDepth(d);
            case X -> TurbineRodSpaceLayout.interiorWidth(w);
            default -> TurbineRodSpaceLayout.interiorHeight(h);
        };
        int coils = TurbineRodSpaceLayout.appliedCoilLayerCount(interiorAlong, coilLayerCount);
        int rw = TurbineRodPatternLogic.rodSpaceWidth(w);
        int rh = TurbineRodPatternLogic.rodSpaceHeight(h, coils);
        int rd = TurbineRodPatternLogic.rodSpaceDepth(d);

        int closureShell = placement.closureShellCoordLocal(w, h, d, coilLayerCount);
        int frameCasings = 0;
        int faceCasings = 0;
        int maxY = h - 1;
        for (int lx = 0; lx < w; lx++) {
            for (int ly = 0; ly < h; ly++) {
                for (int lz = 0; lz < d; lz++) {
                    if (placement.isOpenEndCapLocal(lx, ly, lz, w, h, d) && openTop) {
                        continue;
                    }
                    boolean onBorder = (lx == 0 || lx == w - 1 || ly == 0 || ly == maxY || lz == 0 || lz == d - 1);
                    if (!onBorder) {
                        continue;
                    }
                    boolean edgeOrCorner = isEdgeOrCorner(lx, ly, lz, 0, 0, 0, w - 1, maxY, d - 1);
                    boolean placementCap = placement.isShellCapLocal(lx, ly, lz, w, h, d);
                    if (edgeOrCorner || placementCap || placement.isClosureShellLocal(lx, ly, lz, closureShell)) {
                        frameCasings++;
                    } else {
                        faceCasings++;
                    }
                }
            }
        }
        int closureDeckCasings = Math.max(0, rw * rd - ((rw > 0 && rd > 0) ? 1 : 0));
        int rodControllers = (rw > 0 && rd > 0) ? 1 : 0;
        int rods = 0;
        int blades = 0;
        for (int rx = 0; rx < rw; rx++) {
            for (int rz = 0; rz < rd; rz++) {
                if (!TurbineRodPatternLogic.isRodColumn(rx, rz, rw, rd, rodPattern)) {
                    continue;
                }
                rods += rh;
                for (int ry = 0; ry < rh; ry++) {
                    blades += TurbineRodPatternLogic.targetBladeRingForLayer(ry, rh, rodPattern) * 4;
                }
            }
        }
        int coilBlocks = 0;
        if (!ElecCoilLoader.shouldSkipSolidCoilAutoPlacement(coilIndex)) {
            coilBlocks = rw * rd * coils;
        }

        return new BuildMaterialCounts(frameCasings, faceCasings, closureDeckCasings, rods, rodControllers, blades, coilBlocks);
    }

    private static boolean isEdgeOrCorner(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int onBoundary = 0;
        if (x == minX || x == maxX) {
            onBoundary++;
        }
        if (y == minY || y == maxY) {
            onBoundary++;
        }
        if (z == minZ || z == maxZ) {
            onBoundary++;
        }
        return onBoundary >= 2;
    }
}
