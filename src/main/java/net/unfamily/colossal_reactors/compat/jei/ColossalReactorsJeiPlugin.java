package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;

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
                new HeatSinkRecipeCategory(helper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // On dedicated server the client never runs ServerStartingEvent, so loaders may be empty. Populate for JEI.
        if (CoolantLoader.getAll().isEmpty()) {
            CoolantLoader.scanConfigDirectory();
        }
        if (FuelLoader.getAll().isEmpty()) {
            FuelLoader.scanConfigDirectory();
        }
        if (HeatSinkLoader.getAllDefinitions().isEmpty()) {
            HeatSinkLoader.scanConfigDirectory();
        }
        registration.addRecipes(CoolantRecipeCategory.RECIPE_TYPE, CoolantLoader.getAll().values().stream().toList());
        registration.addRecipes(FuelRecipeCategory.RECIPE_TYPE, FuelLoader.getAll().values().stream().toList());
        registration.addRecipes(HeatSinkRecipeCategory.RECIPE_TYPE, HeatSinkLoader.getAllDefinitions());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        ItemStack controller = new ItemStack(ModBlocks.REACTOR_CONTROLLER.get());
        registration.addRecipeCatalyst(controller, CoolantRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(controller, FuelRecipeCategory.RECIPE_TYPE);
        registration.addRecipeCatalyst(controller, HeatSinkRecipeCategory.RECIPE_TYPE);
    }
}
