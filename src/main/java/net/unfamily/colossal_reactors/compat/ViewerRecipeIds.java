package net.unfamily.colossal_reactors.compat;

import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;
import org.jetbrains.annotations.Nullable;

/**
 * Datapack {@link Identifier} for JEI {@code getRegistryName} / EMI / REI.
 * Collision suffixes ({@code liquid}, {@code gas}, option index) are only for unique EMI/REI cards.
 */
public final class ViewerRecipeIds {
    private ViewerRecipeIds() {}

    /** JEI copy-id: bare datapack id (no collision suffix). */
    public static @Nullable Identifier registryName(@Nullable Identifier recipeId) {
        return recipeId;
    }

    /**
     * Unique location for EMI/REI. Uses datapack id when present; otherwise synthetic fallback.
     * @param collisionSuffix optional path segment when one holder expands to multiple cards
     */
    public static Identifier displayLocation(
            @Nullable Identifier recipeId,
            String group,
            Identifier fallback,
            @Nullable String collisionSuffix) {
        if (recipeId != null) {
            if (collisionSuffix == null || collisionSuffix.isBlank()) {
                return recipeId;
            }
            String path = recipeId.getPath();
            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return Identifier.fromNamespaceAndPath(recipeId.getNamespace(), path + "/" + collisionSuffix);
        }
        return syntheticRecipeId(group, fallback);
    }

    public static Identifier fallbackId(String path) {
        return Identifier.fromNamespaceAndPath(ColossalReactors.MODID, path);
    }

    /** Synthetic id (leading '/') so EMI does not look it up in RecipeManager. */
    public static Identifier syntheticRecipeId(String group, Identifier id) {
        String ns = id != null ? id.getNamespace() : ColossalReactors.MODID;
        String base = id != null ? id.getPath() : "unknown";
        while (base.startsWith("/")) {
            base = base.substring(1);
        }
        String safeGroup = (group == null || group.isBlank()) ? "recipe" : group;
        return Identifier.fromNamespaceAndPath(ns, "/" + safeGroup + "/" + base);
    }
}
