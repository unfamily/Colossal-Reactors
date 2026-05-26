package net.unfamily.colossal_reactors.integration.mekanism;

/**
 * Datapack selector helpers for Mek chemical prefixes ({@code %namespace:id}).
 */
public final class MaterialSelector {

    private MaterialSelector() {}

    public static boolean isChemicalPrefix(String selector) {
        return selector != null && selector.startsWith("%");
    }
}
