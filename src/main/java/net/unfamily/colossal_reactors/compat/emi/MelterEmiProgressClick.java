package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.EmiApi;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ModBlocks;

/** Opens Melter workstation recipes in EMI (melter + melter heat). */
public final class MelterEmiProgressClick {
    private MelterEmiProgressClick() {}

    public static void openMelterRecipes() {
        EmiApi.displayUses(EmiStack.of(new ItemStack(ModBlocks.MELTER.get())));
    }
}
