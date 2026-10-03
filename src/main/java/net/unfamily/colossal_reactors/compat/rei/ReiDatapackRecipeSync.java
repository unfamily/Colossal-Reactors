package net.unfamily.colossal_reactors.compat.rei;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Same role as {@link net.unfamily.colossal_reactors.compat.jei.JeiDatapackRecipeSync}:
 * REI often reloads before Colossal loaders / RecipeManager are ready; fill again in-world.
 */
public final class ReiDatapackRecipeSync {
    private static DisplayRegistry registry;

    private ReiDatapackRecipeSync() {}

    public static void onRegistryAvailable(DisplayRegistry displayRegistry) {
        registry = displayRegistry;
        syncWhenWorldReady();
    }

    public static void syncWhenWorldReady() {
        if (FMLEnvironment.dist != Dist.CLIENT || registry == null) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            return;
        }
        ColossalReiRecipeRegistrar.registerAll(registry);
    }
}
