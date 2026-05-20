package net.unfamily.colossal_reactors.turbine;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One turbine generation entry: steam input, optional fluid output, RF per bucket (1000 mB) steam.
 */
public record TurbineGenerationDefinition(
        ResourceLocation generationId,
        List<String> inputs,
        String output,
        double rfProduction,
        boolean overwritable
) {
    public TurbineGenerationDefinition {
        inputs = inputs != null ? List.copyOf(inputs) : List.of();
    }
}
