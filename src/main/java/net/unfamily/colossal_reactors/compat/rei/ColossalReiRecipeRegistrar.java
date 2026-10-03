package net.unfamily.colossal_reactors.compat.rei;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.display.DynamicDisplayGenerator;
import me.shedaniel.rei.api.client.view.ViewSearchBuilder;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.unfamily.colossal_reactors.compat.jei.JeiNativeRecipeBridge;

public final class ColossalReiRecipeRegistrar {
    private ColossalReiRecipeRegistrar() {}

    public static void registerAll(DisplayRegistry registry) {
        // Static displays when data is already available during REI plugin reload.
        addStatic(registry);
        // Dynamic generators cover U/R and category browsing after datapack load.
        registerGenerator(registry, ColossalReiCategories.FUEL, JeiNativeRecipeBridge::fuels, ColossalReiDisplays::fuel);
        registerGenerator(registry, ColossalReiCategories.COOLANT, JeiNativeRecipeBridge::coolants, ColossalReiDisplays::coolant);
        registerGenerator(registry, ColossalReiCategories.HEAT_SINK, JeiNativeRecipeBridge::heatSinks, ColossalReiDisplays::heatSink);
        registerGenerator(registry, ColossalReiCategories.MELTER, JeiNativeRecipeBridge::melterRecipes, ColossalReiDisplays::melter);
        registerGenerator(registry, ColossalReiCategories.MELTER_HEAT, JeiNativeRecipeBridge::melterHeats, ColossalReiDisplays::melterHeat);
        registerGenerator(registry, ColossalReiCategories.HEATING_COIL, JeiNativeRecipeBridge::heatingCoils, ColossalReiDisplays::heatingCoil);
        registerGenerator(registry, ColossalReiCategories.ELEC_COIL, JeiNativeRecipeBridge::elecCoils, ColossalReiDisplays::elecCoil);
        registerGenerator(registry, ColossalReiCategories.TURBINE, JeiNativeRecipeBridge::turbineGeneration, ColossalReiDisplays::turbineGeneration);
    }

    /** Late fill for categories still empty after world/datapack ready. */
    public static void fillMissing(DisplayRegistry registry) {
        addStatic(registry);
    }

    private static void addStatic(DisplayRegistry registry) {
        addIfEmpty(registry, ColossalReiCategories.FUEL, JeiNativeRecipeBridge.fuels(), ColossalReiDisplays::fuel);
        addIfEmpty(registry, ColossalReiCategories.COOLANT, JeiNativeRecipeBridge.coolants(), ColossalReiDisplays::coolant);
        addIfEmpty(registry, ColossalReiCategories.HEAT_SINK, JeiNativeRecipeBridge.heatSinks(), ColossalReiDisplays::heatSink);
        addIfEmpty(registry, ColossalReiCategories.MELTER, JeiNativeRecipeBridge.melterRecipes(), ColossalReiDisplays::melter);
        addIfEmpty(registry, ColossalReiCategories.MELTER_HEAT, JeiNativeRecipeBridge.melterHeats(), ColossalReiDisplays::melterHeat);
        addIfEmpty(registry, ColossalReiCategories.HEATING_COIL, JeiNativeRecipeBridge.heatingCoils(), ColossalReiDisplays::heatingCoil);
        addIfEmpty(registry, ColossalReiCategories.ELEC_COIL, JeiNativeRecipeBridge.elecCoils(), ColossalReiDisplays::elecCoil);
        addIfEmpty(registry, ColossalReiCategories.TURBINE, JeiNativeRecipeBridge.turbineGeneration(), ColossalReiDisplays::turbineGeneration);
    }

    private static <T> void addIfEmpty(
            DisplayRegistry registry,
            CategoryIdentifier<ColossalReiDisplay> category,
            List<T> recipes,
            Function<T, ColossalReiDisplay> factory) {
        if (recipes.isEmpty() || !registry.get(category).isEmpty()) {
            return;
        }
        for (T recipe : recipes) {
            ColossalReiDisplay display = factory.apply(recipe);
            if (display != null) {
                registry.add(display);
            }
        }
    }

    private static <T> void registerGenerator(
            DisplayRegistry registry,
            CategoryIdentifier<ColossalReiDisplay> category,
            Supplier<List<T>> recipes,
            Function<T, ColossalReiDisplay> factory) {
        registry.registerDisplayGenerator(category, new DynamicDisplayGenerator<>() {
            private List<ColossalReiDisplay> build() {
                List<ColossalReiDisplay> out = new ArrayList<>();
                for (T recipe : recipes.get()) {
                    ColossalReiDisplay display = factory.apply(recipe);
                    if (display != null) {
                        out.add(display);
                    }
                }
                return out;
            }

            @Override
            public Optional<List<ColossalReiDisplay>> generate(ViewSearchBuilder builder) {
                List<ColossalReiDisplay> all = build();
                return all.isEmpty() ? Optional.empty() : Optional.of(all);
            }

            @Override
            public Optional<List<ColossalReiDisplay>> getRecipeFor(EntryStack<?> entry) {
                return filter(build(), entry, true);
            }

            @Override
            public Optional<List<ColossalReiDisplay>> getUsageFor(EntryStack<?> entry) {
                return filter(build(), entry, false);
            }
        });
    }

    private static Optional<List<ColossalReiDisplay>> filter(
            List<ColossalReiDisplay> displays, EntryStack<?> entry, boolean outputs) {
        List<ColossalReiDisplay> matched = new ArrayList<>();
        for (ColossalReiDisplay display : displays) {
            List<EntryIngredient> side = outputs ? display.getOutputEntries() : display.getInputEntries();
            if (contains(side, entry)) {
                matched.add(display);
            }
        }
        return matched.isEmpty() ? Optional.empty() : Optional.of(matched);
    }

    private static boolean contains(List<EntryIngredient> ingredients, EntryStack<?> entry) {
        for (EntryIngredient ingredient : ingredients) {
            for (EntryStack<?> stack : ingredient) {
                if (EntryStacks.equalsFuzzy(stack, entry)) {
                    return true;
                }
            }
        }
        return false;
    }
}
