package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.EmiApi;

/** Opens Melter melting recipes in EMI (not heat-source recipes). */
public final class MelterEmiProgressClick {
    private MelterEmiProgressClick() {}

    public static void openMelterRecipes() {
        EmiApi.displayRecipeCategory(EmiCategories.MELTER);
    }
}
