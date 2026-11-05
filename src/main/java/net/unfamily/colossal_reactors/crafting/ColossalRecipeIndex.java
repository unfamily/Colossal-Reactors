package net.unfamily.colossal_reactors.crafting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.unfamily.iskalib.crafting.RecipeBlockBridge;
import org.jetbrains.annotations.Nullable;

/**
 * Maps RecipeManager child ids ({@code path_N}) to semantic machine definitions for stage/KubeJS gating.
 */
public final class ColossalRecipeIndex {
    private static final Map<Identifier, List<Identifier>> FUEL_ID_TO_RECIPES = new HashMap<>();
    private static final Map<Identifier, List<Identifier>> COOLANT_ID_TO_RECIPES = new HashMap<>();
    private static final Map<Identifier, List<Identifier>> TURBINE_ID_TO_RECIPES = new HashMap<>();
    private static final Map<String, List<Identifier>> MELTER_KEY_TO_RECIPES = new HashMap<>();
    /** coilId -> ordered recipe ids (one per consume option after Library split). */
    private static final Map<Identifier, List<Identifier>> HEATING_COIL_OPTION_RECIPES = new LinkedHashMap<>();

    private ColossalRecipeIndex() {}

    public static void clear() {
        FUEL_ID_TO_RECIPES.clear();
        COOLANT_ID_TO_RECIPES.clear();
        TURBINE_ID_TO_RECIPES.clear();
        MELTER_KEY_TO_RECIPES.clear();
        HEATING_COIL_OPTION_RECIPES.clear();
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
     * When providers are registered (stages etc.): missing/offline owner => blocked.
     * Otherwise any unblocked recipe id allows the semantic definition.
     * No tracked recipe ids => allowed (load/ retrocompat or unindexed).
     */
    public static boolean isAllowed(
            List<Identifier> recipeIds, @Nullable ServerPlayer owner, @Nullable Level level) {
        if (!RecipeBlockBridge.hasProviders()) {
            return true;
        }
        if (recipeIds == null || recipeIds.isEmpty()) {
            return true;
        }
        if (owner == null) {
            return false;
        }
        for (Identifier recipeId : recipeIds) {
            if (!RecipeBlockBridge.isRecipeBlocked(recipeId, owner, level)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isFuelAllowed(Identifier fuelId, @Nullable ServerPlayer owner, @Nullable Level level) {
        return isAllowed(FUEL_ID_TO_RECIPES.getOrDefault(fuelId, Collections.emptyList()), owner, level);
    }

    public static boolean isCoolantAllowed(
            Identifier coolantId, @Nullable ServerPlayer owner, @Nullable Level level) {
        return isAllowed(COOLANT_ID_TO_RECIPES.getOrDefault(coolantId, Collections.emptyList()), owner, level);
    }

    public static boolean isTurbineAllowed(
            Identifier generationId, @Nullable ServerPlayer owner, @Nullable Level level) {
        return isAllowed(TURBINE_ID_TO_RECIPES.getOrDefault(generationId, Collections.emptyList()), owner, level);
    }

    public static boolean isMelterAllowed(String mergeKey, @Nullable ServerPlayer owner, @Nullable Level level) {
        return isAllowed(MELTER_KEY_TO_RECIPES.getOrDefault(mergeKey, Collections.emptyList()), owner, level);
    }

    public static boolean isHeatingCoilOptionAllowed(
            Identifier coilId, int optionIndex, @Nullable ServerPlayer owner, @Nullable Level level) {
        Identifier recipeId = heatingCoilOptionRecipe(coilId, optionIndex);
        if (recipeId == null) {
            return !RecipeBlockBridge.hasProviders() || owner != null;
        }
        return isAllowed(List.of(recipeId), owner, level);
    }
}
