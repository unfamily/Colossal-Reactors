package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.compat.jei.JeiIngredientsHelper;

/** Converts JEI-style stacks into EMI ingredients. */
public final class EmiStackHelper {
    private EmiStackHelper() {}

    /**
     * EMI synthetic recipe ids must start with '/' so they are not looked up in RecipeManager.
     * Group prefix avoids collisions when coolant/turbine/fuel share the same base path (e.g. water).
     */
    public static ResourceLocation syntheticRecipeId(String group, ResourceLocation id) {
        return net.unfamily.colossal_reactors.compat.ViewerRecipeIds.syntheticRecipeId(group, id);
    }

    public static EmiIngredient ingredientOf(ItemStack stack) {
        return stack == null || stack.isEmpty() ? EmiStack.EMPTY : EmiStack.of(stack);
    }

    public static EmiIngredient ingredientOf(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return EmiStack.EMPTY;
        }
        List<EmiStack> emi = new ArrayList<>(stacks.size());
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                emi.add(EmiStack.of(stack));
            }
        }
        return emi.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(emi);
    }

    public static EmiIngredient ingredientOf(FluidStack fluid) {
        return fluid == null || fluid.isEmpty() ? EmiStack.EMPTY : EmiStack.of(fluid.getFluid(), fluid.getAmount());
    }

    public static EmiIngredient ingredientOfFluids(List<FluidStack> fluids) {
        if (fluids == null || fluids.isEmpty()) {
            return EmiStack.EMPTY;
        }
        List<EmiStack> emi = new ArrayList<>(fluids.size());
        for (FluidStack fluid : fluids) {
            if (fluid.isEmpty()) {
                continue;
            }
            // Skip flowing variants so water/steam tags do not show as duplicate nearly-identical stacks.
            if (!fluid.getFluid().defaultFluidState().isSource()) {
                continue;
            }
            emi.add(EmiStack.of(fluid.getFluid(), fluid.getAmount()));
        }
        return emi.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(emi);
    }

    public static EmiStack outputOf(ItemStack stack) {
        return stack == null || stack.isEmpty() ? EmiStack.EMPTY : EmiStack.of(stack);
    }

    public static EmiStack outputOf(FluidStack fluid) {
        return fluid == null || fluid.isEmpty() ? EmiStack.EMPTY : EmiStack.of(fluid.getFluid(), fluid.getAmount());
    }

    public static RegistryAccess registryOrThrow() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.registryAccess();
        }
        if (mc.getConnection() != null) {
            return mc.getConnection().registryAccess();
        }
        throw new IllegalStateException("RegistryAccess required for recipe viewer stacks");
    }

    /**
     * Mek chemical selectors via Mekanism's ChemicalEmiStack bridge.
     * Returns EMPTY when Mek/EMI chemical support is unavailable (never fake empty item slots).
     */
    public static EmiIngredient ingredientOfChemicalSelectors(List<String> selectors) {
        if (selectors == null || selectors.isEmpty() || !JeiIngredientsHelper.jeiChemicalsAvailable()) {
            return EmiStack.EMPTY;
        }
        List<Object> stacks = JeiIngredientsHelper.getChemicalStacks(selectors);
        if (stacks.isEmpty()) {
            return EmiStack.EMPTY;
        }
        List<EmiStack> emi = new ArrayList<>(stacks.size());
        for (Object stack : stacks) {
            EmiStack converted = chemicalToEmiStack(stack);
            if (converted != null && !converted.isEmpty()) {
                emi.add(converted);
            }
        }
        return emi.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(emi);
    }

    /** Reflective call into MekanismEmiHelper — optional client-only bridge. */
    private static EmiStack chemicalToEmiStack(Object chemicalStack) {
        if (chemicalStack == null) {
            return EmiStack.EMPTY;
        }
        if (chemicalStack instanceof ItemStack item) {
            return item.isEmpty() ? EmiStack.EMPTY : EmiStack.of(item);
        }
        try {
            Class<?> helperClass = Class.forName("mekanism.client.recipe_viewer.emi.MekanismEmiHelper");
            Object helper = helperClass.getField("INSTANCE").get(null);
            Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
            if (!stackClass.isInstance(chemicalStack)) {
                return EmiStack.EMPTY;
            }
            Object result = helperClass.getMethod("createEmiStack", stackClass).invoke(helper, chemicalStack);
            return result instanceof EmiStack emiStack ? emiStack : EmiStack.EMPTY;
        } catch (Throwable ignored) {
            return EmiStack.EMPTY;
        }
    }
}
