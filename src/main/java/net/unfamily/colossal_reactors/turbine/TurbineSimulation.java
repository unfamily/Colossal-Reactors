package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.TurbineControllerBlock;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbinePowerPort;
import org.jetbrains.annotations.Nullable;

/**
 * Turbine RF/steam simulation for builder GUI and runtime estimates.
 */
public final class TurbineSimulation {

    public record SimulationResult(
            int bladeCount,
            int validBladeCount,
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
                TurbinePlacementAxis.DEFAULT_INDEX, rodPattern, coilIndex, coilLayerCount, null);
    }

    public static SimulationResult simulateFromBuilderParams(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int placementAxisIndex,
            int rodPattern, int coilIndex, int storedCoilSetting,
            @Nullable Identifier generationId) {

        TurbineGenerationDefinition gen = generationForSimulation(registryAccess, generationId);
        TurbineProductionMath.ProductionEstimate production = TurbineProductionMath.fromBuilderParams(
                sizeLeft, sizeRight, sizeHeight, sizeDepth,
                placementAxisIndex, rodPattern, coilIndex, storedCoilSetting, gen);

        if (Boolean.TRUE.equals(Config.TURBINE_SIMULATION_DEBUG.get())) {
            net.unfamily.colossal_reactors.ColossalReactors.LOGGER.info(
                    "[TurbineSim] blades={} validBlades={} steam={} rf={}",
                    production.bladeCount(), production.validBladeCount(),
                    production.maxSteamMbPerTick(), production.estimatedRfPerTick());
        }

        return new SimulationResult(
                production.bladeCount(),
                production.validBladeCount(),
                production.coilBlockCount(),
                production.maxSteamMbPerTick(),
                production.estimatedRfPerTick(),
                production.coilEfficiency(),
                production.bladeEfficiency());
    }

    @Nullable
    private static TurbineGenerationDefinition generationForSimulation(
            RegistryAccess registryAccess, @Nullable Identifier generationId) {
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
     * One tick of turbine production: consume buffered steam, distribute RF to power ports, push output fluid.
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

        Fluid steamFluid = resolveInputSteamFluid(level.registryAccess());
        TurbineGenerationDefinition gen = TurbineGenerationLoader.getDefault();
        Fluid outputFluid = TurbineGenerationLoader.getOutputFluid(gen, level.registryAccess());

        double steamDemand = result.maxSteamMbPerTick();
        int steamConsumed = 0;
        if (steamFluid != null && steamFluid != Fluids.EMPTY && steamDemand > 0) {
            steamConsumed = controller.consumeSteamInput(steamFluid, (int) Math.ceil(steamDemand));
            if (steamConsumed > 0 && outputFluid != null && outputFluid != Fluids.EMPTY) {
                controller.addOutputReturn(outputFluid, steamConsumed);
            }
        }

        pushOutputReturnToFluidPorts(level, controller, resourcePortPositions);

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

    private static void pushOutputReturnToFluidPorts(
            ServerLevel level,
            TurbineControllerBlockEntity controller,
            long[] resourcePortPositions) {
        for (var entry : controller.getOutputReturnEntries()) {
            if (entry.mb() <= 0) continue;
            Fluid fluid = BuiltInRegistries.FLUID.getValue(entry.fluidId());
            if (fluid == null || fluid == Fluids.EMPTY) continue;
            int remaining = entry.mb();
            int consumed = controller.consumeOutputReturn(fluid, remaining);
            if (consumed <= 0) continue;
            remaining = consumed;
            for (long p : resourcePortPositions) {
                if (remaining <= 0) break;
                if (!(level.getBlockEntity(BlockPos.of(p)) instanceof ResourcePortBlockEntity port)) continue;
                PortMode mode = port.getPortMode();
                if (mode != PortMode.EXTRACT && mode != PortMode.EJECT) continue;
                int filled = port.receiveFluidFromReactor(new FluidStack(fluid, remaining));
                remaining -= filled;
            }
            if (remaining > 0) {
                controller.addOutputReturn(fluid, remaining);
            }
        }
    }

    @Nullable
    public static Fluid resolveInputSteamFluid(RegistryAccess registryAccess) {
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
                Identifier id = Identifier.tryParse(input);
                if (id != null) {
                    Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
                    if (fluid != Fluids.EMPTY) {
                        return fluid;
                    }
                }
            }
        }
        return TurbineGenerationLoader.getFirstFluidFromTag("#c:steam", registryAccess);
    }

    public static void flushFluidBuffersToEject(ServerLevel level, TurbineControllerBlockEntity controller) {
        long[] resourcePortPositions = controller.getCachedResourcePortPositions();
        if (resourcePortPositions.length == 0 && controller.getCachedResult().valid()) {
            controller.rebuildPartCaches(level, controller.getCachedResult());
            resourcePortPositions = controller.getCachedResourcePortPositions();
        }
        Fluid steamFluid = resolveInputSteamFluid(level.registryAccess());
        if (steamFluid != null && steamFluid != Fluids.EMPTY) {
            pushFluidBufferToEject(level, controller, resourcePortPositions, steamFluid, true);
        }
        for (var entry : controller.getOutputReturnEntries()) {
            if (entry.mb() <= 0) continue;
            Fluid fluid = BuiltInRegistries.FLUID.getValue(entry.fluidId());
            if (fluid == null || fluid == Fluids.EMPTY) continue;
            pushFluidBufferToEject(level, controller, resourcePortPositions, fluid, false);
        }
    }

    private static void pushFluidBufferToEject(
            ServerLevel level,
            TurbineControllerBlockEntity controller,
            long[] resourcePortPositions,
            Fluid fluid,
            boolean steamInput) {
        int total = steamInput ? controller.getTotalSteamInputMb() : controller.getTotalOutputReturnMb();
        if (total <= 0) return;
        int remaining = steamInput
                ? controller.consumeSteamInput(fluid, total)
                : controller.consumeOutputReturn(fluid, total);
        while (remaining > 0) {
            int moved = 0;
            for (long p : resourcePortPositions) {
                if (remaining <= 0) break;
                if (!(level.getBlockEntity(BlockPos.of(p)) instanceof ResourcePortBlockEntity port)) continue;
                PortMode mode = port.getPortMode();
                if (mode != PortMode.EXTRACT && mode != PortMode.EJECT) continue;
                int filled = port.receiveFluidFromReactor(new FluidStack(fluid, remaining));
                remaining -= filled;
                moved += filled;
            }
            if (moved <= 0) {
                if (steamInput) controller.addSteamInput(fluid, remaining);
                else controller.addOutputReturn(fluid, remaining);
                break;
            }
        }
    }
}
