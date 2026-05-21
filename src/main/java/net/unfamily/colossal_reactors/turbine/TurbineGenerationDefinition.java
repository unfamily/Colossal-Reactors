package net.unfamily.colossal_reactors.turbine;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * One turbine generation entry: steam input, optional fluid output, {@code rf_production} is RF per mB steam.
 */
public record TurbineGenerationDefinition(
        Identifier generationId,
        List<String> inputs,
        String output,
        double rfProduction,
        boolean overwritable
) {
    public TurbineGenerationDefinition {
        inputs = inputs != null ? List.copyOf(inputs) : List.of();
    }
}
