package net.unfamily.colossal_reactors.crafting;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * Requests Colossal {@link RecipeType} sync to clients. Client cache of the synced
 * {@link RecipeMap} lives in {@link ColossalRecipeClientSync} so JEI/EMI/REI see the same
 * holder ids as KubeJS / RecipeManager on the server.
 */
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

    /** Delegates to client cache when present (no-op on dedicated server). */
    public static @Nullable RecipeMap clientRecipeMap() {
        return ColossalRecipeClientSync.clientRecipeMap();
    }

    public static List<RecipeHolder<ColossalJsonRecipe>> clientHolders(RecipeType<ColossalJsonRecipe> type) {
        return ColossalRecipeClientSync.clientHolders(type);
    }
}
