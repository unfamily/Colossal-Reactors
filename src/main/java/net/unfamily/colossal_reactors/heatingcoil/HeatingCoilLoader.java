package net.unfamily.colossal_reactors.heatingcoil;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses heating coil JSON (type colossal_reactors:heating_coils, coils array).
 * Used by builtin load and by LoadDataReloadListener.
 */
public final class HeatingCoilLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatingCoilLoader.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final String TYPE_HEATING_COILS = "colossal_reactors:heating_coils";
    private static final String KEY_TYPE = "type";
    private static final String KEY_COILS = "coils";
    private static final String KEY_ID = "id";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_CONSUME = "consume";
    private static final String KEY_FLUID = "fluid";
    private static final String KEY_ITEM = "item";
    private static final String KEY_ENERGY = "energy";
    private static final String KEY_BURNABLE = "burnable";
    private static final String KEY_ACTIVATION = "activation";
    private static final String KEY_SUBSTAIN = "substain";
    private static final String KEY_NO_ITEM = "no_item";
    private static final String KEY_NO_FLUID = "no_fluid";
    private static final String KEY_NO_ENERGY = "no_energy";

    private HeatingCoilLoader() {}

    /**
     * Parses heating coil definitions from JSON. Expects root with "type": "colossal_reactors:heating_coils" and "coils" array.
     * Returns empty list if type mismatch or parse error.
     */
    public static List<HeatingCoilDefinition> parse(Reader reader, String source) {
        try {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            return parseFromRoot(root, source);
        } catch (Exception e) {
            LOGGER.warn("Failed to parse heating coils from {}: {}", source, e.getMessage());
            return List.of();
        }
    }

    /**
     * Parses coil definitions from an already-parsed root (same structure as parse(Reader)).
     */
    public static List<HeatingCoilDefinition> parseFromRoot(@Nullable JsonObject root, String source) {
        List<HeatingCoilDefinition> out = new ArrayList<>();
        if (root == null || !root.has(KEY_TYPE) || !TYPE_HEATING_COILS.equals(root.get(KEY_TYPE).getAsString())) {
            return out;
        }
        if (!root.has(KEY_COILS) || !root.get(KEY_COILS).isJsonArray()) {
            return out;
        }
        JsonArray coils = root.getAsJsonArray(KEY_COILS);
        for (JsonElement el : coils) {
            if (!el.isJsonObject()) continue;
            HeatingCoilDefinition def = parseOneCoil(el.getAsJsonObject(), source);
            if (def != null) out.add(def);
        }
        return out;
    }

    @Nullable
    private static HeatingCoilDefinition parseOneCoil(JsonObject json, String source) {
        if (!json.has(KEY_ID)) {
            LOGGER.warn("Heating coil in {}: missing 'id'", source);
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(json.get(KEY_ID).getAsString());
        if (id == null) {
            LOGGER.warn("Heating coil in {}: invalid id", source);
            return null;
        }
        int duration = json.has(KEY_DURATION) ? json.get(KEY_DURATION).getAsInt() : 60;
        if (duration <= 0) duration = 60;

        List<ConsumeOption> consume = new ArrayList<>();
        if (json.has(KEY_CONSUME) && json.get(KEY_CONSUME).isJsonArray()) {
            for (JsonElement ce : json.getAsJsonArray(KEY_CONSUME)) {
                if (!ce.isJsonObject()) continue;
                ConsumeOption opt = parseConsumeOption(ce.getAsJsonObject(), source);
                if (opt != null && !opt.isEmpty()) consume.add(opt);
            }
        }
        boolean noItem = json.has(KEY_NO_ITEM) && json.get(KEY_NO_ITEM).getAsBoolean();
        boolean noFluid = json.has(KEY_NO_FLUID) && json.get(KEY_NO_FLUID).getAsBoolean();
        boolean noEnergy = json.has(KEY_NO_ENERGY) && json.get(KEY_NO_ENERGY).getAsBoolean();
        return new HeatingCoilDefinition(id, duration, consume, noItem, noFluid, noEnergy);
    }

    @Nullable
    private static ConsumeOption parseConsumeOption(JsonObject json, String source) {
        ConsumeOption.FluidRequirement fluid = parseFluid(json.get(KEY_FLUID), source);
        ConsumeOption.ItemRequirement item = parseItem(json.get(KEY_ITEM), source);
        ConsumeOption.EnergyRequirement energy = parseEnergy(json.get(KEY_ENERGY), source);
        ConsumeOption.BurnableRequirement burnable = parseBurnable(json.get(KEY_BURNABLE), source);
        if (fluid == null && item == null && energy == null && burnable == null) return null;
        return new ConsumeOption(fluid, item, energy, burnable);
    }

    @Nullable
    private static ConsumeOption.FluidRequirement parseFluid(JsonElement el, String source) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject o = el.getAsJsonObject();
        if (!o.has("id")) return null;
        String idStr = o.get("id").getAsString();
        boolean isTag = idStr.startsWith("#");
        ResourceLocation tagOrId = parseId(idStr);
        if (tagOrId == null) return null;
        int activation = o.has(KEY_ACTIVATION) ? o.get(KEY_ACTIVATION).getAsInt() : 0;
        int substain = o.has(KEY_SUBSTAIN) ? o.get(KEY_SUBSTAIN).getAsInt() : 0;
        return new ConsumeOption.FluidRequirement(tagOrId, isTag, activation, substain);
    }

    @Nullable
    private static ConsumeOption.ItemRequirement parseItem(JsonElement el, String source) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject o = el.getAsJsonObject();
        if (!o.has("id")) return null;
        String idStr = o.get("id").getAsString();
        boolean isTag = idStr.startsWith("#");
        ResourceLocation tagOrId = parseId(idStr);
        if (tagOrId == null) return null;
        int activation = o.has(KEY_ACTIVATION) ? o.get(KEY_ACTIVATION).getAsInt() : 0;
        int substain = o.has(KEY_SUBSTAIN) ? o.get(KEY_SUBSTAIN).getAsInt() : 0;
        return new ConsumeOption.ItemRequirement(tagOrId, isTag, activation, substain);
    }

    @Nullable
    private static ConsumeOption.EnergyRequirement parseEnergy(JsonElement el, String source) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject o = el.getAsJsonObject();
        int activation = o.has(KEY_ACTIVATION) ? o.get(KEY_ACTIVATION).getAsInt() : 0;
        int substain = o.has(KEY_SUBSTAIN) ? o.get(KEY_SUBSTAIN).getAsInt() : 0;
        return new ConsumeOption.EnergyRequirement(activation, substain);
    }

    @Nullable
    private static ConsumeOption.BurnableRequirement parseBurnable(JsonElement el, String source) {
        if (el == null || !el.isJsonObject()) return null;
        JsonObject o = el.getAsJsonObject();
        int activation = o.has(KEY_ACTIVATION) ? o.get(KEY_ACTIVATION).getAsInt() : 0;
        int substain = o.has(KEY_SUBSTAIN) ? o.get(KEY_SUBSTAIN).getAsInt() : 0;
        return new ConsumeOption.BurnableRequirement(activation, substain);
    }

    /** "#namespace:path" -> namespace:path; else parse as ResourceLocation. */
    @Nullable
    private static ResourceLocation parseId(String s) {
        if (s == null || s.isEmpty()) return null;
        if (s.startsWith("#")) s = s.substring(1);
        return ResourceLocation.tryParse(s);
    }
}
