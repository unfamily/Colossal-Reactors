package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TurbineGenerationRecipeCategory implements IRecipeCategory<TurbineGenerationDefinition> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "turbine_generation");
    public static final RecipeType<TurbineGenerationDefinition> RECIPE_TYPE = new RecipeType<>(UID, TurbineGenerationDefinition.class);

    private static final int WIDTH = 180;
    private static final int HEIGHT = 54;

    private final IDrawable background;
    private final IDrawable icon;

    public TurbineGenerationRecipeCategory(IGuiHelper helper) {
        this.background = new JeiRecipeBackgroundDrawable(WIDTH, HEIGHT, true);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.TURBINE_CONTROLLER.get()));
    }

    @Override
    public RecipeType<TurbineGenerationDefinition> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.colossal_reactors.turbine_generation");
    }

    @Override
    public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public IDrawable getBackground() { return background; }

    @Override
    public boolean isHandled(TurbineGenerationDefinition recipe) {
        return TurbineGenerationLoader.isVisibleInJei(recipe);
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TurbineGenerationDefinition recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();

        List<FluidStack> inputFluids = JeiIngredientsHelper.getTurbineGenerationInputFluids(recipe.inputs(), registryAccess);
        List<FluidStack> outputFluids = JeiIngredientsHelper.getOutputFluidStacks(recipe.output(), registryAccess);

        if (!inputFluids.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT,
                    JeiRecipeBackgroundDrawable.SLOT_IN_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                    JeiRecipeBackgroundDrawable.SLOT_IN_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, inputFluids);
        }
        if (!outputFluids.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT,
                    JeiRecipeBackgroundDrawable.SLOT_OUT_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                    JeiRecipeBackgroundDrawable.SLOT_OUT_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, outputFluids);
        }
    }

    @Override
    public void draw(TurbineGenerationDefinition recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        g.drawString(font, Component.translatable("jei.colossal_reactors.turbine_generation.rf_per_bucket",
                        TurbineGenerationLoader.formatRfPerSteamBucket(recipe.rfProduction())),
                margin, textY, color, false);
    }
}
