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
            int placementAxisIndex,
            int rodPattern, int coilIndex, int storedCoilSetting) {
        return run(registryAccess, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                placementAxisIndex, rodPattern, coilIndex, storedCoilSetting, null);
    }

    public static TurbineSimulation.SimulationResult run(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int placementAxisIndex,
            int rodPattern, int coilIndex, int storedCoilSetting,
            @Nullable ResourceLocation generationId) {
        return TurbineSimulation.simulateFromBuilderParams(
                registryAccess, sizeLeft, sizeRight, sizeHeight, sizeDepth,
                placementAxisIndex, rodPattern, coilIndex, storedCoilSetting, generationId);
    }
}
