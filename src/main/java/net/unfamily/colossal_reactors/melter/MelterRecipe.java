package net.unfamily.colossal_reactors.melter;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

/**
 * One melter recipe: consume input (item/tag) to produce output fluid (fluid id or fluid tag).
 * final_time = time / (UP * DOWN * EAST * WEST * NORTH * SOUTH); sides without heat source = 1.
 */
public record MelterRecipe(
        Identifier inputId,
        boolean inputIsTag,
        Identifier outputFluidId,
        boolean outputIsTag,
        int amountMb,
        int timeTicks,
        int count
) {
    /** Default count when not specified in JSON. */
    public static final int DEFAULT_COUNT = 1;
}
