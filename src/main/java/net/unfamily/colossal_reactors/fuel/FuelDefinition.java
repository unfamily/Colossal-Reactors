package net.unfamily.colossal_reactors.fuel;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One fuel type: id, item/tag inputs, waste output (item tag or id), and per-fuel parameters.
 * Used by FuelLoader; entries can be overridden by JSON in the fuel directory.
 * Input: 1 item = unitsPerFuel fuel units. Output: every unitsPerWaste consumed units = 1 waste item.
 */
public record FuelDefinition(
        ResourceLocation fuelId,
        List<String> inputs,
        String output,
        int unitsPerFuel,
        int unitsPerWaste,
        double baseRfPerTick,
        double baseFuelUnitsPerTick,
        boolean overwritable
) {
    /** Inputs are either "#namespace:tag" (item tag) or "namespace:item_id" (item). */
    public List<String> inputs() {
        return inputs;
    }

    /** Output (waste): "#tag" or "namespace:item_id". Resolved at runtime: if tag, use first valid item; if none, no output. */
    public String output() {
        return output;
    }
}
