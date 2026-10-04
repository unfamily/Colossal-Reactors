package net.unfamily.colossal_reactors.compat.rei;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.compat.jei.JeiNativeRecipeBridge;

/**
 * Same data source as JEI ({@link JeiNativeRecipeBridge}), registered as REI displays.
 * Only fills when the category is empty so the in-world sync can populate after RecipeManager is live.
 */
public final class ColossalReiRecipeRegistrar {
    private ColossalReiRecipeRegistrar() {}

    public static void registerAll(DisplayRegistry registry) {
        addIfEmpty(registry, ColossalReiCategories.FUEL, JeiNativeRecipeBridge.fuels(), ColossalReiDisplays::fuel);
        addIfEmpty(registry, ColossalReiCategories.COOLANT, JeiNativeRecipeBridge.coolants(), ColossalReiDisplays::coolant);
        addIfEmpty(registry, ColossalReiCategories.HEAT_SINK, JeiNativeRecipeBridge.heatSinks(), ColossalReiDisplays::heatSink);
        addIfEmpty(registry, ColossalReiCategories.MELTER, JeiNativeRecipeBridge.melterRecipes(), ColossalReiDisplays::melter);
        addIfEmpty(registry, ColossalReiCategories.MELTER_HEAT, JeiNativeRecipeBridge.melterHeats(), ColossalReiDisplays::melterHeat);
        addIfEmpty(registry, ColossalReiCategories.HEATING_COIL, JeiNativeRecipeBridge.heatingCoils(), ColossalReiDisplays::heatingCoil);
        addIfEmpty(registry, ColossalReiCategories.ELEC_COIL, JeiNativeRecipeBridge.elecCoils(), ColossalReiDisplays::elecCoil);
        addIfEmpty(
                registry,
                ColossalReiCategories.TURBINE,
                JeiNativeRecipeBridge.turbineGeneration(),
                ColossalReiDisplays::turbineGeneration);
    }

    private static <T> void addIfEmpty(
            DisplayRegistry registry,
            CategoryIdentifier<ColossalReiDisplay> category,
            List<T> recipes,
            Function<T, ColossalReiDisplay> factory) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }
        if (!registry.get(category).isEmpty()) {
            return;
        }
        // Skip duplicate RecipeManager ids (safe if a source ever emits the same card twice).
        Set<ResourceLocation> seenIds = new HashSet<>();
        for (T recipe : recipes) {
            ColossalReiDisplay display = factory.apply(recipe);
            if (display == null) {
                continue;
            }
            ResourceLocation id = display.getDisplayLocation().orElse(null);
            if (id != null && !seenIds.add(id)) {
                continue;
            }
            registry.add(display);
        }
    }
}
