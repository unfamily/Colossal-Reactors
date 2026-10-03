package net.unfamily.colossal_reactors.compat.emi;

import java.util.Set;
import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.compat.HeatingCoilViewerHelper;
import net.unfamily.colossal_reactors.compat.RecipeViewerVisibility;

/**
 * EMI plugin (iskandert_utilities style: {@link EmiEntrypoint} + RecipeManager at register time).
 */
@EmiEntrypoint
public class ColossalEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        for (EmiRecipeCategory category : EmiCategories.ALL) {
            registry.addCategory(category);
        }
        registry.addWorkstation(EmiCategories.COOLANT, EmiStack.of(new ItemStack(ModBlocks.REACTOR_CONTROLLER.get())));
        registry.addWorkstation(EmiCategories.FUEL, EmiStack.of(new ItemStack(ModBlocks.REACTOR_CONTROLLER.get())));
        registry.addWorkstation(EmiCategories.HEAT_SINK, EmiStack.of(new ItemStack(ModBlocks.REACTOR_CONTROLLER.get())));
        registry.addWorkstation(EmiCategories.MELTER, EmiStack.of(new ItemStack(ModBlocks.MELTER.get())));
        registry.addWorkstation(EmiCategories.MELTER_HEAT, EmiStack.of(new ItemStack(ModBlocks.MELTER.get())));
        for (ItemStack coil : HeatingCoilViewerHelper.offCoilStacks()) {
            registry.addWorkstation(EmiCategories.HEATING_COIL, EmiStack.of(coil));
        }
        registry.addWorkstation(EmiCategories.ELEC_COIL, EmiStack.of(new ItemStack(ModBlocks.TURBINE_CONTROLLER.get())));
        registry.addWorkstation(EmiCategories.TURBINE_GENERATION, EmiStack.of(new ItemStack(ModBlocks.TURBINE_CONTROLLER.get())));

        Set<Item> visible = RecipeViewerVisibility.visibleItems();
        registry.removeEmiStacks(stack -> {
            Identifier id = stack.getId();
            if (id == null || !ColossalReactors.MODID.equals(id.getNamespace())) {
                return false;
            }
            ItemStack itemStack = stack.getItemStack();
            return !itemStack.isEmpty() && !visible.contains(itemStack.getItem());
        });

        // Same as Utilities: add from RecipeManager while EMI is registering.
        ColossalEmiRecipeRegistrar.registerAll(registry);
        EmiDatapackRecipeSync.onRegistryAvailable(registry);
    }
}
