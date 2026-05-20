package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.turbine.TurbineSimulation;
import org.jetbrains.annotations.Nullable;

/** Builder GUI simulation delegate. */
public final class TurbineBuilderSimulation {

    private TurbineBuilderSimulation() {}

    public static TurbineSimulation.SimulationResult run(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int rodPattern, int coilIndex, int coilLayerCount) {
        return run(registryAccess, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                rodPattern, coilIndex, coilLayerCount, null);
    }

    public static TurbineSimulation.SimulationResult run(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int rodPattern, int coilIndex, int coilLayerCount,
            @Nullable ResourceLocation generationId) {
        return TurbineSimulation.simulateFromBuilderParams(
                registryAccess, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                rodPattern, coilIndex, coilLayerCount, generationId);
    }
}
