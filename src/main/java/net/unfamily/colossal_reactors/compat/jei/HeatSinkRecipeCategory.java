package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HeatSinkRecipeCategory implements IRecipeCategory<HeatSinkDefinition> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_heat_sink");
    private static final int WIDTH = 180;
    private static final int HEIGHT = 52;

    public static final RecipeType<HeatSinkDefinition> RECIPE_TYPE = new RecipeType<>(UID, HeatSinkDefinition.class);

    private final IDrawable background;
    private final IDrawable icon;

    public HeatSinkRecipeCategory(IGuiHelper helper) {
        this.background = new JeiRecipeBackgroundDrawable(WIDTH, HEIGHT, false);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.REACTOR_GLASS.get()));
    }

    @Override
    public RecipeType<HeatSinkDefinition> getRecipeType() {
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
    public void setRecipe(IRecipeLayoutBuilder builder, HeatSinkDefinition recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();

        List<ItemStack> blocks = JeiIngredientsHelper.getBlockStacks(recipe.validBlocks(), registryAccess);
        List<ItemStack> liquidBuckets = JeiIngredientsHelper.getLiquidBuckets(recipe.validLiquids(), registryAccess);
        List<ItemStack> merged = new ArrayList<>(blocks);
        merged.addAll(liquidBuckets);
        if (!merged.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT,
                    JeiRecipeBackgroundDrawable.SLOT_IN_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                    JeiRecipeBackgroundDrawable.SLOT_IN_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y).addItemStacks(merged);
        }
    }

    @Override
    public void draw(HeatSinkDefinition recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        String fuelMult = formatMultiplier(recipe.fuelMultiplier());
        String energyMult = formatMultiplier(recipe.energyMultiplier());
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        guiGraphics.drawString(font, Component.translatable("jei.colossal_reactors.heat_sink.fuel_reduction", fuelMult), margin, textY, 0xFF404040, false);
        guiGraphics.drawString(font, Component.translatable("jei.colossal_reactors.heat_sink.rf_increment", energyMult), margin, textY + 10, 0xFF404040, false);
    }

    private static String formatMultiplier(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }
}
