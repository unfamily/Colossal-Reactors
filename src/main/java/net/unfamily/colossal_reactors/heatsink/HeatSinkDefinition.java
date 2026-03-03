package net.unfamily.colossal_reactors.heatsink;

import java.util.List;

/**
 * One heat sink entry from JSON: valid blocks/liquids and fuel/energy multipliers.
 * Used for interior coolant blocks (and optionally liquids in rods).
 * When {@code mustSource} is true (default), only fluid "source" (e.g. water source block) counts for valid_liquids; flowing fluid is not valid.
 */
public record HeatSinkDefinition(
        List<String> validBlocks,
        List<String> validLiquids,
        double fuelMultiplier,
        double energyMultiplier,
        boolean mustSource
) {
    public HeatSinkDefinition {
        validBlocks = validBlocks != null ? List.copyOf(validBlocks) : List.of();
        validLiquids = validLiquids != null ? List.copyOf(validLiquids) : List.of();
    }
}
