package net.unfamily.colossal_reactors.compat;

import net.neoforged.fml.ModList;

/**
 * Opens Melter recipes from the GUI progress bar when the active recipe viewer needs a manual click
 * (EMI). JEI/REI register native click areas instead.
 */
public final class MelterProgressBarRecipes {
    private MelterProgressBarRecipes() {}

    /**
     * @return true if a viewer handled the click (screen should consume it)
     */
    public static boolean tryOpenFromProgressBar() {
        if (!ModList.get().isLoaded("emi")) {
            return false;
        }
        try {
            Class.forName("net.unfamily.colossal_reactors.compat.emi.MelterEmiProgressClick")
                    .getMethod("openMelterRecipes")
                    .invoke(null);
            return true;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            return false;
        }
    }
}
