package net.unfamily.colossal_reactors.fuel;

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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
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
 * Loads fuel definitions from external JSON (external_scripts_path/reactor/*.json with type colossal_reactors:fuel).
 * Internal default (uranium) is registered first; JSON entries can add or override by fuel_id.
 * README with examples is auto-generated in the scripts directory on every startup.
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
    private static final String KEY_UNITS_PER_FUEL = "units_per_fuel";
    private static final String KEY_UNITS_PER_WASTE = "units_per_waste";
    private static final String KEY_BASE_RF_PER_TICK = "base_rf_per_tick";
    private static final String KEY_BASE_FUEL_UNITS_PER_TICK = "base_fuel_units_per_tick";
    private static final String KEY_BASE_MB_PER_TICK_LEGACY = "base_mb_per_tick";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_OVERWRITABLE = "overwritable";

    private static final Map<ResourceLocation, FuelDefinition> DEFINITIONS = new HashMap<>();
    /** Input selectors (tag or item id) that are excluded: not valid as fuel even if a definition would match. */
    private static final Set<String> EXCLUDED_INPUTS = new HashSet<>();

    /** Subfolder under external scripts path where all reactor JSON configs live (fuel, coolant, heat_sink). */
    private static final String REACTOR_CONFIG_DIR = "reactor";

    /**
     * Scans the reactor config directory under configured external scripts path. Internal defaults first, then JSON overrides.
     */
    public static void scanConfigDirectory() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scanning reactor config directory (fuel)...");
        }
        DEFINITIONS.clear();
        EXCLUDED_INPUTS.clear();
        registerInternalDefaults();

        String basePath = Config.EXTERNAL_SCRIPTS_PATH.get();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "kubejs/external_scripts/colossal_reactors";
        }
        Path reactorPath = Paths.get(basePath, REACTOR_CONFIG_DIR);
        if (!Files.exists(reactorPath)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Reactor config directory does not exist ({}). Using internal defaults only. README is generated in scripts directory on startup.", reactorPath.toAbsolutePath());
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
                    .forEach(FuelLoader::parseConfigFile);
        } catch (IOException e) {
            LOGGER.error("Error scanning reactor config directory: {}", e.getMessage());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Fuel definitions loaded: {}", DEFINITIONS.size());
        }
    }

    private static void registerInternalDefaults() {
        ResourceLocation uraniumId = ReactorRodBlockEntity.URANIUM_FUEL_ID;
        int unitsPerFuel = 1000;
        int unitsPerWaste = 1000;
        double baseRf = Config.BASE_RF_PER_TICK.get();
        double baseFuelUnitsPerTick = Config.BASE_FUEL_UNITS_PER_TICK.get();
        List<String> inputs = List.of("#c:ingots/uranium");
        String output = ColossalReactors.MODID + ":nuclear_waste";
        DEFINITIONS.put(uraniumId, new FuelDefinition(uraniumId, inputs, output, unitsPerFuel, unitsPerWaste, baseRf, baseFuelUnitsPerTick, true));

        // Azurite: 500 base RF, 500 fuel units per ingot, 1500 consumed units per 1 waste (nuclear_waste)
        ResourceLocation azuriteId = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "azurite");
        DEFINITIONS.put(azuriteId, new FuelDefinition(
                azuriteId,
                List.of("#c:ingots/azurite"),
                ColossalReactors.MODID + ":nuclear_waste",
                500,
                1500,
                500.0,
                baseFuelUnitsPerTick,
                true
        ));
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
        int unitsPerFuel = 1000;
        int unitsPerWaste = 1000;
        if (json.has(KEY_UNITS_PER_FUEL)) unitsPerFuel = json.get(KEY_UNITS_PER_FUEL).getAsInt();
        else if (json.has(KEY_UNITS_PER_ITEM)) unitsPerFuel = json.get(KEY_UNITS_PER_ITEM).getAsInt();
        if (json.has(KEY_UNITS_PER_WASTE)) unitsPerWaste = json.get(KEY_UNITS_PER_WASTE).getAsInt();
        else if (json.has(KEY_UNITS_PER_ITEM)) unitsPerWaste = json.get(KEY_UNITS_PER_ITEM).getAsInt();
        double baseRf = json.has(KEY_BASE_RF_PER_TICK) ? json.get(KEY_BASE_RF_PER_TICK).getAsDouble() : Config.BASE_RF_PER_TICK.get();
        double baseFuelUnitsPerTick = json.has(KEY_BASE_FUEL_UNITS_PER_TICK) ? json.get(KEY_BASE_FUEL_UNITS_PER_TICK).getAsDouble()
                : json.has(KEY_BASE_MB_PER_TICK_LEGACY) ? json.get(KEY_BASE_MB_PER_TICK_LEGACY).getAsDouble()
                : Config.BASE_FUEL_UNITS_PER_TICK.get();
        boolean overwritable = json.has(KEY_OVERWRITABLE) ? json.get(KEY_OVERWRITABLE).getAsBoolean() : defaultOverwritable;
        return new FuelDefinition(fuelId, inputs.isEmpty() ? List.of(fuelId.toString()) : List.copyOf(inputs), output, unitsPerFuel, unitsPerWaste, baseRf, baseFuelUnitsPerTick, overwritable);
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
     * Finds the fuel definition that matches the given item (by item id or item tag). Returns null if excluded or no match.
     * Prefers exact item id match over tag match so e.g. uranium_ingot gets the uranium definition (with correct unitsPerFuel).
     */
    @Nullable
    public static FuelDefinition getDefinitionForItem(ItemStack stack, RegistryAccess registryAccess) {
        if (stack == null || stack.isEmpty()) return null;
        Item item = stack.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        FuelDefinition tagMatch = null;
        for (FuelDefinition def : DEFINITIONS.values()) {
            for (String input : def.inputs()) {
                if (isInputExcluded(input)) continue;
                if (input.startsWith("#")) {
                    ResourceLocation tagId = ResourceLocation.tryParse(input.substring(1));
                    if (tagId == null) continue;
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
                    var itemHolder = registryAccess.registryOrThrow(Registries.ITEM).getHolder(ResourceKey.create(Registries.ITEM, itemId)).orElse(null);
                    if (itemHolder == null) continue;
                    boolean inTag = registryAccess.lookup(Registries.ITEM)
                            .flatMap(l -> l.get(tagKey))
                            .map(holders -> holders.contains(itemHolder))
                            .orElse(false);
                    if (inTag) tagMatch = def;
                } else {
                    if (ResourceLocation.tryParse(input).equals(itemId)) return def;
                }
            }
        }
        return tagMatch;
    }

    /**
     * Returns a single item stack for the first valid input of this fuel type (for eject: convert fuel units back to items).
     * Caller must use definition's unitsPerFuel when converting fuel units back to item count.
     */
    public static ItemStack getFirstInputStack(ResourceLocation fuelId, RegistryAccess registryAccess) {
        FuelDefinition def = DEFINITIONS.get(fuelId);
        if (def == null || def.inputs().isEmpty()) return ItemStack.EMPTY;
        for (String input : def.inputs()) {
            if (isInputExcluded(input)) continue;
            if (input.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(input.substring(1));
                if (tagId == null) continue;
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
                var optItem = registryAccess.lookup(Registries.ITEM)
                        .flatMap(l -> l.get(tagKey))
                        .stream()
                        .flatMap(holders -> holders.stream())
                        .findFirst()
                        .map(h -> h.value());
                if (optItem.isPresent()) return new ItemStack(optItem.get(), 1);
            } else {
                ResourceLocation id = ResourceLocation.tryParse(input);
                if (id != null) {
                    Item item = BuiltInRegistries.ITEM.get(id);
                    if (item != null && item != net.minecraft.world.item.Items.AIR) return new ItemStack(item, 1);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns a single item stack for the waste output of this fuel type (for tooltip display).
     * Output is either "#tag" (first item in tag) or "namespace:item_id".
     */
    public static ItemStack getFirstOutputStack(ResourceLocation fuelId, RegistryAccess registryAccess) {
        FuelDefinition def = DEFINITIONS.get(fuelId);
        if (def == null) return ItemStack.EMPTY;
        String output = def.output();
        if (output == null || output.isEmpty()) return ItemStack.EMPTY;
        if (output.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(output.substring(1));
            if (tagId == null) return ItemStack.EMPTY;
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            var optItem = registryAccess.lookup(Registries.ITEM)
                    .flatMap(l -> l.get(tagKey))
                    .stream()
                    .flatMap(holders -> holders.stream())
                    .findFirst()
                    .map(h -> h.value());
            if (optItem.isPresent()) return new ItemStack(optItem.get(), 1);
        } else {
            ResourceLocation id = ResourceLocation.tryParse(output);
            if (id != null) {
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) return new ItemStack(item, 1);
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Writes the default fuel JSON into the given reactor directory. Called only by /colossal_reactors dump.
     * Edits to the file override internal defaults when the mod loads.
     */
    public static void dumpDefaultFile(Path reactorDir) throws IOException {
        Files.createDirectories(reactorDir);
        Path file = reactorDir.resolve("default_fuel.json");
        String content = """
            {
              "type": "colossal_reactors:fuel",
              "entries": [
                {
                  "disable": false,
                  "fuel_id": "colossal_reactors:uranium",
                  "inputs": ["#c:ingots/uranium"],
                  "output": "colossal_reactors:nuclear_waste",
                  "units_per_fuel": 1000,
                  "units_per_waste": 1000,
                  "base_rf_per_tick": 200.0,
                  "base_fuel_units_per_tick": 0.03
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
