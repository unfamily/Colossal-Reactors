package net.unfamily.colossal_reactors.compat;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import org.jetbrains.annotations.Nullable;

/**
 * Datapack {@link ResourceLocation} for JEI {@code getRegistryName} / EMI / REI.
 * Collision suffixes ({@code liquid}, {@code gas}, option index) are only for unique EMI/REI cards.
 */
public final class ViewerRecipeIds {
    private ViewerRecipeIds() {}

    /** JEI copy-id: bare datapack id (no collision suffix). */
    public static @Nullable ResourceLocation registryName(@Nullable ResourceLocation recipeId) {
        return recipeId;
    }

    /**
     * Unique location for EMI/REI. Uses datapack id when present; otherwise synthetic fallback.
     * @param collisionSuffix optional path segment when one holder expands to multiple cards
     */
    public static ResourceLocation displayLocation(
            @Nullable ResourceLocation recipeId,
            String group,
            ResourceLocation fallback,
            @Nullable String collisionSuffix) {
        if (recipeId != null) {
            if (collisionSuffix == null || collisionSuffix.isBlank()) {
                return recipeId;
            }
            String path = recipeId.getPath();
            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return ResourceLocation.fromNamespaceAndPath(recipeId.getNamespace(), path + "/" + collisionSuffix);
        }
        return syntheticRecipeId(group, fallback);
    }

    public static ResourceLocation fallbackId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, path);
    }

    /** Synthetic id (leading '/') so EMI does not look it up in RecipeManager. */
    public static ResourceLocation syntheticRecipeId(String group, ResourceLocation id) {
        String ns = id != null ? id.getNamespace() : ColossalReactors.MODID;
        String base = id != null ? id.getPath() : "unknown";
        while (base.startsWith("/")) {
            base = base.substring(1);
        }
        String safeGroup = (group == null || group.isBlank()) ? "recipe" : group;
        return ResourceLocation.fromNamespaceAndPath(ns, "/" + safeGroup + "/" + base);
    }
}
