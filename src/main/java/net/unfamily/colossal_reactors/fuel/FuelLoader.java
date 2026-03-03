package net.unfamily.colossal_reactors.fuel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
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
 * Loads fuel definitions from external JSON (external_scripts_path/fuel/*.json).
 * Internal default (uranium) is registered first; JSON entries can add or override by fuel_id.
 * Use dump_default command to write the base fuel JSON into the fuel directory.
 * "disable" is optional (default false). When true, the entry means "these tags/items: no" (excluded inputs).
 */
public class FuelLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FuelLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String TYPE_FUEL = "colossal_reactors:fuel";
    private static final String KEY_ENTRIES = "entries";
    /** Optional, default false. When true: "these inputs (tags/items) are not valid" — they are excluded. */
    private static final String KEY_DISABLE = "disable";
    private static final String KEY_FUEL_ID = "fuel_id";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_UNITS_PER_ITEM = "units_per_item";
    private static final String KEY_BASE_RF_PER_TICK = "base_rf_per_tick";
    private static final String KEY_BASE_MB_PER_TICK = "base_mb_per_tick";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_OVERWRITABLE = "overwritable";

    private static final Map<ResourceLocation, FuelDefinition> DEFINITIONS = new HashMap<>();
    /** Input selectors (tag or item id) that are excluded: not valid as fuel even if a definition would match. */
    private static final Set<String> EXCLUDED_INPUTS = new HashSet<>();

    /**
     * Scans the fuel directory under configured external scripts path. Internal defaults first, then JSON overrides.
     */
    public static void scanConfigDirectory() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scanning fuel configuration directory...");
        }
        DEFINITIONS.clear();
        EXCLUDED_INPUTS.clear();
        registerInternalDefaults();

        String basePath = Config.EXTERNAL_SCRIPTS_PATH.get();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "kubejs/external_scripts/colossal_reactors";
        }
        Path fuelPath = Paths.get(basePath, "fuel");
        if (!Files.exists(fuelPath)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Fuel directory does not exist ({}). Using internal defaults only. Run dump_default to create it.", fuelPath.toAbsolutePath());
            }
            return;
        }
        if (!Files.isDirectory(fuelPath)) {
            LOGGER.warn("Fuel path is not a directory: {}", fuelPath);
            return;
        }
        try (Stream<Path> files = Files.walk(fuelPath)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .sorted()
                    .forEach(FuelLoader::parseConfigFile);
        } catch (IOException e) {
            LOGGER.error("Error scanning fuel directory: {}", e.getMessage());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Fuel definitions loaded: {}", DEFINITIONS.size());
        }
    }

    private static void registerInternalDefaults() {
        ResourceLocation uraniumId = ReactorRodBlockEntity.URANIUM_FUEL_ID;
        int unitsPerItem = Config.URANIUM_INGOT_MB.get();
        double baseRf = Config.BASE_RF_PER_TICK.get();
        double baseMb = Config.BASE_MB_PER_TICK.get();
        List<String> inputs = List.of(
                "#c:ingots/uranium",
                ColossalReactors.MODID + ":uranium_ingot"
        );
        String output = ColossalReactors.MODID + ":nuclear_waste";
        DEFINITIONS.put(uraniumId, new FuelDefinition(uraniumId, inputs, output, unitsPerItem, baseRf, baseMb, true));
    }

    private static void parseConfigFile(Path filePath) {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonElement el = GSON.fromJson(reader, JsonElement.class);
            if (el == null || !el.isJsonObject()) return;
            JsonObject root = el.getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "";
            if (!TYPE_FUEL.equals(type)) {
                LOGGER.debug("Skipping {}: type is not {}", filePath.getFileName(), TYPE_FUEL);
                return;
            }
            boolean fileOverwritable = !root.has(KEY_OVERWRITABLE) || root.get(KEY_OVERWRITABLE).getAsBoolean();
            if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) {
                LOGGER.warn("Fuel config {}: missing or invalid 'entries' array", filePath.getFileName());
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
                FuelDefinition def = parseEntry(obj, filePath.toString(), fileOverwritable);
                if (def != null) processEntry(def);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing fuel file {}: {}", filePath, e.getMessage());
        }
    }

    private static FuelDefinition parseEntry(JsonObject json, String filePath, boolean defaultOverwritable) {
        if (!json.has(KEY_FUEL_ID)) {
            LOGGER.warn("Fuel entry in {}: missing 'fuel_id'", filePath);
            return null;
        }
        ResourceLocation fuelId = ResourceLocation.tryParse(json.get(KEY_FUEL_ID).getAsString());
        if (fuelId == null) {
            LOGGER.warn("Fuel entry in {}: invalid fuel_id", filePath);
            return null;
        }
        List<String> inputs = new ArrayList<>();
        if (json.has(KEY_INPUTS) && json.get(KEY_INPUTS).isJsonArray()) {
            for (JsonElement i : json.getAsJsonArray(KEY_INPUTS)) {
                if (i.isJsonPrimitive()) inputs.add(i.getAsString());
            }
        }
        String output = json.has(KEY_OUTPUT) ? json.get(KEY_OUTPUT).getAsString() : "";
        int unitsPerItem = json.has(KEY_UNITS_PER_ITEM) ? json.get(KEY_UNITS_PER_ITEM).getAsInt() : Config.URANIUM_INGOT_MB.get();
        double baseRf = json.has(KEY_BASE_RF_PER_TICK) ? json.get(KEY_BASE_RF_PER_TICK).getAsDouble() : Config.BASE_RF_PER_TICK.get();
        double baseMb = json.has(KEY_BASE_MB_PER_TICK) ? json.get(KEY_BASE_MB_PER_TICK).getAsDouble() : Config.BASE_MB_PER_TICK.get();
        boolean overwritable = json.has(KEY_OVERWRITABLE) ? json.get(KEY_OVERWRITABLE).getAsBoolean() : defaultOverwritable;
        return new FuelDefinition(fuelId, inputs.isEmpty() ? List.of(fuelId.toString()) : List.copyOf(inputs), output, unitsPerItem, baseRf, baseMb, overwritable);
    }

    private static void addExcludedInputs(JsonObject obj) {
        if (!obj.has(KEY_INPUTS) || !obj.get(KEY_INPUTS).isJsonArray()) return;
        for (JsonElement i : obj.getAsJsonArray(KEY_INPUTS)) {
            if (i.isJsonPrimitive()) EXCLUDED_INPUTS.add(i.getAsString());
        }
    }

    private static void processEntry(FuelDefinition def) {
        FuelDefinition existing = DEFINITIONS.get(def.fuelId());
        if (existing != null && !existing.overwritable()) {
            LOGGER.debug("Skipping fuel {}: existing definition is not overwritable", def.fuelId());
            return;
        }
        DEFINITIONS.put(def.fuelId(), def);
    }

    public static FuelDefinition get(ResourceLocation fuelId) {
        return DEFINITIONS.get(fuelId);
    }

    public static Map<ResourceLocation, FuelDefinition> getAll() {
        return new HashMap<>(DEFINITIONS);
    }

    /** True if this input selector (tag or item id) is excluded ("disable: true" for these tags/items). */
    public static boolean isInputExcluded(String inputSelector) {
        return EXCLUDED_INPUTS.contains(inputSelector);
    }

    /**
     * Writes the default fuel JSON into the given directory so users can see and override the base fuel.
     * Called by the dump_default command.
     */
    public static void dumpDefaultFile(Path fuelPath) throws IOException {
        Files.createDirectories(fuelPath);
        Path file = fuelPath.resolve("default_fuel.json");
        String content = """
            {
              "type": "colossal_reactors:fuel",
              "overwritable": true,
              "entries": [
                {
                  "disable": false,
                  "fuel_id": "colossal_reactors:uranium",
                  "inputs": [
                    "#c:ingots/uranium",
                    "colossal_reactors:uranium_ingot"
                  ],
                  "output": "colossal_reactors:nuclear_waste",
                  "units_per_item": 1000,
                  "base_rf_per_tick": 200.0,
                  "base_mb_per_tick": 0.03
                }
              ]
            }
            """;
        Files.writeString(file, content);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Dumped default fuel to {}", file.toAbsolutePath());
        }
    }
}
