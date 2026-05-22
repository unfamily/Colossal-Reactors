package net.unfamily.colossal_reactors.turbine;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;

import java.util.List;

/**
 * One turbine generation entry: steam input (fluid and/or gas), optional outputs, RF per mB steam.
 */
public record TurbineGenerationDefinition(
        ResourceLocation generationId,
        List<String> inputs,
        String output,
        List<String> outputs,
        double rfProduction,
        boolean overwritable
) {
    public TurbineGenerationDefinition {
        inputs = inputs != null ? List.copyOf(inputs) : List.of();
        if (outputs == null || outputs.isEmpty()) {
            outputs = (output != null && !output.isBlank()) ? List.of(output) : List.of();
        } else {
            outputs = List.copyOf(outputs);
        }
    }

    public String liquidOutputSelector() {
        for (String o : outputs) {
            if (!MaterialSelector.isChemicalPrefix(o)) return o;
        }
        return output != null ? output : "";
    }

    public String gasOutputSelector() {
        for (String o : outputs) {
            if (MaterialSelector.isChemicalPrefix(o)) return o;
        }
        return null;
    }
}
