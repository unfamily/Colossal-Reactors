package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.compat.emi.recipe.EmiFuelRecipe;
import net.unfamily.colossal_reactors.compat.jei.JeiNativeRecipeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Same data source as JEI ({@link JeiNativeRecipeBridge}). */
public final class ColossalEmiRecipeRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColossalEmiRecipeRegistrar.class);

    private ColossalEmiRecipeRegistrar() {}

    public static void registerAll(EmiRegistry registry) {
        int added = 0;
        added += registerEach(registry, "fuel", () -> JeiNativeRecipeBridge.fuels(), EmiFuelRecipe::new);
        added += registerEach(registry, "coolant", JeiNativeRecipeBridge::coolants, ColossalEmiRecipes::coolant);
        added += registerEach(registry, "heat_sink", JeiNativeRecipeBridge::heatSinks, ColossalEmiRecipes::heatSink);
        added += registerEach(registry, "melter", JeiNativeRecipeBridge::melterRecipes, ColossalEmiRecipes::melter);
        added += registerEach(registry, "melter_heat", JeiNativeRecipeBridge::melterHeats, ColossalEmiRecipes::melterHeat);
        added += registerEach(registry, "heating_coil", JeiNativeRecipeBridge::heatingCoils, ColossalEmiRecipes::heatingCoil);
        added += registerEach(registry, "elec_coil", JeiNativeRecipeBridge::elecCoils, ColossalEmiRecipes::elecCoil);
        added += registerEach(
                registry, "turbine_generation", JeiNativeRecipeBridge::turbineGeneration, ColossalEmiRecipes::turbineGeneration);
        LOGGER.info("EMI Colossal recipes registered: {}", added);
    }

    private static <T> int registerEach(
            EmiRegistry registry, String group, Supplier<Iterable<T>> source, RecipeFactory<T> factory) {
        int n = 0;
        Set<Identifier> seen = new HashSet<>();
        try {
            for (T value : source.get()) {
                try {
                    n += add(registry, factory.create(value), seen);
                } catch (Throwable t) {
                    LOGGER.error("EMI Colossal recipe failed in group '{}'", group, t);
                }
            }
        } catch (Throwable t) {
            LOGGER.error("EMI Colossal group '{}' enumeration failed", group, t);
        }
        if (n == 0) {
            LOGGER.warn("EMI Colossal group '{}' registered 0 recipes", group);
        } else {
            LOGGER.info("EMI Colossal group '{}': {} recipes", group, n);
        }
        return n;
    }

    private static int add(EmiRegistry registry, EmiRecipe recipe, Set<Identifier> seen) {
        if (recipe == null) {
            return 0;
        }
        Identifier id = recipe.getId();
        if (id != null && !seen.add(id)) {
            LOGGER.debug("EMI Colossal skip duplicate id: {}", id);
            return 0;
        }
        // Energy-only coils: catalysts + output, no consume inputs. Skip only totally empty cards.
        boolean hasInputs = !recipe.getInputs().isEmpty();
        boolean hasCatalysts = !recipe.getCatalysts().isEmpty();
        boolean hasOutputs = !recipe.getOutputs().isEmpty();
        if (!hasInputs && !hasCatalysts) {
            LOGGER.debug("EMI Colossal skip (no inputs/catalysts): {}", id);
            return 0;
        }
        if (!hasInputs && !hasOutputs) {
            LOGGER.debug("EMI Colossal skip (catalyst-only, no output): {}", id);
            return 0;
        }
        registry.addRecipe(recipe);
        return 1;
    }

    @FunctionalInterface
    private interface RecipeFactory<T> {
        EmiRecipe create(T value);
    }
}
