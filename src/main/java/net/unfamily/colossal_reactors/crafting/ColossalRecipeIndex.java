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
import net.minecraft.resources.Identifier;
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
    private static final Map<Identifier, List<Identifier>> FUEL_ID_TO_RECIPES = new HashMap<>();
    private static final Map<Identifier, List<Identifier>> COOLANT_ID_TO_RECIPES = new HashMap<>();
    private static final Map<Identifier, List<Identifier>> TURBINE_ID_TO_RECIPES = new HashMap<>();
    private static final Map<String, List<Identifier>> MELTER_KEY_TO_RECIPES = new HashMap<>();
    /** coilId -> ordered recipe ids (one per consume option after Library split). */
    private static final Map<Identifier, List<Identifier>> HEATING_COIL_OPTION_RECIPES = new LinkedHashMap<>();
    /** Catalyst selector (item id or {@code #tag}) -> recipe ids that declare it. */
    private static final Map<String, List<Identifier>> SCRUBBER_CATALYST_TO_RECIPES = new HashMap<>();

    private ColossalRecipeIndex() {}

    public static void clear() {
        FUEL_ID_TO_RECIPES.clear();
        COOLANT_ID_TO_RECIPES.clear();
        TURBINE_ID_TO_RECIPES.clear();
        MELTER_KEY_TO_RECIPES.clear();
        HEATING_COIL_OPTION_RECIPES.clear();
        SCRUBBER_CATALYST_TO_RECIPES.clear();
    }

    public static void registerFuel(Identifier recipeId, Identifier fuelId) {
        if (recipeId == null || fuelId == null) return;
        FUEL_ID_TO_RECIPES.computeIfAbsent(fuelId, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerCoolant(Identifier recipeId, Identifier coolantId) {
        if (recipeId == null || coolantId == null) return;
        COOLANT_ID_TO_RECIPES.computeIfAbsent(coolantId, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerTurbine(Identifier recipeId, Identifier generationId) {
        if (recipeId == null || generationId == null) return;
        TURBINE_ID_TO_RECIPES.computeIfAbsent(generationId, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerMelter(Identifier recipeId, String mergeKey) {
        if (recipeId == null || mergeKey == null) return;
        MELTER_KEY_TO_RECIPES.computeIfAbsent(mergeKey, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerHeatingCoilOption(Identifier coilId, Identifier recipeId) {
        if (coilId == null || recipeId == null) return;
        HEATING_COIL_OPTION_RECIPES.computeIfAbsent(coilId, k -> new ArrayList<>()).add(recipeId);
    }

    public static void registerScrubberCatalyst(Identifier recipeId, String selector) {
        if (recipeId == null || selector == null || selector.isBlank()) return;
        SCRUBBER_CATALYST_TO_RECIPES.computeIfAbsent(selector.trim(), k -> new ArrayList<>()).add(recipeId);
    }

    public static List<Identifier> fuelRecipes(Identifier fuelId) {
        return List.copyOf(FUEL_ID_TO_RECIPES.getOrDefault(fuelId, List.of()));
    }

    public static List<Identifier> coolantRecipes(Identifier coolantId) {
        return List.copyOf(COOLANT_ID_TO_RECIPES.getOrDefault(coolantId, List.of()));
    }

    public static List<Identifier> turbineRecipes(Identifier generationId) {
        return List.copyOf(TURBINE_ID_TO_RECIPES.getOrDefault(generationId, List.of()));
    }

    public static List<Identifier> melterRecipes(String mergeKey) {
        return List.copyOf(MELTER_KEY_TO_RECIPES.getOrDefault(mergeKey, List.of()));
    }

    public static List<Identifier> heatingCoilOptionRecipes(Identifier coilId) {
        return List.copyOf(HEATING_COIL_OPTION_RECIPES.getOrDefault(coilId, List.of()));
    }

    @Nullable
    public static Identifier heatingCoilOptionRecipe(Identifier coilId, int optionIndex) {
        List<Identifier> ids = HEATING_COIL_OPTION_RECIPES.getOrDefault(coilId, List.of());
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
    public static Identifier heatingCoilLoadOptionId(@Nullable Identifier coilId, int optionIndex) {
        String coilPath = coilId != null ? coilId.getPath() : "unknown";
        int idx = Math.max(0, optionIndex);
        return Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "load/" + coilPath + "_" + idx);
    }

    /**
     * Any unblocked recipe id allows the semantic definition.
     * No providers → allowed. Empty/unindexed lists with providers active → denied
     * (fail-closed so unindexed semantic keys cannot bypass stage locks).
     * Null owner is normal — bridge delegates to providers (world stages apply;
     * not a blanket fail-close). Offline owners still pass their UUID to the bridge.
     */
    public static boolean isAllowed(
            List<Identifier> recipeIds, @Nullable UUID ownerUuid, @Nullable Level level) {
        if (!RecipeBlockBridge.hasProviders()) {
            return true;
        }
        if (recipeIds == null || recipeIds.isEmpty()) {
            return false;
        }
        for (Identifier recipeId : recipeIds) {
            if (!RecipeBlockBridge.isRecipeBlocked(recipeId, ownerUuid, level)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFuelAllowed(Identifier fuelId, @Nullable UUID ownerUuid, @Nullable Level level) {
        return isAllowed(FUEL_ID_TO_RECIPES.getOrDefault(fuelId, Collections.emptyList()), ownerUuid, level);
    }

    public static boolean isCoolantAllowed(
            Identifier coolantId, @Nullable UUID ownerUuid, @Nullable Level level) {
        return isAllowed(COOLANT_ID_TO_RECIPES.getOrDefault(coolantId, Collections.emptyList()), ownerUuid, level);
    }

    public static boolean isTurbineAllowed(
            Identifier generationId, @Nullable UUID ownerUuid, @Nullable Level level) {
        return isAllowed(TURBINE_ID_TO_RECIPES.getOrDefault(generationId, Collections.emptyList()), ownerUuid, level);
    }

    public static boolean isMelterAllowed(String mergeKey, @Nullable UUID ownerUuid, @Nullable Level level) {
        return isAllowed(MELTER_KEY_TO_RECIPES.getOrDefault(mergeKey, Collections.emptyList()), ownerUuid, level);
    }

    public static boolean isHeatingCoilOptionAllowed(
            Identifier coilId, int optionIndex, @Nullable UUID ownerUuid, @Nullable Level level) {
        if (!RecipeBlockBridge.hasProviders()) {
            return true;
        }
        Identifier recipeId = heatingCoilOptionRecipe(coilId, optionIndex);
        if (recipeId == null) {
            recipeId = heatingCoilLoadOptionId(coilId, optionIndex);
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
        List<Identifier> matched = new ArrayList<>();
        for (Map.Entry<String, List<Identifier>> e : SCRUBBER_CATALYST_TO_RECIPES.entrySet()) {
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
                Identifier tagId = Identifier.parse(selector.substring(1).trim());
                TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
                return stack.is(tag);
            }
            Identifier itemId = Identifier.parse(selector.trim());
            Item item = BuiltInRegistries.ITEM.getValue(itemId);
            return item != null && !item.equals(net.minecraft.world.item.Items.AIR) && stack.is(item);
        } catch (Exception ignored) {
            return false;
        }
    }
}
