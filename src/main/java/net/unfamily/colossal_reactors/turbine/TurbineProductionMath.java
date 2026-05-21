package net.unfamily.colossal_reactors.turbine;

import net.unfamily.colossal_reactors.Config;
import org.jetbrains.annotations.Nullable;

/**
 * Shared steam/RF production formulas for validation, builder simulation, and runtime caps.
 */
public final class TurbineProductionMath {

    public record ProductionEstimate(
            int bladeCount,
            int validBladeCount,
            int coilBlockCount,
            double coilEfficiency,
            double bladeEfficiency,
            double maxSteamMbPerTick,
            double estimatedRfPerTick
    ) {}

    private TurbineProductionMath() {}

    public static ProductionEstimate compute(
            int bladeCount,
            int validBladeCount,
            int coilBlockCount,
            double coilEfficiency,
            double bladeEfficiency,
            @Nullable TurbineGenerationDefinition generation) {
        double steam = validBladeCount * Config.TURBINE_STEAM_MB_PER_BLADE_PER_TICK.get()
                * Config.TURBINE_CONSUMPTION_MULTIPLIER.get();
        double rfPerMb = generation != null
                ? TurbineGenerationLoader.rfPerSteamMb(generation.rfProduction())
                : TurbineGenerationLoader.rfPerSteamMb(Config.TURBINE_DEFAULT_RF_PER_STEAM_BUCKET.get());
        double rf = steam * rfPerMb * coilEfficiency * bladeEfficiency * Config.TURBINE_PRODUCTION_MULTIPLIER.get();
        rf = Math.max(rf, Config.TURBINE_MIN_RF_PER_TICK.get());
        return new ProductionEstimate(
                bladeCount,
                validBladeCount,
                coilBlockCount,
                coilEfficiency,
                bladeEfficiency,
                steam,
                rf);
    }

    /** Ideal multiblock from builder menu parameters (matches a correctly built turbine). */
    public static ProductionEstimate fromBuilderParams(
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int placementAxisIndex,
            int rodPattern,
            int coilIndex,
            int storedCoilSetting,
            @Nullable TurbineGenerationDefinition generation) {
        TurbineBuilderMetrics.IdealBuildMetrics ideal = TurbineBuilderMetrics.idealBuildMetrics(
                sizeLeft, sizeRight, sizeHeight, sizeDepth,
                placementAxisIndex, storedCoilSetting, rodPattern, coilIndex);
        TurbineGenerationDefinition gen = generation != null ? generation : TurbineGenerationLoader.getDefault();
        return compute(
                ideal.bladeCount(),
                ideal.validBladeCount(),
                ideal.coilBlockCount(),
                ideal.coilEfficiency(),
                ideal.bladeEfficiency(),
                gen);
    }
}
