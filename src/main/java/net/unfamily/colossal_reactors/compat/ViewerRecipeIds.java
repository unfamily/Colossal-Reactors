package net.unfamily.colossal_reactors.compat;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import org.jetbrains.annotations.Nullable;

/**
 * Datapack {@link ResourceLocation} for JEI {@code getRegistryName} / EMI / REI / KubeJS.
 *
 * <p>Every Colossal RecipeManager id (Library progressive {@code path_N}, or heating-coil
 * {@code heating_coils_<coil>_<i>}) must be shown as-is in JEI, EMI and REI. Do not invent
 * {@code /gas}, {@code /liquid}, or other fake path segments — those are not RecipeManager keys.
 *
 * <p>Collision suffixes apply only to synthetic fallbacks when {@code recipeId} is null.
 */
public final class ViewerRecipeIds {
    private ViewerRecipeIds() {}

    /** JEI copy-id: bare datapack id. */
    public static @Nullable ResourceLocation registryName(@Nullable ResourceLocation recipeId) {
        return recipeId;
    }

    /**
     * Location for EMI/REI. Bare progressive holder id when present.
     * @param collisionSuffix only used for synthetic fallbacks (no RecipeManager id)
     */
    public static ResourceLocation displayLocation(
            @Nullable ResourceLocation recipeId,
            String group,
            ResourceLocation fallback,
            @Nullable String collisionSuffix) {
        if (recipeId != null) {
            return recipeId;
        }
        if (collisionSuffix != null && !collisionSuffix.isBlank() && fallback != null) {
            String path = fallback.getPath();
            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }
            return syntheticRecipeId(
                    group, ResourceLocation.fromNamespaceAndPath(fallback.getNamespace(), path + "_" + collisionSuffix));
        }
        return syntheticRecipeId(group, fallback);
    }

    public static ResourceLocation fallbackId(String path) {
        return ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, path);
    }

    /**
     * Heating-coil id shared by RecipeManager / KubeJS / JEI / EMI / REI:
     * {@code ns:heating_coils_coilPath_optionIndex}
     * e.g. {@code colossal_reactors:heating_coils_heating_coil_energy_0}.
     * Prefer {@code recipeId} when already set by the Library heating-coil split.
     */
    public static ResourceLocation heatingCoilDisplayLocation(
            @Nullable ResourceLocation recipeId, ResourceLocation coilId, int optionIndex) {
        if (recipeId != null) {
            return recipeId;
        }
        String ns = coilId != null ? coilId.getNamespace() : ColossalReactors.MODID;
        String coilPath = coilId != null ? coilId.getPath() : "unknown";
        while (coilPath.startsWith("/")) {
            coilPath = coilPath.substring(1);
        }
        coilPath = coilPath.replace('/', '_');
        return ResourceLocation.fromNamespaceAndPath(
                ns, "heating_coils_" + coilPath + "_" + optionIndex);
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
