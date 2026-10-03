package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipe;
import net.unfamily.colossal_reactors.compat.emi.recipe.EmiFuelRecipe;
import net.unfamily.colossal_reactors.compat.jei.JeiNativeRecipeBridge;

/** Registers all Colossal EMI recipes (same sources as JEI). */
public final class ColossalEmiRecipeRegistrar {
    private ColossalEmiRecipeRegistrar() {}

    public static void registerAll(EmiRegistry registry) {
        for (var def : JeiNativeRecipeBridge.fuels()) {
            add(registry, new EmiFuelRecipe(def));
        }
        for (var recipe : JeiNativeRecipeBridge.coolants()) {
            add(registry, ColossalEmiRecipes.coolant(recipe));
        }
        for (var def : JeiNativeRecipeBridge.heatSinks()) {
            add(registry, ColossalEmiRecipes.heatSink(def));
        }
        for (var recipe : JeiNativeRecipeBridge.melterRecipes()) {
            add(registry, ColossalEmiRecipes.melter(recipe));
        }
        for (var entry : JeiNativeRecipeBridge.melterHeats()) {
            add(registry, ColossalEmiRecipes.melterHeat(entry));
        }
        for (var recipe : JeiNativeRecipeBridge.heatingCoils()) {
            add(registry, ColossalEmiRecipes.heatingCoil(recipe));
        }
        for (var def : JeiNativeRecipeBridge.elecCoils()) {
            add(registry, ColossalEmiRecipes.elecCoil(def));
        }
        for (var recipe : JeiNativeRecipeBridge.turbineGeneration()) {
            add(registry, ColossalEmiRecipes.turbineGeneration(recipe));
        }
    }

    private static void add(EmiRegistry registry, EmiRecipe recipe) {
        if (recipe != null) {
            registry.addRecipe(recipe);
        }
    }
}
