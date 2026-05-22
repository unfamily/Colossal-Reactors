package net.unfamily.colossal_reactors.compat.jei;

/** JEI display medium: one recipe card per liquid or gas path. */
public enum JeiMedium {
    LIQUID,
    GAS;

    public boolean matchesSelector(String selector) {
        boolean chemical = net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector.isChemicalPrefix(selector);
        return this == GAS ? chemical : !chemical;
    }
}
