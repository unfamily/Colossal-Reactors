package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.compat.emi.recipe.EmiFuelRecipe;
import net.unfamily.colossal_reactors.compat.jei.JeiNativeRecipeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Same data source as JEI ({@link JeiNativeRecipeBridge}). */
public final class ColossalEmiRecipeRegistrar {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColossalEmiRecipeRegistrar.class);

    private ColossalEmiRecipeRegistrar() {}

    public static void registerAll(EmiRegistry registry) {
        var rm = registry.getRecipeManager();
        int added = 0;
        added += registerEach(registry, "fuel", () -> JeiNativeRecipeBridge.fuels(rm), EmiFuelRecipe::new);
        added += registerEach(registry, "coolant", () -> JeiNativeRecipeBridge.coolants(rm), ColossalEmiRecipes::coolant);
        added += registerEach(registry, "heat_sink", () -> JeiNativeRecipeBridge.heatSinks(rm), ColossalEmiRecipes::heatSink);
        added += registerEach(registry, "melter", () -> JeiNativeRecipeBridge.melterRecipes(rm), ColossalEmiRecipes::melter);
        added += registerEach(registry, "melter_heat", () -> JeiNativeRecipeBridge.melterHeats(rm), ColossalEmiRecipes::melterHeat);
        added += registerEach(registry, "heating_coil", () -> JeiNativeRecipeBridge.heatingCoils(rm), ColossalEmiRecipes::heatingCoil);
        added += registerEach(registry, "elec_coil", () -> JeiNativeRecipeBridge.elecCoils(rm), ColossalEmiRecipes::elecCoil);
        added += registerEach(
                registry,
                "turbine_generation",
                () -> JeiNativeRecipeBridge.turbineGeneration(rm),
                ColossalEmiRecipes::turbineGeneration);
        LOGGER.info("EMI Colossal recipes registered: {}", added);
    }

    private static <T> int registerEach(
            EmiRegistry registry, String group, Supplier<Iterable<T>> source, RecipeFactory<T> factory) {
        int n = 0;
        // Skip duplicate RecipeManager ids (safe if a source ever emits the same card twice).
        Set<ResourceLocation> seenIds = new HashSet<>();
        try {
            for (T value : source.get()) {
                try {
                    n += add(registry, factory.create(value), seenIds);
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

    private static int add(EmiRegistry registry, EmiRecipe recipe, Set<ResourceLocation> seenIds) {
        if (recipe == null) {
            return 0;
        }
        ResourceLocation id = recipe.getId();
        if (id != null && !seenIds.add(id)) {
            LOGGER.debug("EMI Colossal skip (duplicate id): {}", id);
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
