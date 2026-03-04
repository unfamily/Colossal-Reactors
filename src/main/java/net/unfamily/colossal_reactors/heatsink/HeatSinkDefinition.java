package net.unfamily.colossal_reactors.heatsink;

import java.util.List;

/**
 * One heat sink entry from JSON: valid blocks/liquids, fuel/energy and overheating multipliers.
 * Overheating is used only for stability (surriscaldamento); defaults to same as fuel when omitted in JSON.
 * Only used when reactor instability is enabled in config (evil_things / REACTOR_UNSTABILITY).
 * When {@code mustSource} is true (default), only fluid "source" counts for valid_liquids; flowing fluid is not valid.
 */
public record HeatSinkDefinition(
        List<String> validBlocks,
        List<String> validLiquids,
        double fuelMultiplier,
        double energyMultiplier,
        double overheatingMultiplier,
        boolean mustSource
) {
    public HeatSinkDefinition {
        validBlocks = validBlocks != null ? List.copyOf(validBlocks) : List.of();
        validLiquids = validLiquids != null ? List.copyOf(validLiquids) : List.of();
    }
}
