package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TurbineGenerationRecipeCategory implements IRecipeCategory<TurbineJeiRecipe> {

    public static final Identifier UID = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "turbine_generation");
    public static final IRecipeType<TurbineJeiRecipe> RECIPE_TYPE = IRecipeType.create(UID, TurbineJeiRecipe.class);

    private static final int WIDTH = 180;
    private static final int HEIGHT = 54;

    private final IDrawable background;
    private final IDrawable icon;

    public TurbineGenerationRecipeCategory(IGuiHelper helper) {
        this.background = new JeiRecipeBackgroundDrawable(WIDTH, HEIGHT, true);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.TURBINE_CONTROLLER.get()));
    }

    @Override
    public IRecipeType<TurbineJeiRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public int getWidth() { return WIDTH; }

    @Override
    public int getHeight() { return HEIGHT; }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.colossal_reactors.turbine_generation");
    }

    @Override
    public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, TurbineJeiRecipe recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();
        TurbineGenerationDefinition def = recipe.definition();

        if (recipe.medium() == JeiMedium.LIQUID) {
            List<FluidStack> inputFluids = JeiIngredientsHelper.getTurbineGenerationInputFluids(recipe.inputSelectors(), registryAccess);
            if (!inputFluids.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.INPUT,
                        JeiRecipeBackgroundDrawable.SLOT_IN_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                        JeiRecipeBackgroundDrawable.SLOT_IN_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y)
                        .addIngredients(NeoForgeTypes.FLUID_STACK, inputFluids);
            }
            List<FluidStack> outputFluids = new ArrayList<>();
            for (String sel : recipe.outputSelectors()) {
                outputFluids.addAll(JeiIngredientsHelper.getOutputFluidStacks(sel, registryAccess));
            }
            if (!outputFluids.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.OUTPUT,
                        JeiRecipeBackgroundDrawable.SLOT_OUT_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                        JeiRecipeBackgroundDrawable.SLOT_OUT_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y)
                        .addIngredients(NeoForgeTypes.FLUID_STACK, outputFluids);
            }
        } else {
            JeiIngredientsHelper.addChemicalSlot(builder, RecipeIngredientRole.INPUT,
                    JeiRecipeBackgroundDrawable.SLOT_IN_X, JeiRecipeBackgroundDrawable.SLOT_IN_Y, recipe.inputSelectors());
            if (!recipe.outputSelectors().isEmpty()) {
                JeiIngredientsHelper.addChemicalSlot(builder, RecipeIngredientRole.OUTPUT,
                        JeiRecipeBackgroundDrawable.SLOT_OUT_X, JeiRecipeBackgroundDrawable.SLOT_OUT_Y, recipe.outputSelectors());
            } else {
                String liquidOut = def.liquidOutputSelector();
                if (liquidOut != null && !liquidOut.isBlank()) {
                    List<FluidStack> outputFluids = JeiIngredientsHelper.getOutputFluidStacks(liquidOut, registryAccess);
                    if (!outputFluids.isEmpty()) {
                        builder.addSlot(RecipeIngredientRole.OUTPUT,
                                JeiRecipeBackgroundDrawable.SLOT_OUT_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                                JeiRecipeBackgroundDrawable.SLOT_OUT_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y)
                                .addIngredients(NeoForgeTypes.FLUID_STACK, outputFluids);
                    }
                }
            }
        }
    }

    @Override
    public void draw(TurbineJeiRecipe recipe, IRecipeSlotsView view, GuiGraphicsExtractor g, double mouseX, double mouseY) {
        background.draw(g);
        TurbineGenerationDefinition def = recipe.definition();
        var font = Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        g.text(font, Component.translatable("jei.colossal_reactors.turbine_generation.rf_per_bucket",
                        TurbineGenerationLoader.formatRfPerSteamBucket(def.rfProduction())),
                margin, textY, color, false);
    }
}
