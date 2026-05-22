package net.unfamily.colossal_reactors.compat.jei;

/** JEI display medium: one recipe card per liquid or gas path. */
public enum JeiMedium {
    LIQUID,
    GAS;

    public boolean matchesSelector(String selector) {
        boolean chemical = selector != null && selector.startsWith("%");
        return this == GAS ? chemical : !chemical;
    }
}
