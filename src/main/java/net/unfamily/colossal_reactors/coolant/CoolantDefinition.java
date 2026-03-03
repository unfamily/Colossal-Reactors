package net.unfamily.colossal_reactors.coolant;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One coolant type: id, fluid inputs (tag or id), and output fluid selector.
 * Output is a fluid tag (e.g. "#c:steam"); at runtime we only output if the tag resolves to a valid fluid (otherwise no output).
 */
public record CoolantDefinition(
        ResourceLocation coolantId,
        List<String> inputs,
        String output,
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
}
