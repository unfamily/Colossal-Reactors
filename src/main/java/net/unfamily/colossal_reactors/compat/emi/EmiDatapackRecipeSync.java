package net.unfamily.colossal_reactors.compat.emi;

/**
 * EMI rebuilds itself when recipes/tags sync; Colossal recipes are added only in
 * {@link ColossalEmiPlugin#register}. Do not keep a post-bake {@link dev.emi.emi.api.EmiRegistry}
 * and mutate it — that removed Colossal recipes from the live index.
 */
public final class EmiDatapackRecipeSync {

    private EmiDatapackRecipeSync() {}

    public static void onRegistryAvailable(dev.emi.emi.api.EmiRegistry emiRegistry) {
        // no-op (call-site compatibility)
    }

    public static void syncWhenWorldReady() {
        // no-op: registration happens during EMI plugin reload from RecipeManager
    }
}
