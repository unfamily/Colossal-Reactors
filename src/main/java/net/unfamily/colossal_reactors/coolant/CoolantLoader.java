package net.unfamily.colossal_reactors.coolant;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.colossal_reactors.ColossalReactors;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads coolant definitions from datapack JSON: data/colossal_reactors/reactor_coolant/*.json.
 * Each file is one definition (coolant_id, inputs, output, etc.).
 * Internal default: water (#c:water) → #c:steam. Datapack entries override by coolant_id.
 */
public class CoolantLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoolantLoader.class);

    private static final String KEY_COOLANT_ID = "coolant_id";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_RF_INCREMENT_PERCENT = "rf_increment_percent";
    private static final String KEY_MB_DECREMENT_PERCENT = "mb_decrement_percent";
    private static final String KEY_REDUCE_RF_PRODUCTION = "reduce_rf_production";
    @SuppressWarnings("DeprecatedIsStillUsed") // backward compatibility with old scripts
    private static final String KEY_CONSUMES_FLUID_FOR_STEAM_LEGACY = "consumes_fluid_for_steam";
    private static final String KEY_RF_TO_COOLANT_FACTOR = "rf_to_coolant_factor";
    private static final String KEY_STEAM_PER_COOLANT = "steam_per_coolant";
    private static final String KEY_FLUID_COLOR = "fluid_color";
    private static final String KEY_OUTPUT_COLOR = "output_color";
    private static final String KEY_OVERWRITABLE = "overwritable";

    /** Default colors for simple fluid bar rendering: water azure, steam almost white. ARGB. */
    private static final int DEFAULT_WATER_COLOR = 0xFF3498DB;
    private static final int DEFAULT_STEAM_COLOR = 0xFFE8F0F0;

    private static final Map<ResourceLocation, CoolantDefinition> DEFINITIONS = new HashMap<>();

    /** Default coolant id for water. */
    public static final ResourceLocation WATER_COOLANT_ID = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "water");

    /**
     * Applies loaded datapack data: clears, registers internal defaults, then merges in loaded map.
     */
    public static void applyLoaded(Map<ResourceLocation, CoolantDefinition> loaded) {
        DEFINITIONS.clear();
        registerInternalDefaults();
        if (loaded != null) {
            for (CoolantDefinition def : loaded.values()) {
                processEntry(def);
            }
        }
    }

    private static void registerInternalDefaults() {
        // #c:water tag so vanilla and modded waters are accepted
        List<String> inputs = List.of("#c:water");
        String output = "#c:steam";
        DEFINITIONS.put(WATER_COOLANT_ID, new CoolantDefinition(WATER_COOLANT_ID, inputs, output, 0, 100, true, 0.45, 1.0, DEFAULT_WATER_COLOR, DEFAULT_STEAM_COLOR, true));
    }

    /** Parses a single coolant definition from JSON (one file = one entry). Used by datapack reload listener. */
    public static CoolantDefinition parseEntry(JsonObject json, String sourcePath, boolean defaultOverwritable) {
        if (!json.has(KEY_COOLANT_ID)) {
            LOGGER.warn("Coolant entry in {}: missing 'coolant_id'", sourcePath);
            return null;
        }
        ResourceLocation coolantId = ResourceLocation.tryParse(json.get(KEY_COOLANT_ID).getAsString());
        if (coolantId == null) {
            LOGGER.warn("Coolant entry in {}: invalid coolant_id", sourcePath);
            return null;
        }
        List<String> inputs = new ArrayList<>();
        if (json.has(KEY_INPUTS) && json.get(KEY_INPUTS).isJsonArray()) {
            for (JsonElement i : json.getAsJsonArray(KEY_INPUTS)) {
                if (i.isJsonPrimitive()) inputs.add(i.getAsString());
            }
        }
        String output = json.has(KEY_OUTPUT) ? json.get(KEY_OUTPUT).getAsString() : "";
        int rfIncrement = json.has(KEY_RF_INCREMENT_PERCENT) ? json.get(KEY_RF_INCREMENT_PERCENT).getAsInt() : 0;
        int mbDecrement = json.has(KEY_MB_DECREMENT_PERCENT) ? json.get(KEY_MB_DECREMENT_PERCENT).getAsInt() : 100;
        boolean reduceRf = json.has(KEY_REDUCE_RF_PRODUCTION) ? json.get(KEY_REDUCE_RF_PRODUCTION).getAsBoolean()
                : (json.has(KEY_CONSUMES_FLUID_FOR_STEAM_LEGACY) && json.get(KEY_CONSUMES_FLUID_FOR_STEAM_LEGACY).getAsBoolean());
        double rfToCoolant = json.has(KEY_RF_TO_COOLANT_FACTOR) ? json.get(KEY_RF_TO_COOLANT_FACTOR).getAsDouble() : 0.45;
        double steamPerCoolant = json.has(KEY_STEAM_PER_COOLANT) ? json.get(KEY_STEAM_PER_COOLANT).getAsDouble() : 1.0;
        int fluidColor = parseColor(json, KEY_FLUID_COLOR, DEFAULT_WATER_COLOR);
        int outputColor = parseColor(json, KEY_OUTPUT_COLOR, DEFAULT_STEAM_COLOR);
        boolean overwritable = json.has(KEY_OVERWRITABLE) ? json.get(KEY_OVERWRITABLE).getAsBoolean() : defaultOverwritable;
        return new CoolantDefinition(coolantId, inputs.isEmpty() ? List.of(coolantId.toString()) : List.copyOf(inputs), output, rfIncrement, mbDecrement, reduceRf, rfToCoolant, steamPerCoolant, fluidColor, outputColor, overwritable);
    }

    /** Parses optional color from JSON: "fluid_color": "#3498db" or number. Returns ARGB (0 = use default). */
    private static int parseColor(JsonObject json, String key, int defaultColor) {
        if (!json.has(key)) return defaultColor;
        JsonElement el = json.get(key);
        if (el.isJsonPrimitive()) {
            if (el.getAsJsonPrimitive().isString()) {
                String hex = el.getAsString();
                if (hex.startsWith("#") && hex.length() >= 7) {
                    try {
                        int rgb = Integer.parseInt(hex.substring(1), 16);
                        return 0xFF000000 | (rgb & 0xFFFFFF);
                    } catch (NumberFormatException ignored) { }
                }
            } else if (el.getAsJsonPrimitive().isNumber()) {
                return el.getAsInt() & 0xFFFFFFFF;
            }
        }
        return defaultColor;
    }

    private static void processEntry(CoolantDefinition def) {
        CoolantDefinition existing = DEFINITIONS.get(def.coolantId());
        if (existing != null && !existing.overwritable()) {
            LOGGER.debug("Skipping coolant {}: existing definition is not overwritable", def.coolantId());
            return;
        }
        DEFINITIONS.put(def.coolantId(), def);
    }

    public static CoolantDefinition get(ResourceLocation coolantId) {
        return DEFINITIONS.get(coolantId);
    }

    public static Map<ResourceLocation, CoolantDefinition> getAll() {
        return new HashMap<>(DEFINITIONS);
    }

    /** No longer used (disable removed); kept for API compatibility. */
    public static boolean isInputExcluded(String inputSelector) {
        return false;
    }

    /** Returns the first fluid in the given tag (e.g. "#c:steam"), or null if not a tag or tag is empty. */
    @Nullable
    public static Fluid getFirstFluidFromTag(String outputSelector, net.minecraft.core.RegistryAccess registryAccess) {
        if (outputSelector == null || !outputSelector.startsWith("#")) return null;
        ResourceLocation tagId = ResourceLocation.tryParse(outputSelector.substring(1));
        if (tagId == null) return null;
        var tagKey = TagKey.create(Registries.FLUID, tagId);
        return registryAccess.lookup(Registries.FLUID)
                .flatMap(l -> l.get(tagKey))
                .flatMap(holders -> holders.stream().findFirst())
                .map(h -> h.value())
                .orElse(null);
    }

    /** Returns the fluid to drain for this coolant (first input: fluid id or first fluid from tag). Null if definition has no valid input. */
    @Nullable
    public static Fluid getFirstFluidFromDefinition(CoolantDefinition def, RegistryAccess registryAccess) {
        if (def == null || def.inputs().isEmpty()) return null;
        String first = def.inputs().get(0);
        if (first.startsWith("#")) return getFirstFluidFromTag(first, registryAccess);
        ResourceLocation id = ResourceLocation.tryParse(first);
        return id != null ? BuiltInRegistries.FLUID.get(id) : null;
    }

    /**
     * Finds the coolant definition that matches the given fluid (by fluid id or fluid tag). Returns null if excluded or no match.
     */
    @Nullable
    public static CoolantDefinition getDefinitionForFluid(Fluid fluid, RegistryAccess registryAccess) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        for (CoolantDefinition def : DEFINITIONS.values()) {
            for (String input : def.inputs()) {
                if (isInputExcluded(input)) continue;
                if (input.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(input.substring(1));
                    if (tagId == null) continue;
                    TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
                    var fluidHolder = registryAccess.registryOrThrow(Registries.FLUID).getHolder(ResourceKey.create(Registries.FLUID, fluidId)).orElse(null);
                    if (fluidHolder == null) continue;
                    boolean inTag = registryAccess.lookup(Registries.FLUID)
                            .flatMap(l -> l.get(tagKey))
                            .map(holders -> holders.contains(fluidHolder))
                            .orElse(false);
                    if (inTag) return def;
                } else {
                    if (ResourceLocation.tryParse(input).equals(fluidId)) return def;
                }
            }
        }
        return null;
    }
}
