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
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MelterHeatSourceRecipeCategory implements IRecipeCategory<MelterHeatEntry> {

    public static final Identifier UID = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "melter_heat_source");
    private static final int WIDTH = 180;
    private static final int HEIGHT = 52;

    public static final IRecipeType<MelterHeatEntry> RECIPE_TYPE = IRecipeType.create(UID, MelterHeatEntry.class);

    private final IDrawable background;
    private final IDrawable icon;

    public MelterHeatSourceRecipeCategory(IGuiHelper helper) {
        this.background = new JeiRecipeBackgroundDrawable(WIDTH, HEIGHT, false);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MELTER.get()));
    }

    @Override
    public IRecipeType<MelterHeatEntry> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.colossal_reactors.melter_heat_source");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MelterHeatEntry recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();

        int slotX = JeiRecipeBackgroundDrawable.SLOT_IN_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X;
        int slotY = JeiRecipeBackgroundDrawable.SLOT_IN_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y;

        List<ItemStack> blocks = JeiIngredientsHelper.getBlockStacksFromMelterEntry(recipe, registryAccess);
        List<FluidStack> fluids = JeiIngredientsHelper.getFluidStacksFromMelterEntry(recipe, registryAccess);

        if (!blocks.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT, slotX, slotY).addItemStacks(blocks);
        }
        if (!fluids.isEmpty()) {
            int fluidSlotX = blocks.isEmpty() ? slotX : slotX + JeiRecipeBackgroundDrawable.SLOT_SIZE + 4;
            builder.addSlot(RecipeIngredientRole.INPUT, fluidSlotX, slotY).addIngredients(NeoForgeTypes.FLUID_STACK, fluids);
        }
    }

    @Override
    public void draw(MelterHeatEntry recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
        var font = Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;

        String factorStr = formatFactor(recipe.factor());
        guiGraphics.text(font, Component.translatable("jei.colossal_reactors.melter_heat_source.factor", factorStr), margin, textY, color, false);
        if (recipe.notValid()) {
            guiGraphics.text(font, Component.translatable("jei.colossal_reactors.melter_heat_source.not_valid"), margin, textY + 10, 0xFF808080, false);
        }
    }

    private static String formatFactor(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        if (value < 0.01) return String.format("%.3f", value);
        return String.format("%.2f", value);
    }
}
