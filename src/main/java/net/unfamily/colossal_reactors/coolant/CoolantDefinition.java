package net.unfamily.colossal_reactors.coolant;

import net.minecraft.resources.Identifier;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One coolant type: fluid/gas inputs, liquid/gas outputs (max 2: one fluid + one gas), RF/MB modifiers.
 */
public record CoolantDefinition(
        Identifier coolantId,
        List<String> inputs,
        String output,
        List<String> outputs,
        int rfIncrementPercent,
        int mbDecrementPercent,
        boolean reduceRfProduction,
        double rfToCoolantFactor,
        double steamPerCoolant,
        double overheatingMultiplier,
        int fluidColor,
        int outputColor,
        boolean overwritable
) {
    public CoolantDefinition {
        inputs = inputs != null ? List.copyOf(inputs) : List.of();
        outputs = normalizeOutputs(output, outputs);
    }

    private static List<String> normalizeOutputs(String legacyOutput, List<String> outputsList) {
        if (outputsList != null && !outputsList.isEmpty()) {
            return List.copyOf(outputsList);
        }
        if (legacyOutput != null && !legacyOutput.isBlank()) {
            return List.of(legacyOutput);
        }
        return List.of();
    }

    /** First fluid output selector (# or plain id), or legacy output. */
    public String liquidOutputSelector() {
        for (String o : outputs) {
            if (o == null || o.isBlank() || o.startsWith("%")) continue;
            return o;
        }
        return output != null ? output : "";
    }

    /** First gas/chemical output selector (%), or null. */
    @Nullable
    public String gasOutputSelector() {
        for (String o : outputs) {
            if (o != null && o.startsWith("%")) {
                return o;
            }
        }
        return null;
    }

    public double rfMultiplier() {
        return 1.0 + rfIncrementPercent / 100.0;
    }

    public double mbMultiplier() {
        return mbDecrementPercent / 100.0;
    }
}
