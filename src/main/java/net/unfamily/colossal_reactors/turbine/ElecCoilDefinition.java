package net.unfamily.colossal_reactors.turbine;

import java.util.List;

/**
 * One elec coil entry from datapack: valid blocks, efficiency coefficients.
 */
public record ElecCoilDefinition(
        List<String> validBlocks,
        double effCoe,
        double effMax
) {
    public ElecCoilDefinition {
        validBlocks = validBlocks != null ? List.copyOf(validBlocks) : List.of();
    }
}
