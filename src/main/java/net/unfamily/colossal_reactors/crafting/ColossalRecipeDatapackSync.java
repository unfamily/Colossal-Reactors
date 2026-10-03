package net.unfamily.colossal_reactors.crafting;

import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.unfamily.colossal_reactors.ColossalReactors;

@EventBusSubscriber(modid = ColossalReactors.MODID)
public final class ColossalRecipeDatapackSync {
    private ColossalRecipeDatapackSync() {}

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.sendRecipes(
                ModColossalRecipes.FUEL.get(),
                ModColossalRecipes.COOLANT.get(),
                ModColossalRecipes.HEAT_SINKS.get(),
                ModColossalRecipes.MELTER_RECIPES.get(),
                ModColossalRecipes.MELTER_HEATS.get(),
                ModColossalRecipes.RADIATION_SCRUBBER_CATALYSTS.get(),
                ModColossalRecipes.TURBINE_GENERATION.get(),
                ModColossalRecipes.ELEC_COILS.get(),
                ModColossalRecipes.HEATING_COILS.get());
    }

    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        ColossalRecipeData.applyFromRecipeMap(event.getRecipeMap());
    }
}
