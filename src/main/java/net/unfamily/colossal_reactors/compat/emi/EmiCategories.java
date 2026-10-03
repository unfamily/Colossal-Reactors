package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.compat.RecipeViewerIds;

/** EMI categories aligned with shared {@link RecipeViewerIds}. */
public final class EmiCategories {
    public static final EmiRecipeCategory COOLANT =
            category(RecipeViewerIds.REACTOR_COOLANT, ModBlocks.RESOURCE_PORT.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory FUEL =
            category(RecipeViewerIds.REACTOR_FUEL, ModBlocks.REACTOR_ROD.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory HEAT_SINK =
            category(RecipeViewerIds.REACTOR_HEAT_SINK, ModBlocks.REACTOR_GLASS.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory MELTER =
            category(RecipeViewerIds.MELTER, ModBlocks.MELTER.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory MELTER_HEAT =
            category(RecipeViewerIds.MELTER_HEAT_SOURCE, ModBlocks.MELTER.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory HEATING_COIL =
            category(RecipeViewerIds.HEATING_COIL, ModBlocks.MELTER.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory ELEC_COIL =
            category(RecipeViewerIds.ELEC_COIL, ModBlocks.TURBINE_CASING.get().asItem().getDefaultInstance());
    public static final EmiRecipeCategory TURBINE_GENERATION =
            category(RecipeViewerIds.TURBINE_GENERATION, ModBlocks.TURBINE_CONTROLLER.get().asItem().getDefaultInstance());

    public static final EmiRecipeCategory[] ALL = {
        COOLANT, FUEL, HEAT_SINK, MELTER, MELTER_HEAT, HEATING_COIL, ELEC_COIL, TURBINE_GENERATION
    };

    private EmiCategories() {}

    private static EmiRecipeCategory category(ResourceLocation id, ItemStack icon) {
        return new EmiRecipeCategory(id, EmiStack.of(icon));
    }
}
