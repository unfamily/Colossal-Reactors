package net.unfamily.colossal_reactors.fuel;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One fuel type: id, item/tag/chemical inputs, waste output, and per-fuel parameters.
 * {@code subType} describes input/output medium: item-item (default), chemical-chemical, item-chemical, etc.
 */
public record FuelDefinition(
        ResourceLocation fuelId,
        /** Aggregated waste buffer key; defaults to {@code fuelId} when omitted in JSON. */
        ResourceLocation wasteId,
        String subType,
        List<String> inputs,
        /** Input amount (1 item, 1 mB, etc.) required per {@code unitsPerFuel} grant; default 1. */
        int consume,
        String output,
        /** Output amount (1 item, 1 mB, etc.) per {@code unitsPerWaste} grant; default 1. */
        int produce,
        int unitsPerFuel,
        int unitsPerWaste,
        double baseRfPerTick,
        double baseFuelUnitsPerTick,
        boolean overwritable
) {
    public static final String SUBTYPE_ITEM_ITEM = "item-item";

    public FuelDefinition {
        if (wasteId == null) {
            wasteId = fuelId;
        }
        if (subType == null || subType.isBlank()) {
            subType = SUBTYPE_ITEM_ITEM;
        }
        inputs = inputs != null ? List.copyOf(inputs) : List.of();
        if (consume <= 0) {
            consume = 1;
        }
        if (produce <= 0) {
            produce = 1;
        }
    }

    /** Fuel units granted for {@code inputAmount} items/mB (e.g. consume=500, units_per_fuel=50000 → 500 mB = 50000 units). */
    public float fuelUnitsFromInputAmount(float inputAmount) {
        return inputAmount * unitsPerFuel / (float) consume;
    }

    /** Fuel units per 1 mB or 1 item. */
    public float fuelUnitsPerInputUnit() {
        return fuelUnitsFromInputAmount(1f);
    }

    /** How many mB/items fit in {@code spaceUnits} of reactor buffer (0 if less than one unit fits). */
    public int inputAmountForSpaceUnits(float spaceUnits) {
        float per = fuelUnitsPerInputUnit();
        if (per <= 0f) {
            return 0;
        }
        return (int) Math.floor(spaceUnits / per);
    }

    /**
     * Pull size for chemical fuel: largest multiple of {@link #consume()} when possible; otherwise all
     * {@code maxMbAvailable} (reactor capacity may be smaller than one batch, e.g. 100 mB in a 10k-unit rod).
     */
    public int pullAmountMb(int maxMbAvailable) {
        if (maxMbAvailable <= 0) {
            return 0;
        }
        int batch = consume;
        if (maxMbAvailable >= batch) {
            return (maxMbAvailable / batch) * batch;
        }
        return maxMbAvailable;
    }

    /** mB/items actually consumed for {@code grantedUnits} fuel buffer units. */
    public int inputAmountForGrantedUnits(float grantedUnits) {
        float per = fuelUnitsPerInputUnit();
        if (per <= 0f) {
            return 0;
        }
        return (int) Math.ceil(grantedUnits / per - 1e-6f);
    }

    /** Fuel units represented by one item when ejecting solid fuel back to ports. */
    public float fuelUnitsPerItemStack() {
        return fuelUnitsFromInputAmount(1f);
    }

    /** Waste units accumulated when {@code fuelUnitsConsumed} fuel units are burned. */
    public float wasteUnitsFromFuelConsumed(float fuelUnitsConsumed) {
        if (unitsPerFuel <= 0) {
            return fuelUnitsConsumed;
        }
        return fuelUnitsConsumed * (float) unitsPerWaste / (float) unitsPerFuel;
    }

    /**
     * mB or item count to eject from {@code wasteUnits} in buffer
     * (e.g. produce=500, units_per_waste=50000 → 500 mB per 50000 waste units).
     */
    public int wasteOutputAmountFromWasteUnits(float wasteUnits) {
        if (unitsPerWaste <= 0) {
            return 0;
        }
        return (int) Math.floor(wasteUnits * produce / (float) unitsPerWaste);
    }

    public boolean isChemicalFuel() {
        return subType != null && subType.contains("chemical");
    }

    public boolean isChemicalWaste() {
        return output != null && output.startsWith("%");
    }
}
