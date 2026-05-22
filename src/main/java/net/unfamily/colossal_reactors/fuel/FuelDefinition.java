package net.unfamily.colossal_reactors.fuel;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * One fuel type: id, item/tag inputs, waste output (item tag or id), and per-fuel parameters.
 */
public record FuelDefinition(
        Identifier fuelId,
        Identifier wasteId,
        List<String> inputs,
        int consume,
        String output,
        int produce,
        int unitsPerFuel,
        int unitsPerWaste,
        double baseRfPerTick,
        double baseFuelUnitsPerTick,
        boolean overwritable
) {
    public FuelDefinition {
        if (wasteId == null) {
            wasteId = fuelId;
        }
        if (consume <= 0) {
            consume = 1;
        }
        if (produce <= 0) {
            produce = 1;
        }
    }

    public float fuelUnitsFromInputAmount(float inputAmount) {
        return inputAmount * unitsPerFuel / (float) consume;
    }

    public float fuelUnitsPerItemStack() {
        return fuelUnitsFromInputAmount(1f);
    }

    public float wasteUnitsFromFuelConsumed(float fuelUnitsConsumed) {
        if (unitsPerFuel <= 0) {
            return fuelUnitsConsumed;
        }
        return fuelUnitsConsumed * (float) unitsPerWaste / (float) unitsPerFuel;
    }

    public int wasteOutputAmountFromWasteUnits(float wasteUnits) {
        if (unitsPerWaste <= 0) {
            return 0;
        }
        return (int) Math.floor(wasteUnits * produce / (float) unitsPerWaste);
    }
}
