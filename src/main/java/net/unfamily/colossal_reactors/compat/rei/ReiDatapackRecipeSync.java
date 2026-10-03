package net.unfamily.colossal_reactors.compat.rei;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Holds the REI display registry and fills static displays after datapack/world ready.
 * Dynamic generators registered in {@link ColossalReiRecipeRegistrar} cover most lookups.
 */
public final class ReiDatapackRecipeSync {
    private static DisplayRegistry registry;

    private ReiDatapackRecipeSync() {}

    public static void onRegistryAvailable(DisplayRegistry displayRegistry) {
        registry = displayRegistry;
        syncWhenWorldReady();
    }

    public static void syncWhenWorldReady() {
        if (FMLEnvironment.getDist() != Dist.CLIENT || registry == null) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            return;
        }
        ColossalReiRecipeRegistrar.fillMissing(registry);
    }
}
