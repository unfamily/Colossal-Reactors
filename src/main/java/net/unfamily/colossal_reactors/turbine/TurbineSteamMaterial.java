package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;
import net.unfamily.colossal_reactors.util.FluidInputMatcher;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Maps turbine generation steam inputs to fluid buffer ids and Mek gas stacks for resource ports. */
final class TurbineSteamMaterial {

    private TurbineSteamMaterial() {}

    @Nullable
    static Fluid resolveBufferFluid(List<String> inputs, RegistryAccess registryAccess) {
        if (inputs == null || registryAccess == null) {
            return null;
        }
        for (String input : inputs) {
            if (input == null || input.isBlank() || FluidInputMatcher.isChemicalPrefix(input)) {
                continue;
            }
            if (input.startsWith("#")) {
                Fluid fluid = TurbineGenerationLoader.getFirstFluidFromTag(input, registryAccess);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    return fluid;
                }
            } else {
                Identifier id = Identifier.tryParse(input);
                if (id != null) {
                    Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
                    if (fluid != null && fluid != Fluids.EMPTY) {
                        return fluid;
                    }
                }
            }
        }
        return TurbineSimulation.resolveInputSteamFluid(registryAccess);
    }

    @Nullable
    static Object createGasStack(long amountMb, List<String> inputs) {
        if (!MekChemicalHelper.isLoaded() || amountMb <= 0 || inputs == null) {
            return null;
        }
        for (String input : inputs) {
            if (!FluidInputMatcher.isChemicalPrefix(input)) {
                continue;
            }
            Identifier chemId = Identifier.tryParse(input.substring(1));
            if (chemId == null) {
                continue;
            }
            return MekChemicalHelper.createStack(chemId, amountMb);
        }
        return MekChemicalHelper.createStack(
                Identifier.fromNamespaceAndPath("mekanism", "steam"), amountMb);
    }

    static boolean isSteamFluid(Fluid fluid, List<String> inputs) {
        return FluidInputMatcher.matchesAnyFluidInput(fluid, inputs);
    }
}
