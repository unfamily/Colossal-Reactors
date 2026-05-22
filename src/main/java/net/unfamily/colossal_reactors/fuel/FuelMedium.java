package net.unfamily.colossal_reactors.fuel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.colossal_reactors.datapack.DatapackSelectorValidator;
import net.unfamily.colossal_reactors.util.FluidInputMatcher;

/**
 * Input/output medium parsed from {@link FuelDefinition#subType()} ({@code input-output}).
 */
public enum FuelMedium {
    ITEM,
    FLUID,
    CHEMICAL;

    public static final String NAME_ITEM = "item";
    public static final String NAME_FLUID = "fluid";
    public static final String NAME_CHEMICAL = "chemical";

    public static FuelMedium fromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return switch (name.trim().toLowerCase()) {
            case NAME_ITEM -> ITEM;
            case NAME_FLUID -> FLUID;
            case NAME_CHEMICAL -> CHEMICAL;
            default -> null;
        };
    }

    /** Medium implied by a datapack selector (item id/tag, fluid id/tag, or {@code %chemical} when Mek is present). */
    public static FuelMedium fromSelector(String selector) {
        if (selector == null || selector.isBlank()) {
            return null;
        }
        if (FluidInputMatcher.isChemicalPrefix(selector)) {
            return CHEMICAL;
        }
        if (selector.startsWith("#")) {
            return DatapackSelectorValidator.isResolvableFluidSelector(selector) ? FLUID : ITEM;
        }
        Identifier id = Identifier.tryParse(selector);
        if (id != null) {
            Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
            if (fluid != null && fluid != Fluids.EMPTY) {
                return FLUID;
            }
        }
        return ITEM;
    }
}
