package net.unfamily.colossal_reactors.compat.rei;

import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import net.unfamily.colossal_reactors.compat.jei.JeiNativeRecipeBridge;

/**
 * Same data source as JEI ({@link JeiNativeRecipeBridge}), registered as REI displays.
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
            java.util.List<T> recipes,
            java.util.function.Function<T, ColossalReiDisplay> factory) {
        if (recipes == null || recipes.isEmpty()) {
            return;
        }
        if (!registry.get(category).isEmpty()) {
            return;
        }
        for (T recipe : recipes) {
            ColossalReiDisplay display = factory.apply(recipe);
            if (display != null) {
                registry.add(display);
            }
        }
    }
}
