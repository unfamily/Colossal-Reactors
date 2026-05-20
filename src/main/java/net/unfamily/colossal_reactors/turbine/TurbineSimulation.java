package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.RegistryAccess;
import net.unfamily.colossal_reactors.Config;
import org.jetbrains.annotations.Nullable;

/**
 * Turbine RF/steam simulation for builder GUI and runtime estimates.
 */
public final class TurbineSimulation {

    public record SimulationResult(
            int rodColumns,
            int bladeCount,
            int coilBlockCount,
            double steamMbPerTick,
            double rfPerTick,
            double coilEfficiency,
            double bladeEfficiency
    ) {}

    private TurbineSimulation() {}

    public static SimulationResult simulateFromBuilderParams(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int rodPattern, int coilIndex, int coilLayerCount) {

        int w = sizeLeft + sizeRight + 1;
        int h = sizeHeight;
        int d = sizeDepth;
        int rw = TurbineRodPatternLogic.rodSpaceWidth(w);
        int rh = TurbineRodPatternLogic.rodSpaceHeight(h, coilLayerCount);
        int rd = TurbineRodPatternLogic.rodSpaceDepth(d);

        int rodColumns = 0;
        int bladeCount = 0;
        for (int rx = 0; rx < rw; rx++) {
            for (int rz = 0; rz < rd; rz++) {
                if (TurbineRodPatternLogic.isRodColumn(rx, rz, rw, rd, rodPattern)) {
                    rodColumns++;
                    for (int ry = 0; ry < rh; ry++) {
                        int ring = TurbineRodPatternLogic.targetBladeRingForLayer(ry, rh, rodPattern);
                        bladeCount += ring * 4;
                    }
                }
            }
        }

        int interiorBlocks = Math.max(1, rw * rh * rd);
        int coilBlocks = Math.max(1, rw * rd * TurbineRodSpaceLayout.coilLayerCount(
                TurbineRodSpaceLayout.interiorHeight(h), coilLayerCount));

        ElecCoilDefinition coilDef = coilIndex >= 0 && coilIndex < ElecCoilLoader.getAllDefinitions().size()
                ? ElecCoilLoader.getAllDefinitions().get(coilIndex)
                : null;
        double coilEff;
        if (coilDef != null) {
            coilEff = Math.min(coilDef.effCoe(), coilDef.effMax());
        } else {
            coilEff = Config.TURBINE_EMPTY_COIL_EFFICIENCY.get();
        }

        java.util.List<Integer> layerCounts = new java.util.ArrayList<>();
        for (int ry = 0; ry < rh; ry++) {
            int ring = TurbineRodPatternLogic.targetBladeRingForLayer(ry, rh, rodPattern);
            layerCounts.add(ring * 4 * Math.max(1, rodColumns));
        }
        double bladeEff = TurbineBladeEfficiency.computeMultiplier(layerCounts);

        int validBlades = bladeCount;
        if (Boolean.TRUE.equals(Config.TURBINE_REQUIRE_BALANCED_BLADE_RINGS.get())) {
            validBlades = (bladeCount / 4) * 4;
        }

        double steam = validBlades * Config.TURBINE_STEAM_MB_PER_BLADE_PER_TICK.get()
                * Config.TURBINE_CONSUMPTION_MULTIPLIER.get();
        TurbineGenerationDefinition gen = TurbineGenerationLoader.getDefault();
        double rfPerMb = gen != null ? gen.rfProduction() : Config.TURBINE_DEFAULT_RF_PER_STEAM_MB.get();
        double rf = steam * rfPerMb * coilEff * bladeEff * Config.TURBINE_PRODUCTION_MULTIPLIER.get();
        rf = Math.max(rf, Config.TURBINE_MIN_RF_PER_TICK.get());

        if (Boolean.TRUE.equals(Config.TURBINE_SIMULATION_DEBUG.get())) {
            net.unfamily.colossal_reactors.ColossalReactors.LOGGER.info(
                    "[TurbineSim] rods={} blades={} steam={} rf={} coilEff={} bladeEff={}",
                    rodColumns, bladeCount, steam, rf, coilEff, bladeEff);
        }

        return new SimulationResult(rodColumns, bladeCount, coilBlocks, steam, rf, coilEff, bladeEff);
    }
}
