package net.unfamily.colossal_reactors.compat.jei;

import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.RecipeIngredientRole;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.compat.RecipeViewerLayout;

/** JEI-only Mek chemical slot helpers (not safe to load without JEI). */
public final class JeiChemicalSlots {
    private JeiChemicalSlots() {}

    /** Mek {@code MekanismJEI#TYPE_CHEMICAL}. */
    @SuppressWarnings("unchecked")
    public static <T> IIngredientType<T> getMekChemicalIngredientType() {
        if (!JeiIngredientsHelper.jeiChemicalsAvailable()) {
            return null;
        }
        try {
            Class<?> cls = Class.forName("mekanism.client.recipe_viewer.jei.MekanismJEI");
            return (IIngredientType<T>) cls.getField("TYPE_CHEMICAL").get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static void addChemicalSlot(
            IRecipeLayoutBuilder builder,
            RecipeIngredientRole role,
            int slotX,
            int slotY,
            List<String> chemicalSelectors) {
        if (chemicalSelectors.isEmpty()) {
            return;
        }
        IIngredientType<Object> type = getMekChemicalIngredientType();
        List<Object> stacks = JeiIngredientsHelper.getChemicalStacks(chemicalSelectors);
        if (type == null || stacks.isEmpty()) {
            return;
        }
        builder.addSlot(role, slotX + RecipeViewerLayout.ITEM_OFFSET_X, slotY + RecipeViewerLayout.ITEM_OFFSET_Y)
                .addIngredients(type, stacks);
    }

    /**
     * One JEI slot that cycles fluids and Mek chemicals together (same RecipeManager card).
     * @return true if at least one ingredient was added
     */
    public static boolean addFluidAndChemicalSlot(
            IRecipeLayoutBuilder builder,
            RecipeIngredientRole role,
            int slotX,
            int slotY,
            List<FluidStack> fluids,
            List<String> chemicalSelectors) {
        boolean hasFluids = fluids != null && !fluids.isEmpty();
        IIngredientType<Object> chemType = getMekChemicalIngredientType();
        List<Object> chemStacks = chemicalSelectors == null || chemicalSelectors.isEmpty()
                ? List.of()
                : JeiIngredientsHelper.getChemicalStacks(chemicalSelectors);
        boolean hasChem = chemType != null && !chemStacks.isEmpty();
        if (!hasFluids && !hasChem) {
            return false;
        }
        IRecipeSlotBuilder slot = builder.addSlot(
                role,
                slotX + RecipeViewerLayout.ITEM_OFFSET_X,
                slotY + RecipeViewerLayout.ITEM_OFFSET_Y);
        if (hasFluids) {
            slot.addIngredients(NeoForgeTypes.FLUID_STACK, fluids);
        }
        if (hasChem) {
            slot.addIngredients(chemType, chemStacks);
        }
        return true;
    }
}
