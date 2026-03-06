package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.client.gui.MelterScreen;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilRegistry;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;

@JeiPlugin
public class ColossalReactorsJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "jei_plugin");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var helper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new CoolantRecipeCategory(helper),
                new FuelRecipeCategory(helper),
                new HeatSinkRecipeCategory(helper),
                new MelterRecipeCategory(helper),
                new HeatingCoilRecipeCategory(helper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Reactor data is loaded from datapacks via ReactorDataReloadListener (server and client).
        registration.addRecipes(CoolantRecipeCategory.RECIPE_TYPE, CoolantLoader.getAll().values().stream().toList());
        registration.addRecipes(FuelRecipeCategory.RECIPE_TYPE, FuelLoader.getAll().values().stream().toList());
        registration.addRecipes(HeatSinkRecipeCategory.RECIPE_TYPE, HeatSinkLoader.getAllDefinitions());
        registration.addRecipes(MelterRecipeCategory.RECIPE_TYPE, MelterRecipesLoader.getAll());

        var coilRecipes = HeatingCoilRegistry.getAll().values().stream()
                .flatMap(def -> {
                    var opts = def.consume();
                    if (opts == null || opts.isEmpty()) return java.util.stream.Stream.empty();
                    return java.util.stream.IntStream.range(0, opts.size())
                            .mapToObj(i -> new HeatingCoilJeiRecipe(def.id(), def.duration(), i, opts.get(i)));
                })
                .toList();
        registration.addRecipes(HeatingCoilRecipeCategory.RECIPE_TYPE, coilRecipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Click on Melter progress bar (77, 34, 24x16) opens JEI to Melter recipe category
        registration.addRecipeClickArea(MelterScreen.class, MelterScreen.getProgressBarX(), MelterScreen.getProgressBarY(),
                MelterScreen.getProgressBarWidth(), MelterScreen.getProgressBarHeight(), MelterRecipeCategory.RECIPE_TYPE);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ItemStack controller = new ItemStack(ModBlocks.REACTOR_CONTROLLER.get());
        registration.addRecipeCatalyst(controller, CoolantRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(controller, FuelRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(controller, HeatSinkRecipeCategory.RECIPE_TYPE);

        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MELTER.get()), MelterRecipeCategory.RECIPE_TYPE);

        for (var id : HeatingCoilRegistry.getAll().keySet()) {
            var off = ModBlocks.getHeatingCoilBlock(id, false);
            if (off != null) {
                registration.addRecipeCatalyst(new ItemStack(off), HeatingCoilRecipeCategory.RECIPE_TYPE);
            }
        }
    }
}
