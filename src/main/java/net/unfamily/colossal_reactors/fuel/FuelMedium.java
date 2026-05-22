package net.unfamily.colossal_reactors.fuel;

import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;

/**
 * Input/output medium parsed from {@link FuelDefinition#subType()} ({@code input-output}, e.g. {@code fluid-chemical}).
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

    /** Medium implied by a datapack input/output selector string. */
    public static FuelMedium fromSelector(String selector) {
        if (selector == null || selector.isBlank()) {
            return null;
        }
        if (MaterialSelector.isChemicalPrefix(selector)) {
            return CHEMICAL;
        }
        if (MaterialSelector.isFluidSelector(selector)) {
            return FLUID;
        }
        return ITEM;
    }
}
