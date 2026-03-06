package net.unfamily.colossal_reactors.melter;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

import java.util.List;

/**
 * One entry from melter_heats: blocks and/or fluids that provide a heat factor.
 * Factor is used in final_time = time / (UP * DOWN * EAST * NORTH / SOUTH / WEST).
 */
public record MelterHeatEntry(
        List<ResourceLocation> blockIds,
        List<Boolean> blockIdIsTag,
        List<ResourceLocation> fluidIds,
        List<Boolean> fluidIdIsTag,
        double factor
) {}
