package net.unfamily.colossal_reactors.turbine;

import java.util.ArrayList;
import java.util.List;

/**
 * Rod/blade/coil estimates from builder shell sizes. Uses {@link TurbineRotorLayout#rodExtent()}
 * so simulation and material counts match {@link TurbineBuildLogic}.
 */
public final class TurbineBuilderMetrics {

    public record Estimate(
            int rodBlocks,
            int bladeItems,
            int coilBlocks,
            int rodExtent,
            List<Integer> layerBladeCounts
    ) {}

    private TurbineBuilderMetrics() {}

    public static Estimate fromShellSizes(
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int placementAxisIndex,
            int storedCoilSetting,
            int rodPattern,
            int coilIndex) {

        int w = sizeLeft + sizeRight + 1;
        int h = sizeHeight + 1;
        int d = sizeDepth + 1;
        TurbinePlacementAxis placement = TurbinePlacementAxis.fromIndex(placementAxisIndex);
        int interiorAlong = interiorAlongPlacement(w, h, d, placement);
        int appliedCoils = TurbineRodSpaceLayout.appliedCoilLayerCount(interiorAlong, storedCoilSetting);

        TurbineRotorLayout layout = TurbineRotorLayout.from(
                0, 0, 0, w - 1, h - 1, d - 1, w, h, d, appliedCoils, placement.facing());
        int rodExtent = layout.rodExtent();

        int bladeItems = 0;
        List<Integer> layerBladeCounts = new ArrayList<>();
        for (int layer = 0; layer < rodExtent; layer++) {
            int ring = TurbineRodPatternLogic.targetBladeRingForLayer(layer, rodExtent, rodPattern);
            int layerBlades = ring * 4;
            bladeItems += layerBlades;
            layerBladeCounts.add(layerBlades);
        }

        int coilBlocks = 0;
        if (!ElecCoilLoader.shouldSkipSolidCoilAutoPlacement(coilIndex)) {
            coilBlocks = layout.crossSizeA() * layout.crossSizeB() * appliedCoils;
        }

        return new Estimate(rodExtent, bladeItems, coilBlocks, rodExtent, layerBladeCounts);
    }

    public static int balancedBladesForSteam(int bladeItems) {
        if (!Boolean.TRUE.equals(net.unfamily.colossal_reactors.Config.TURBINE_REQUIRE_BALANCED_BLADE_RINGS.get())) {
            return bladeItems;
        }
        return (bladeItems / 4) * 4;
    }

    private static int interiorAlongPlacement(int w, int h, int d, TurbinePlacementAxis placement) {
        return switch (placement.facing().getAxis()) {
            case Y -> TurbineRodSpaceLayout.interiorHeight(h);
            case Z -> TurbineRodSpaceLayout.interiorDepth(d);
            case X -> TurbineRodSpaceLayout.interiorWidth(w);
            default -> TurbineRodSpaceLayout.interiorHeight(h);
        };
    }
}
