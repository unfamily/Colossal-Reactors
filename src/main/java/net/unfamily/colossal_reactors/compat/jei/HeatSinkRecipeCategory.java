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
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import org.jetbrains.annotations.Nullable;
import net.unfamily.colossal_reactors.compat.ViewerRecipeIds;

import java.util.List;
import net.unfamily.colossal_reactors.compat.RecipeViewerIds;

public class HeatSinkRecipeCategory implements IRecipeCategory<HeatSinkJeiRecipe> {

    public static final ResourceLocation UID = RecipeViewerIds.REACTOR_HEAT_SINK;
    private static final int WIDTH = 180;
    /** Slots + three text lines */
    private static final int HEIGHT = 54;

    public static final RecipeType<HeatSinkJeiRecipe> RECIPE_TYPE = new RecipeType<>(UID, HeatSinkJeiRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public HeatSinkRecipeCategory(IGuiHelper helper) {
        this.background = new JeiRecipeBackgroundDrawable(WIDTH, HEIGHT, false);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.REACTOR_GLASS.get()));
    }

    @Override
    public RecipeType<HeatSinkJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.colossal_reactors.reactor_heat_sink");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HeatSinkJeiRecipe recipe, IFocusGroup focuses) {
        HeatSinkDefinition def = recipe.definition();
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();

        int slotX = JeiRecipeBackgroundDrawable.SLOT_IN_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X;
        int slotY = JeiRecipeBackgroundDrawable.SLOT_IN_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y;

        List<ItemStack> blocks = JeiIngredientsHelper.getBlockStacks(def.validBlocks(), registryAccess);
        List<FluidStack> liquidFluids = JeiIngredientsHelper.getLiquidFluidStacks(def.validLiquids(), registryAccess);

        if (!blocks.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY).addItemStacks(blocks);
        } else if (!liquidFluids.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY).addIngredients(NeoForgeTypes.FLUID_STACK, liquidFluids);
        }
    }

    @Override
    public void draw(HeatSinkJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        HeatSinkDefinition def = recipe.definition();
        var font = Minecraft.getInstance().font;
        String fuelMult = formatMultiplier(def.fuelMultiplier());
        String energyMult = formatMultiplier(def.energyMultiplier());
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        String heatMult = formatMultiplier(def.overheatingMultiplier());
        guiGraphics.drawString(font, Component.translatable("jei.colossal_reactors.heat_sink.fuel_reduction", fuelMult), margin, textY, 0xFF404040, false);
        guiGraphics.drawString(font, Component.translatable("jei.colossal_reactors.heat_sink.rf_increment", energyMult), margin, textY + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT, 0xFF404040, false);
        guiGraphics.drawString(font, Component.translatable("jei.colossal_reactors.heat_sink.heat_reduction", heatMult), margin,
                textY + 2 * JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT, 0xFF404040, false);
    }

    private static String formatMultiplier(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }

    @Override
    public @Nullable ResourceLocation getRegistryName(HeatSinkJeiRecipe recipe) {
        return ViewerRecipeIds.registryName(recipe.recipeId());
    }
}
