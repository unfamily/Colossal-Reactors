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
import net.unfamily.colossal_reactors.turbine.ElecCoilDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ElecCoilRecipeCategory implements IRecipeCategory<ElecCoilDefinition> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "elec_coil");
    public static final RecipeType<ElecCoilDefinition> RECIPE_TYPE = new RecipeType<>(UID, ElecCoilDefinition.class);

    private static final int WIDTH = 180;
    private static final int HEIGHT = 54;

    private final IDrawable background;
    private final IDrawable icon;

    public ElecCoilRecipeCategory(IGuiHelper helper) {
        this.background = new JeiRecipeBackgroundDrawable(WIDTH, HEIGHT, false);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.TURBINE_CASING.get()));
    }

    @Override
    public RecipeType<ElecCoilDefinition> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.colossal_reactors.elec_coil");
    }

    @Override
    public @Nullable IDrawable getIcon() { return icon; }

    @Override
    public IDrawable getBackground() { return background; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ElecCoilDefinition recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();

        List<ItemStack> blocks = JeiIngredientsHelper.getElecCoilDisplayStacks(recipe.validBlocks(), registryAccess);
        if (!blocks.isEmpty()) {
            int slotX = JeiRecipeBackgroundDrawable.SLOT_IN_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X;
            int slotY = JeiRecipeBackgroundDrawable.SLOT_IN_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y;
            builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY).addItemStacks(blocks);
        }
    }

    @Override
    public void draw(ElecCoilDefinition recipe, IRecipeSlotsView view, GuiGraphics g, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        g.drawString(font, Component.translatable("jei.colossal_reactors.elec_coil.eff_coe", formatMultiplier(recipe.effCoe())),
                margin, textY, color, false);
        g.drawString(font, Component.translatable("jei.colossal_reactors.elec_coil.eff_max", formatMultiplier(recipe.effMax())),
                margin, textY + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT, color, false);
        g.drawString(font, Component.translatable("jei.colossal_reactors.elec_coil.eff_combined",
                        formatMultiplier(Math.min(recipe.effCoe(), recipe.effMax()))),
                margin, textY + 2 * JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT, color, false);
    }

    private static String formatMultiplier(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }
}
