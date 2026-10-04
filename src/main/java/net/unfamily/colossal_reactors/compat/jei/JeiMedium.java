package net.unfamily.colossal_reactors.compat.jei;

/** Selector medium helper (liquid vs Mek chemical). Kept for FuelMedium-style partitioning. */
public enum JeiMedium {
    LIQUID,
    GAS;

    public boolean matchesSelector(String selector) {
        boolean chemical = net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector.isChemicalPrefix(selector);
        return this == GAS ? chemical : !chemical;
    }
}
