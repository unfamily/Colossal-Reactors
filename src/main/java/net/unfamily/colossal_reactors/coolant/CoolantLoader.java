package net.unfamily.colossal_reactors.coolant;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads coolant definitions from external JSON (external_scripts_path/coolant/*.json).
 * Internal default: water (minecraft:water) → output #c:steam. JSON entries can add or override by coolant_id.
 * Output by tag: we store the tag; at runtime only if the tag resolves to a valid fluid do we produce output.
 */
public class CoolantLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(CoolantLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String TYPE_COOLANT = "colossal_reactors:coolant";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_DISABLE = "disable";
    private static final String KEY_COOLANT_ID = "coolant_id";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_OVERWRITABLE = "overwritable";

    private static final Map<ResourceLocation, CoolantDefinition> DEFINITIONS = new HashMap<>();

    /** Default coolant id for water. */
    public static final ResourceLocation WATER_COOLANT_ID = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "water");

    /**
     * Scans the coolant directory under configured external scripts path. Internal defaults first, then JSON overrides.
     */
    public static void scanConfigDirectory() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scanning coolant configuration directory...");
        }
        DEFINITIONS.clear();
        registerInternalDefaults();

        String basePath = Config.EXTERNAL_SCRIPTS_PATH.get();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "kubejs/external_scripts/colossal_reactors";
        }
        Path coolantPath = Paths.get(basePath, "coolant");
        if (!Files.exists(coolantPath)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Coolant directory does not exist ({}). Using internal defaults only. Run dump_default to create it.", coolantPath.toAbsolutePath());
            }
            return;
        }
        if (!Files.isDirectory(coolantPath)) {
            LOGGER.warn("Coolant path is not a directory: {}", coolantPath);
            return;
        }
        try (Stream<Path> files = Files.walk(coolantPath)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .sorted()
                    .forEach(CoolantLoader::parseConfigFile);
        } catch (IOException e) {
            LOGGER.error("Error scanning coolant directory: {}", e.getMessage());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Coolant definitions loaded: {}", DEFINITIONS.size());
        }
    }

    private static void registerInternalDefaults() {
        List<String> inputs = List.of("minecraft:water");
        String output = "#c:steam";
        DEFINITIONS.put(WATER_COOLANT_ID, new CoolantDefinition(WATER_COOLANT_ID, inputs, output, true));
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
                if (obj.has(KEY_DISABLE) && obj.get(KEY_DISABLE).getAsBoolean()) continue;
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
        boolean overwritable = json.has(KEY_OVERWRITABLE) ? json.get(KEY_OVERWRITABLE).getAsBoolean() : defaultOverwritable;
        return new CoolantDefinition(coolantId, inputs.isEmpty() ? List.of(coolantId.toString()) : List.copyOf(inputs), output, overwritable);
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

    /**
     * Writes the default coolant JSON into the given directory. Called by dump_default command.
     */
    public static void dumpDefaultFile(Path coolantPath) throws IOException {
        Files.createDirectories(coolantPath);
        Path file = coolantPath.resolve("default_coolant.json");
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
                  "output": "#c:steam"
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
