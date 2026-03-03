package net.unfamily.colossal_reactors.coolant;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One coolant type: id, fluid inputs (tag or id), output fluid selector, RF/MB modifiers, and optional "water mode".
 * When consumesFluidForSteam is true: no RF output; wouldBeRf * rfToCoolantFactor = coolant consumed (mB);
 * steam produced = coolantConsumed * steamPerCoolant (default 1:1).
 * fluidColor and outputColor are ARGB (0 = use game default) for simple GUI rendering.
 */
public record CoolantDefinition(
        ResourceLocation coolantId,
        List<String> inputs,
        String output,
        int rfIncrementPercent,
        int mbDecrementPercent,
        boolean consumesFluidForSteam,
        double rfToCoolantFactor,
        double steamPerCoolant,
        int fluidColor,
        int outputColor,
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
