package net.unfamily.colossal_reactors.compat.jei;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;

/**
 * One JEI entry for a heating coil: a specific consume option (alternative) for a coil definition.
 */
public record HeatingCoilJeiRecipe(
        ResourceLocation coilId,
        int durationTicks,
        int optionIndex,
        ConsumeOption option
) {}

