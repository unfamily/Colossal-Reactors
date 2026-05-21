package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.TurbineControllerBlock;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbinePowerPort;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Turbine RF/steam simulation for builder GUI and runtime estimates.
 */
public final class TurbineSimulation {

    public record SimulationResult(
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
        return simulateFromBuilderParams(registryAccess, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                rodPattern, coilIndex, coilLayerCount, null);
    }

    public static SimulationResult simulateFromBuilderParams(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int rodPattern, int coilIndex, int coilLayerCount,
            @Nullable ResourceLocation generationId) {

        int w = sizeLeft + sizeRight + 1;
        int h = sizeHeight;
        int d = sizeDepth;
        int rw = TurbineRodPatternLogic.rodSpaceWidth(w);
        int rh = TurbineRodPatternLogic.rodSpaceHeight(h, coilLayerCount);
        int rd = TurbineRodPatternLogic.rodSpaceDepth(d);

        int bladeCount = 0;
        java.util.List<Integer> layerCounts = new java.util.ArrayList<>();
        for (int ry = 0; ry < rh; ry++) {
            int ring = TurbineRodPatternLogic.targetBladeRingForLayer(ry, rh, rodPattern);
            int layerBlades = ring * 4;
            bladeCount += layerBlades;
            layerCounts.add(layerBlades);
        }

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

        double bladeEff = TurbineBladeEfficiency.computeMultiplier(layerCounts);

        int validBlades = bladeCount;
        if (Boolean.TRUE.equals(Config.TURBINE_REQUIRE_BALANCED_BLADE_RINGS.get())) {
            validBlades = (bladeCount / 4) * 4;
        }

        double steam = validBlades * Config.TURBINE_STEAM_MB_PER_BLADE_PER_TICK.get()
                * Config.TURBINE_CONSUMPTION_MULTIPLIER.get();
        TurbineGenerationDefinition gen = generationForSimulation(registryAccess, generationId);
        double rfPerMb = gen != null
                ? TurbineGenerationLoader.rfPerSteamMb(gen.rfProduction())
                : TurbineGenerationLoader.rfPerSteamMb(Config.TURBINE_DEFAULT_RF_PER_STEAM_BUCKET.get());
        double rf = steam * rfPerMb * coilEff * bladeEff * Config.TURBINE_PRODUCTION_MULTIPLIER.get();
        rf = Math.max(rf, Config.TURBINE_MIN_RF_PER_TICK.get());

        if (Boolean.TRUE.equals(Config.TURBINE_SIMULATION_DEBUG.get())) {
            net.unfamily.colossal_reactors.ColossalReactors.LOGGER.info(
                    "[TurbineSim] blades={} steam={} rf={} coilEff={} bladeEff={}",
                    bladeCount, steam, rf, coilEff, bladeEff);
        }

        return new SimulationResult(bladeCount, coilBlocks, steam, rf, coilEff, bladeEff);
    }

    @Nullable
    private static TurbineGenerationDefinition generationForSimulation(RegistryAccess registryAccess) {
        return generationForSimulation(registryAccess, null);
    }

    @Nullable
    private static TurbineGenerationDefinition generationForSimulation(
            RegistryAccess registryAccess, @Nullable ResourceLocation generationId) {
        if (generationId != null) {
            TurbineGenerationDefinition def = TurbineGenerationLoader.get(generationId);
            if (def != null) return def;
        }
        return TurbineGenerationLoader.getDefault();
    }

    public record RuntimeResult(long rfPerTick, double steamMbPerTick, double coilEfficiency, double bladeEfficiency) {}

    /** Runtime tick estimate from validated structure (steam cap and RF output). */
    public static RuntimeResult tickRuntime(ServerLevel level, TurbineValidation.Result result) {
        if (!result.valid()) {
            return new RuntimeResult(0, 0, 0, 0);
        }
        long rf = (long) Math.min(Long.MAX_VALUE, result.estimatedRfPerTick());
        return new RuntimeResult(rf, result.maxSteamMbPerTick(), result.coilEfficiency(), result.bladeEfficiency());
    }

    /**
     * One tick of turbine production: drain steam from INSERT resource ports, distribute RF to power ports.
     */
    public static void tick(ServerLevel level, TurbineControllerBlockEntity controller) {
        TurbineValidation.Result result = controller.getCachedResult();
        if (!result.valid()) {
            controller.setRuntimeStats(0, 0, false);
            return;
        }
        if (!TurbineControllerBlock.isRedstoneGateSatisfied(level, controller, result)) {
            controller.setRuntimeStats(0, 0, false);
            return;
        }

        long[] resourcePortPositions = controller.getCachedResourcePortPositions();
        long[] powerPortPositions = controller.getCachedPowerPortPositions();
        if (resourcePortPositions.length == 0 && powerPortPositions.length == 0
                && (result.maxSteamMbPerTick() > 0 || result.estimatedRfPerTick() > 0)) {
            controller.rebuildPartCaches(level, result);
            resourcePortPositions = controller.getCachedResourcePortPositions();
            powerPortPositions = controller.getCachedPowerPortPositions();
        }

        Fluid steamFluid = resolveSteamFluid(level.registryAccess());
        double steamDemand = result.maxSteamMbPerTick();
        int steamConsumed = 0;
        if (steamFluid != null && steamFluid != Fluids.EMPTY && steamDemand > 0) {
            int remaining = (int) Math.ceil(steamDemand);
            for (long p : resourcePortPositions) {
                if (remaining <= 0) break;
                if (level.getBlockEntity(BlockPos.of(p)) instanceof ResourcePortBlockEntity port) {
                    int drained = port.takeFluidForReactor(steamFluid, remaining);
                    steamConsumed += drained;
                    remaining -= drained;
                }
            }
        }

        double rfScale = steamDemand > 0 ? Math.min(1.0, steamConsumed / steamDemand) : 0.0;
        long rfTarget = (long) Math.min(Long.MAX_VALUE, result.estimatedRfPerTick() * rfScale);
        long rfPushed = 0;
        if (rfTarget > 0 && powerPortPositions.length > 0) {
            long perPort = rfTarget / powerPortPositions.length;
            long remainder = rfTarget % powerPortPositions.length;
            for (int i = 0; i < powerPortPositions.length; i++) {
                long offer = perPort + (i < remainder ? 1 : 0);
                if (offer <= 0) continue;
                if (level.getBlockEntity(BlockPos.of(powerPortPositions[i])) instanceof TurbinePowerPort port) {
                    rfPushed += port.receiveEnergyFromTurbine(offer);
                }
            }
        }

        controller.setRuntimeStats(rfPushed, steamConsumed, rfPushed > 0);
    }

    @Nullable
    private static Fluid resolveSteamFluid(RegistryAccess registryAccess) {
        TurbineGenerationDefinition def = TurbineGenerationLoader.getDefault();
        if (def == null || def.inputs().isEmpty()) {
            return TurbineGenerationLoader.getFirstFluidFromTag("#c:steam", registryAccess);
        }
        for (String input : def.inputs()) {
            if (input.startsWith("#")) {
                Fluid fluid = TurbineGenerationLoader.getFirstFluidFromTag(input, registryAccess);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    return fluid;
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(input);
                if (id != null) {
                    Fluid fluid = BuiltInRegistries.FLUID.get(id);
                    if (fluid != Fluids.EMPTY) {
                        return fluid;
                    }
                }
            }
        }
        return TurbineGenerationLoader.getFirstFluidFromTag("#c:steam", registryAccess);
    }
}
