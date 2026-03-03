package net.unfamily.colossal_reactors.coolant;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One coolant type: id, fluid inputs (tag or id), output fluid selector, and RF/MB modifiers for simulation.
 * Output is a fluid tag (e.g. "#c:steam"); at runtime we only output if the tag resolves to a valid fluid.
 * rfIncrementPercent: 0 = 1.0x RF, 2 = 1.02x. mbDecrementPercent: 100 = 1.0x consumption, 150 = 1.5x.
 */
public record CoolantDefinition(
        ResourceLocation coolantId,
        List<String> inputs,
        String output,
        int rfIncrementPercent,
        int mbDecrementPercent,
        boolean overwritable
) {
    /** Inputs: "#namespace:tag" (fluid tag) or "namespace:fluid_id" (fluid). */
    public List<String> inputs() {
        return inputs;
    }

    /** Output: fluid tag e.g. "#c:steam". If tag has no valid fluid at runtime, no output is produced. */
    public String output() {
        return output;
    }

    /** RF multiplier: 1 + rfIncrementPercent/100 (e.g. 0 → 1.0, 2 → 1.02). */
    public double rfMultiplier() {
        return 1.0 + rfIncrementPercent / 100.0;
    }

    /** MB (consumption) multiplier: mbDecrementPercent/100 (e.g. 100 → 1.0, 150 → 1.5). */
    public double mbMultiplier() {
        return mbDecrementPercent / 100.0;
    }
}
