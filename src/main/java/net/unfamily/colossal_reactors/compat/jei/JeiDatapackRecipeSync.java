package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.recipe.IRecipeManager;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.unfamily.colossal_reactors.melter.MelterHeatsLoader;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import net.unfamily.colossal_reactors.turbine.ElecCoilLoader;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * JEI registers before datapack entries are resolved with a live level.
 * Fills melter and turbine categories after world load. Never uses {@code hideRecipes}.
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
        if (FMLEnvironment.getDist() != Dist.CLIENT || runtime == null) {
            return;
        }
        if (Minecraft.getInstance().level == null) {
            return;
        }
        IRecipeManager recipeManager = runtime.getRecipeManager();
        addIfFewerThanExpected(recipeManager, MelterRecipeCategory.RECIPE_TYPE, MelterRecipesLoader.getAll());
        addIfFewerThanExpected(recipeManager, MelterHeatSourceRecipeCategory.RECIPE_TYPE, MelterHeatsLoader.getAll());
        addIfFewerThanExpected(recipeManager, ElecCoilRecipeCategory.RECIPE_TYPE, ElecCoilLoader.getVisibleDefinitions());
        addIfFewerThanExpected(recipeManager, TurbineGenerationRecipeCategory.RECIPE_TYPE, TurbineGenerationLoader.getVisibleDefinitions());
    }

    private static <T> void addIfFewerThanExpected(IRecipeManager recipeManager, IRecipeType<T> recipeType, List<T> recipes) {
        if (recipes.isEmpty()) {
            return;
        }
        long visible = recipeManager.createRecipeLookup(recipeType).get().count();
        if (visible < recipes.size()) {
            recipeManager.addRecipes(recipeType, recipes);
        }
    }
}
