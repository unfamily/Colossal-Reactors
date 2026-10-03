package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Converts JEI-style stacks into EMI ingredients. */
public final class EmiStackHelper {
    private EmiStackHelper() {}

    public static EmiIngredient ingredientOf(ItemStack stack) {
        return EmiStack.of(stack);
    }

    public static EmiIngredient ingredientOf(List<ItemStack> stacks) {
        if (stacks.isEmpty()) {
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
        return fluid.isEmpty() ? EmiStack.EMPTY : EmiStack.of(fluid.getFluid(), fluid.getAmount());
    }

    public static EmiIngredient ingredientOfFluids(List<FluidStack> fluids) {
        if (fluids.isEmpty()) {
            return EmiStack.EMPTY;
        }
        List<EmiStack> emi = new ArrayList<>(fluids.size());
        for (FluidStack fluid : fluids) {
            if (!fluid.isEmpty()) {
                emi.add(EmiStack.of(fluid.getFluid(), fluid.getAmount()));
            }
        }
        return emi.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(emi);
    }

    public static EmiStack outputOf(ItemStack stack) {
        return stack.isEmpty() ? EmiStack.EMPTY : EmiStack.of(stack);
    }

    public static EmiStack outputOf(FluidStack fluid) {
        return fluid.isEmpty() ? EmiStack.EMPTY : EmiStack.of(fluid.getFluid(), fluid.getAmount());
    }

    public static RegistryAccess registryOrThrow() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc.level == null) {
            throw new IllegalStateException("Level required for recipe viewer stacks");
        }
        return mc.level.registryAccess();
    }

    /** Mek chemical selectors; empty when Mek/EMI chemical bridge is unavailable. */
    public static EmiIngredient ingredientOfChemicalSelectors(List<String> selectors) {
        List<Object> stacks = net.unfamily.colossal_reactors.compat.jei.JeiIngredientsHelper.getChemicalStacks(selectors);
        if (stacks.isEmpty()) {
            return EmiStack.EMPTY;
        }
        List<EmiStack> emi = new ArrayList<>();
        for (Object stack : stacks) {
            if (stack instanceof ItemStack item && !item.isEmpty()) {
                emi.add(EmiStack.of(item));
            }
        }
        return emi.isEmpty() ? EmiStack.EMPTY : EmiIngredient.of(emi);
    }
}
