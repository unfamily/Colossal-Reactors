package net.unfamily.colossal_reactors.fuel;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One fuel type: id, item/tag/chemical inputs, waste output, and per-fuel parameters.
 * {@code subType} describes input/output medium: item-item (default), chemical-chemical, item-chemical, etc.
 */
public record FuelDefinition(
        ResourceLocation fuelId,
        String subType,
        List<String> inputs,
        String output,
        int unitsPerFuel,
        int unitsPerWaste,
        double baseRfPerTick,
        double baseFuelUnitsPerTick,
        boolean overwritable
) {
    public static final String SUBTYPE_ITEM_ITEM = "item-item";

    public FuelDefinition {
        if (subType == null || subType.isBlank()) {
            subType = SUBTYPE_ITEM_ITEM;
        }
        inputs = inputs != null ? List.copyOf(inputs) : List.of();
    }

    public boolean isChemicalFuel() {
        return subType != null && subType.contains("chemical");
    }

    public boolean isChemicalWaste() {
        return output != null && output.startsWith("%");
    }
}
