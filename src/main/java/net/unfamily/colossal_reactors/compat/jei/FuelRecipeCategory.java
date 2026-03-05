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
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FuelRecipeCategory implements IRecipeCategory<FuelDefinition> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_fuel");
    private static final int WIDTH = 180;
    private static final int HEIGHT = 52;

    public static final RecipeType<FuelDefinition> RECIPE_TYPE = new RecipeType<>(UID, FuelDefinition.class);

    private final IDrawable background;
    private final IDrawable icon;

    public FuelRecipeCategory(IGuiHelper helper) {
        this.background = new JeiRecipeBackgroundDrawable(WIDTH, HEIGHT, true);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.REACTOR_ROD.get()));
    }

    @Override
    public RecipeType<FuelDefinition> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.colossal_reactors.reactor_fuel");
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
    public void setRecipe(IRecipeLayoutBuilder builder, FuelDefinition recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();

        List<ItemStack> inputs = JeiIngredientsHelper.getFuelInputStacks(recipe.inputs(), registryAccess);
        List<ItemStack> outputs = JeiIngredientsHelper.getWasteOutputStacks(recipe.output(), registryAccess);
        if (!inputs.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT,
                    JeiRecipeBackgroundDrawable.SLOT_IN_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                    JeiRecipeBackgroundDrawable.SLOT_IN_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y).addItemStacks(inputs);
        }
        if (!outputs.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT,
                    JeiRecipeBackgroundDrawable.SLOT_OUT_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                    JeiRecipeBackgroundDrawable.SLOT_OUT_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y).addItemStacks(outputs);
        }
    }

    @Override
    public void draw(FuelDefinition recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int line2 = textY + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;

        int unitsPerFuel = recipe.unitsPerFuel();
        int unitsPerWaste = recipe.unitsPerWaste();
        String[] ratio = JeiIngredientsHelper.formatSimplifiedRatio(unitsPerFuel, unitsPerWaste);
        Component consumeFuel = Component.translatable("jei.colossal_reactors.consume_fuel", ratio[0]);
        Component produceWaste = Component.translatable("jei.colossal_reactors.produce_waste", ratio[1]);
        guiGraphics.drawString(font, consumeFuel, margin, textY, color, false);
        guiGraphics.drawString(font, produceWaste, margin, line2, color, false);
    }
}
