package net.unfamily.colossal_reactors.coolant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
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
import net.unfamily.colossal_reactors.Config;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Loads coolant definitions from external JSON (external_scripts_path/reactor/*.json with type colossal_reactors:coolant).
 * Internal default: water (minecraft:water) → output #c:steam. JSON entries can add or override by coolant_id.
 * "disable" is optional (default false). When true, the entry means "these tags/liquids: no" (excluded inputs).
 */
public class CoolantLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoolantLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String TYPE_COOLANT = "colossal_reactors:coolant";
    private static final String KEY_ENTRIES = "entries";
    /** Optional, default false. When true: "these inputs (tags/fluids) are not valid" — they are excluded. */
    private static final String KEY_DISABLE = "disable";
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
    /** Input selectors (tag or fluid id) that are excluded: not valid as coolant. */
    private static final Set<String> EXCLUDED_INPUTS = new HashSet<>();

    /** Default coolant id for water. */
    public static final ResourceLocation WATER_COOLANT_ID = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "water");

    /**
     * Scans the reactor config directory under configured external scripts path. Internal defaults first, then JSON overrides.
     */
    public static void scanConfigDirectory() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scanning reactor config directory (coolant)...");
        }
        DEFINITIONS.clear();
        EXCLUDED_INPUTS.clear();
        registerInternalDefaults();

        String basePath = Config.EXTERNAL_SCRIPTS_PATH.get();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "kubejs/external_scripts/colossal_reactors";
        }
        Path reactorPath = Paths.get(basePath, "reactor");
        if (!Files.exists(reactorPath)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Reactor config directory does not exist ({}). Using internal defaults only. Run dump to create it.", reactorPath.toAbsolutePath());
            }
            return;
        }
        if (!Files.isDirectory(reactorPath)) {
            LOGGER.warn("Reactor config path is not a directory: {}", reactorPath);
            return;
        }
        try (Stream<Path> files = Files.walk(reactorPath)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .sorted()
                    .forEach(CoolantLoader::parseConfigFile);
        } catch (IOException e) {
            LOGGER.error("Error scanning reactor config directory: {}", e.getMessage());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Coolant definitions loaded: {}", DEFINITIONS.size());
        }
    }

    private static void registerInternalDefaults() {
        // Vanilla water and forge:water tag so modded waters are accepted
        List<String> inputs = List.of("minecraft:water", "#forge:water");
        String output = "#c:steam";
        DEFINITIONS.put(WATER_COOLANT_ID, new CoolantDefinition(WATER_COOLANT_ID, inputs, output, 0, 100, true, 0.45, 1.0, DEFAULT_WATER_COLOR, DEFAULT_STEAM_COLOR, true));
    }

    private static void parseConfigFile(Path filePath) {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonElement el = GSON.fromJson(reader, JsonElement.class);
            if (el == null || !el.isJsonObject()) return;
            JsonObject root = el.getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "";
            if (!TYPE_COOLANT.equals(type)) {
                LOGGER.debug("Skipping {}: type is not {}", filePath.getFileName(), TYPE_COOLANT);
                return;
            }
            boolean fileOverwritable = !root.has(KEY_OVERWRITABLE) || root.get(KEY_OVERWRITABLE).getAsBoolean();
            if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) {
                LOGGER.warn("Coolant config {}: missing or invalid 'entries' array", filePath.getFileName());
                return;
            }
            JsonArray entries = root.getAsJsonArray(KEY_ENTRIES);
            for (JsonElement e : entries) {
                if (!e.isJsonObject()) continue;
                JsonObject obj = e.getAsJsonObject();
                boolean disable = obj.has(KEY_DISABLE) && obj.get(KEY_DISABLE).getAsBoolean();
                if (disable) {
                    addExcludedInputs(obj);
                    continue;
                }
                CoolantDefinition def = parseEntry(obj, filePath.toString(), fileOverwritable);
                if (def != null) processEntry(def);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing coolant file {}: {}", filePath, e.getMessage());
        }
    }

    private static CoolantDefinition parseEntry(JsonObject json, String filePath, boolean defaultOverwritable) {
        if (!json.has(KEY_COOLANT_ID)) {
            LOGGER.warn("Coolant entry in {}: missing 'coolant_id'", filePath);
            return null;
        }
        ResourceLocation coolantId = ResourceLocation.tryParse(json.get(KEY_COOLANT_ID).getAsString());
        if (coolantId == null) {
            LOGGER.warn("Coolant entry in {}: invalid coolant_id", filePath);
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

    private static void addExcludedInputs(JsonObject obj) {
        if (!obj.has(KEY_INPUTS) || !obj.get(KEY_INPUTS).isJsonArray()) return;
        for (JsonElement i : obj.getAsJsonArray(KEY_INPUTS)) {
            if (i.isJsonPrimitive()) EXCLUDED_INPUTS.add(i.getAsString());
        }
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

    /** True if this input selector (tag or fluid id) is excluded ("disable: true" for these tags/liquids). */
    public static boolean isInputExcluded(String inputSelector) {
        return EXCLUDED_INPUTS.contains(inputSelector);
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
     * Returns ARGB color for simple fluid bar rendering. Input fluids use fluid_color, output (e.g. steam) uses output_color.
     * Returns 0 to use game default when no definition provides a color.
     */
    public static int getColorForFluid(Fluid fluid, RegistryAccess registryAccess) {
        if (fluid == null || fluid == Fluids.EMPTY) return 0;
        for (CoolantDefinition def : DEFINITIONS.values()) {
            Fluid outputFluid = getFirstFluidFromTag(def.output(), registryAccess);
            if (outputFluid == fluid && def.outputColor() != 0) return def.outputColor();
            for (String input : def.inputs()) {
                if (isInputExcluded(input)) continue;
                Fluid fromInput = input.startsWith("#") ? getFirstFluidFromTag(input, registryAccess) : BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(input));
                if (fromInput == fluid && def.fluidColor() != 0) return def.fluidColor();
            }
        }
        return 0;
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

    /**
     * Writes the default coolant JSON into the given directory (e.g. reactor). Called by dump command.
     */
    public static void dumpDefaultFile(Path reactorDir) throws IOException {
        Files.createDirectories(reactorDir);
        Path file = reactorDir.resolve("default_coolant.json");
        String content = """
            {
              "type": "colossal_reactors:coolant",
              "overwritable": true,
              "entries": [
                {
                  "disable": false,
                  "coolant_id": "colossal_reactors:water",
                  "inputs": [
                    "minecraft:water"
                  ],
                  "output": "#c:steam",
                  "rf_increment_percent": 0,
                  "mb_decrement_percent": 100,
                  "reduce_rf_production": true,
                  "rf_to_coolant_factor": 0.45,
                  "steam_per_coolant": 1.0,
                  "fluid_color": "#3498db",
                  "output_color": "#e8f0f0"
                }
              ]
            }
            """;
        Files.writeString(file, content);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Dumped default coolant to {}", file.toAbsolutePath());
        }
    }
}
