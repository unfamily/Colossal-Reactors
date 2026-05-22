package net.unfamily.colossal_reactors.turbine;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * One turbine generation entry: steam input (fluid and/or gas), optional outputs, RF per mB steam.
 */
public record TurbineGenerationDefinition(
        Identifier generationId,
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

    /** First fluid output selector (# or plain id), or legacy output. */
    public String liquidOutputSelector() {
        for (String o : outputs) {
            if (o != null && !o.isBlank() && !o.startsWith("%")) return o;
        }
        return output != null ? output : "";
    }
}
