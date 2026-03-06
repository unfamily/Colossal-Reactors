package net.unfamily.colossal_reactors.melter;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads melter recipes from datapack (type colossal_reactors:melter_recipes).
 * Path: data/&lt;namespace&gt;/recipe/... (e.g. recipe/melter/melter_recipes.json).
 */
public final class MelterRecipesLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MelterRecipesLoader.class);
    private static final String KEY_INPUT = "input";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_TIME = "time";
    private static final String KEY_COUNT = "count";

    private static final List<MelterRecipe> RECIPES = new ArrayList<>();

    public static void applyLoaded(List<MelterRecipe> loaded) {
        RECIPES.clear();
        if (loaded != null) RECIPES.addAll(loaded);
    }

    public static List<MelterRecipe> getAll() {
        return Collections.unmodifiableList(RECIPES);
    }

    @Nullable
    public static MelterRecipe getRecipeFor(net.minecraft.world.item.ItemStack stack, net.minecraft.core.RegistryAccess registryAccess) {
        if (stack.isEmpty()) return null;
        for (MelterRecipe r : RECIPES) {
            if (matchesInput(stack, r, registryAccess) && stack.getCount() >= r.count()) return r;
        }
        return null;
    }

    private static boolean matchesInput(net.minecraft.world.item.ItemStack stack, MelterRecipe r, net.minecraft.core.RegistryAccess registryAccess) {
        if (r.inputIsTag()) {
            var tagKey = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, r.inputId());
            return stack.is(tagKey);
        }
        var item = registryAccess.registry(net.minecraft.core.registries.Registries.ITEM).map(reg -> reg.get(r.inputId())).orElse(null);
        return item != null && !item.equals(net.minecraft.world.item.Items.AIR) && stack.is(item);
    }

    @Nullable
    public static MelterRecipe parseEntry(JsonObject json, String source) {
        if (!json.has(KEY_INPUT) || !json.has(KEY_OUTPUT) || !json.has(KEY_AMOUNT) || !json.has(KEY_TIME)) {
            LOGGER.warn("Melter recipe in {}: missing input/output/amount/time", source);
            return null;
        }
        String inputStr = json.get(KEY_INPUT).getAsString();
        boolean inputIsTag = inputStr.startsWith("#");
        ResourceLocation inputId = ResourceLocation.tryParse(inputIsTag ? inputStr.substring(1) : inputStr);
        if (inputId == null) {
            LOGGER.warn("Melter recipe in {}: invalid input", source);
            return null;
        }
        ResourceLocation outputFluidId = ResourceLocation.tryParse(json.get(KEY_OUTPUT).getAsString());
        if (outputFluidId == null) {
            LOGGER.warn("Melter recipe in {}: invalid output", source);
            return null;
        }
        int amount = json.get(KEY_AMOUNT).getAsInt();
        int time = json.get(KEY_TIME).getAsInt();
        if (amount <= 0 || time <= 0) {
            LOGGER.warn("Melter recipe in {}: amount and time must be positive", source);
            return null;
        }
        int count = json.has(KEY_COUNT) ? json.get(KEY_COUNT).getAsInt() : MelterRecipe.DEFAULT_COUNT;
        if (count <= 0) count = MelterRecipe.DEFAULT_COUNT;
        return new MelterRecipe(inputId, inputIsTag, outputFluidId, amount, time, count);
    }
}
