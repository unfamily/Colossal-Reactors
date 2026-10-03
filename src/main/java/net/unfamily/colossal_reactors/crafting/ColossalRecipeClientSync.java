package net.unfamily.colossal_reactors.crafting;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RecipesReceivedEvent;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * Client-only: keeps the synced {@link RecipeMap} so viewers use the same recipe ids as
 * RecipeManager / KubeJS on the server.
 */
@EventBusSubscriber(modid = ColossalReactors.MODID, value = Dist.CLIENT)
public final class ColossalRecipeClientSync {
    private static volatile @Nullable RecipeMap clientRecipeMap;

    private ColossalRecipeClientSync() {}

    @SubscribeEvent
    public static void onRecipesReceived(RecipesReceivedEvent event) {
        clientRecipeMap = event.getRecipeMap();
        ColossalRecipeData.applyFromRecipeMap(event.getRecipeMap());
    }

    @SubscribeEvent
    public static void onClientLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        clientRecipeMap = null;
    }

    static @Nullable RecipeMap clientRecipeMap() {
        return clientRecipeMap;
    }

    static List<RecipeHolder<ColossalJsonRecipe>> clientHolders(RecipeType<ColossalJsonRecipe> type) {
        RecipeMap map = clientRecipeMap;
        if (map == null) {
            return List.of();
        }
        return List.copyOf(map.byType(type));
    }
}
