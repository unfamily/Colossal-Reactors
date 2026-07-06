package net.unfamily.colossal_reactors.integration.mekanism;

import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Datapack selector helpers for Mek chemical prefixes ({@code %namespace:id}).
 */
public final class MaterialSelector {

    private MaterialSelector() {}

    public static boolean isChemicalPrefix(String selector) {
        return selector != null && selector.startsWith("%");
    }

    /** Mek chemical stack match (reflection). */
    public static boolean matchesChemical(@Nullable Object chemicalStack, String selector) {
        if (!MekChemicalHelper.isLoaded()) {
            return false;
        }
        return MekChemicalHelper.matchesSelector(chemicalStack, selector);
    }
}
