package net.unfamily.colossal_reactors.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.iskalib.crafting.RecipeBlockBridge;
import org.jetbrains.annotations.Nullable;

/**
 * Maps RecipeManager child ids ({@code path_N}) to semantic machine definitions for stage/KubeJS gating.
 * <p>
 * Synergy Matter Foundry recipes ({@code synergy:foundry} under {@code recipe/synergy/foundry/}) are
 * not indexed here — stage those via Synergy/KubeJS recipe ids, not ColossalRecipeIndex.
 */
public final class ColossalRecipeIndex {
    private static final Map<ResourceLocation, List<ResourceLocation>> FUEL_ID_TO_RECIPES = new HashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> COOLANT_ID_TO_RECIPES = new HashMap<>();
    private static final Map<ResourceLocation, List<ResourceLocation>> TURBINE_ID_TO_RECIPES = new HashMap<>();
    private static final Map<String, List<ResourceLocation>> MELTER_KEY_TO_RECIPES = new HashMap<>();
    /** coilId -> ordered recipe ids (one per consume option after Library split). */
    private static final Map<ResourceLocation, List<ResourceLocation>> HEATING_COIL_OPTION_RECIPES = new LinkedHashMap<>();
    /** Catalyst selector (item id or {@code #tag}) -> recipe ids that declare it. */
    private static final Map<String, List<ResourceLocation>> SCRUBBER_CATALYST_TO_RECIPES = new HashMap<>();

    private ColossalRecipeIndex() {}

    public static void clear() {
        FUEL_ID_TO_RECIPES.clear();
        COOLANT_ID_TO_RECIPES.clear();
        TURBINE_ID_TO_RECIPES.clear();
        MELTER_KEY_TO_RECIPES.clear();
        HEATING_COIL_OPTION_RECIPES.clear();
        SCRUBBER_CATALYST_TO_RECIPES.clear();
    }

    public static void registerFuel(ResourceLocation recipeId, ResourceLocation fuelId) {
        if (recipeId == null || fuelId == null) return;
        FUEL_ID_TO_RECIPES.computeIfAbsent(fuelId, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerCoolant(ResourceLocation recipeId, ResourceLocation coolantId) {
        if (recipeId == null || coolantId == null) return;
        COOLANT_ID_TO_RECIPES.computeIfAbsent(coolantId, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerTurbine(ResourceLocation recipeId, ResourceLocation generationId) {
        if (recipeId == null || generationId == null) return;
        TURBINE_ID_TO_RECIPES.computeIfAbsent(generationId, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerMelter(ResourceLocation recipeId, String mergeKey) {
        if (recipeId == null || mergeKey == null) return;
        MELTER_KEY_TO_RECIPES.computeIfAbsent(mergeKey, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerHeatingCoilOption(ResourceLocation coilId, ResourceLocation recipeId) {
        if (coilId == null || recipeId == null) return;
        HEATING_COIL_OPTION_RECIPES.computeIfAbsent(coilId, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerScrubberCatalyst(ResourceLocation recipeId, String selector) {
        if (recipeId == null || selector == null || selector.isBlank()) return;
        SCRUBBER_CATALYST_TO_RECIPES.computeIfAbsent(selector.trim(), k -> new ArrayList<>()).add(recipeId);
    }

    public static List<ResourceLocation> fuelRecipes(ResourceLocation fuelId) {
        return List.copyOf(FUEL_ID_TO_RECIPES.getOrDefault(fuelId, List.of()));
    }

    public static List<ResourceLocation> coolantRecipes(ResourceLocation coolantId) {
        return List.copyOf(COOLANT_ID_TO_RECIPES.getOrDefault(coolantId, List.of()));
    }

    public static List<ResourceLocation> turbineRecipes(ResourceLocation generationId) {
        return List.copyOf(TURBINE_ID_TO_RECIPES.getOrDefault(generationId, List.of()));
    }

    public static List<ResourceLocation> melterRecipes(String mergeKey) {
        return List.copyOf(MELTER_KEY_TO_RECIPES.getOrDefault(mergeKey, List.of()));
    }

    public static List<ResourceLocation> heatingCoilOptionRecipes(ResourceLocation coilId) {
        return List.copyOf(HEATING_COIL_OPTION_RECIPES.getOrDefault(coilId, List.of()));
    }

    @Nullable
    public static ResourceLocation heatingCoilOptionRecipe(ResourceLocation coilId, int optionIndex) {
        List<ResourceLocation> ids = HEATING_COIL_OPTION_RECIPES.getOrDefault(coilId, List.of());
        if (optionIndex < 0 || optionIndex >= ids.size()) {
            return null;
        }
        return ids.get(optionIndex);
    }

    /**
     * Stable synthetic id for load/-only (or unindexed) coil options:
     * {@code colossal_reactors:load/<coilPath>_<optionIndex>}.
     * Providers gate by this id even when no RecipeManager entry exists.
     */
    public static ResourceLocation heatingCoilLoadOptionId(@Nullable ResourceLocation coilId, int optionIndex) {
        String coilPath = coilId != null ? coilId.getPath() : "unknown";
        int idx = Math.max(0, optionIndex);
        return ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "load/" + coilPath + "_" + idx);
    }

    /**
     * Any unblocked recipe id allows the semantic definition.
     * No providers → allowed. Empty/unindexed lists with providers active → denied
     * (fail-closed so unindexed semantic keys cannot bypass stage locks).
     * Null owner is normal — bridge delegates to providers (world stages apply;
     * not a blanket fail-close). Offline owners still pass their UUID to the bridge.
     */
    public static boolean isAllowed(
            List<ResourceLocation> recipeIds, @Nullable UUID ownerUuid, @Nullable Level level) {
        if (!RecipeBlockBridge.hasProviders()) {
            return true;
        }
        if (recipeIds == null || recipeIds.isEmpty()) {
            return false;
        }
        for (ResourceLocation recipeId : recipeIds) {
            if (!RecipeBlockBridge.isRecipeBlocked(recipeId, ownerUuid, level)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFuelAllowed(
            ResourceLocation fuelId, @Nullable UUID ownerUuid, @Nullable Level level) {
        return isAllowed(FUEL_ID_TO_RECIPES.getOrDefault(fuelId, Collections.emptyList()), ownerUuid, level);
    }

    public static boolean isCoolantAllowed(
            ResourceLocation coolantId, @Nullable UUID ownerUuid, @Nullable Level level) {
        return isAllowed(COOLANT_ID_TO_RECIPES.getOrDefault(coolantId, Collections.emptyList()), ownerUuid, level);
    }

    public static boolean isTurbineAllowed(
            ResourceLocation generationId, @Nullable UUID ownerUuid, @Nullable Level level) {
        return isAllowed(TURBINE_ID_TO_RECIPES.getOrDefault(generationId, Collections.emptyList()), ownerUuid, level);
    }

    public static boolean isMelterAllowed(String mergeKey, @Nullable UUID ownerUuid, @Nullable Level level) {
        return isAllowed(MELTER_KEY_TO_RECIPES.getOrDefault(mergeKey, Collections.emptyList()), ownerUuid, level);
    }

    public static boolean isHeatingCoilOptionAllowed(
            ResourceLocation coilId, int optionIndex, @Nullable UUID ownerUuid, @Nullable Level level) {
        if (!RecipeBlockBridge.hasProviders()) {
            return true;
        }
        // Fail-closed: unindexed options must not invent synthetic ids that bypass stage gates.
        // load/-only coils register via heatingCoilLoadOptionId during ColossalRecipeData fill-missing.
        ResourceLocation recipeId = heatingCoilOptionRecipe(coilId, optionIndex);
        if (recipeId == null) {
            return false;
        }
        return !RecipeBlockBridge.isRecipeBlocked(recipeId, ownerUuid, level);
    }

    /** True if any indexed catalyst selector matching the stack is allowed for the owner. */
    public static boolean isScrubberCatalystAllowed(
            ItemStack stack, @Nullable UUID ownerUuid, @Nullable Level level) {
        if (stack.isEmpty()) {
            return false;
        }
        if (!RecipeBlockBridge.hasProviders()) {
            return true;
        }
        List<ResourceLocation> matched = new ArrayList<>();
        for (Map.Entry<String, List<ResourceLocation>> e : SCRUBBER_CATALYST_TO_RECIPES.entrySet()) {
            if (selectorMatchesStack(e.getKey(), stack)) {
                matched.addAll(e.getValue());
            }
        }
        return isAllowed(matched, ownerUuid, level);
    }

    private static boolean selectorMatchesStack(String selector, ItemStack stack) {
        if (selector == null || selector.isBlank()) {
            return false;
        }
        try {
            if (selector.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.parse(selector.substring(1).trim());
                TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
                return stack.is(tag);
            }
            ResourceLocation itemId = ResourceLocation.parse(selector.trim());
            Item item = BuiltInRegistries.ITEM.get(itemId);
            return item != null && stack.is(item);
        } catch (Exception ignored) {
            return false;
        }
    }
}
