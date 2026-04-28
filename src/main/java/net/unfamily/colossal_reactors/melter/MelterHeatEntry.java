package net.unfamily.colossal_reactors.melter;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * One entry from melter_heats: blocks and/or fluids that provide a heat factor.
 * Factor is used in final_time = time / (UP * DOWN * EAST * NORTH / SOUTH / WEST).
 * If notValid is true, the block/fluid still contributes its factor to the heat multiplier
 * but does not count as a valid heating source to start the melter (at least one valid source required).
 */
public record MelterHeatEntry(
        List<Identifier> blockIds,
        List<Boolean> blockIdIsTag,
        List<Identifier> fluidIds,
        List<Boolean> fluidIdIsTag,
        double factor,
        boolean notValid
) {}
