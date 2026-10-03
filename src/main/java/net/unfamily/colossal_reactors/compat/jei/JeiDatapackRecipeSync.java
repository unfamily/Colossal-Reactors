package net.unfamily.colossal_reactors.compat.jei;

import java.util.List;
import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import org.jetbrains.annotations.Nullable;

/**
 * JEI registers before datapack entries are resolved with a live level.
 * Fills melter, turbine, and heating coil categories after world load using native holder ids.
 */
public final class JeiDatapackRecipeSync {

    @Nullable
    private static IJeiRuntime runtime;

    private JeiDatapackRecipeSync() {}

    public static void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;
        syncWhenWorldReady();
    }

    /** Call after datapack loaders were rebuilt with a live level (tags bound). */
    public static void syncWhenWorldReady() {
        if (FMLEnvironment.dist != Dist.CLIENT || runtime == null) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            return;
        }
        IRecipeManager recipeManager = runtime.getRecipeManager();
        addIfFewerThanExpected(recipeManager, MelterRecipeCategory.RECIPE_TYPE, JeiNativeRecipeBridge.melterRecipes());
        addIfFewerThanExpected(
                recipeManager, MelterHeatSourceRecipeCategory.RECIPE_TYPE, JeiNativeRecipeBridge.melterHeats());
        addIfFewerThanExpected(recipeManager, ElecCoilRecipeCategory.RECIPE_TYPE, JeiNativeRecipeBridge.elecCoils());
        addIfFewerThanExpected(
                recipeManager, TurbineGenerationRecipeCategory.RECIPE_TYPE, JeiNativeRecipeBridge.turbineGeneration());
        addIfFewerThanExpected(
                recipeManager, HeatingCoilRecipeCategory.RECIPE_TYPE, JeiNativeRecipeBridge.heatingCoils());
        addIfFewerThanExpected(recipeManager, FuelRecipeCategory.RECIPE_TYPE, JeiNativeRecipeBridge.fuels());
        addIfFewerThanExpected(recipeManager, CoolantRecipeCategory.RECIPE_TYPE, JeiNativeRecipeBridge.coolants());
        addIfFewerThanExpected(recipeManager, HeatSinkRecipeCategory.RECIPE_TYPE, JeiNativeRecipeBridge.heatSinks());
    }

    private static <T> void addIfFewerThanExpected(
            IRecipeManager recipeManager, RecipeType<T> recipeType, List<T> recipes) {
        if (recipes.isEmpty()) {
            return;
        }
        long visible = recipeManager.createRecipeLookup(recipeType).get().count();
        if (visible < recipes.size()) {
            recipeManager.addRecipes(recipeType, recipes);
        }
    }
}
