package net.unfamily.colossal_reactors.fluid;

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
import net.unfamily.colossal_reactors.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Loads fluid-to-color associations from JSON (reactor/*.json with type colossal_reactors:fluid_colors).
 * Used for GUI fluid bar rendering. Separate from coolant definitions so colors can be managed in one place.
 * Coolant definitions' fluid_color/output_color are still used as fallback when no fluid_colors entry matches.
 */
public final class FluidColorLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(FluidColorLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String TYPE_FLUID_COLORS = "colossal_reactors:fluid_colors";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_FLUID = "fluid";
    private static final String KEY_COLOR = "color";

    private static final int DEFAULT_WATER_ARGB = 0xFF3498DB;
    private static final int DEFAULT_STEAM_ARGB = 0xFFE8F0F0;

    /** Order matters: first matching entry wins. */
    private static final List<FluidColorEntry> ENTRIES = new ArrayList<>();

    private FluidColorLoader() {}

    /**
     * Scans the reactor config directory for fluid_colors JSON. Internal defaults are used when no file is present.
     */
    public static void scanConfigDirectory() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scanning reactor config directory (fluid colors)...");
        }
        ENTRIES.clear();
        registerInternalDefaults();

        String basePath = Config.EXTERNAL_SCRIPTS_PATH.get();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "kubejs/external_scripts/colossal_reactors";
        }
        Path reactorPath = Paths.get(basePath, "reactor");
        if (!Files.exists(reactorPath) || !Files.isDirectory(reactorPath)) {
            return;
        }
        try (Stream<Path> files = Files.walk(reactorPath)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .sorted()
                    .forEach(FluidColorLoader::parseConfigFile);
        } catch (IOException e) {
            LOGGER.error("Error scanning reactor directory for fluid colors: {}", e.getMessage());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Fluid color entries loaded: {}", ENTRIES.size());
        }
    }

    private static void registerInternalDefaults() {
        ENTRIES.add(new FluidColorEntry("#c:water", DEFAULT_WATER_ARGB));
        ENTRIES.add(new FluidColorEntry("#c:steam", DEFAULT_STEAM_ARGB));
    }

    private static void parseConfigFile(Path filePath) {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonElement el = GSON.fromJson(reader, JsonElement.class);
            if (el == null || !el.isJsonObject()) return;
            JsonObject root = el.getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "";
            if (!TYPE_FLUID_COLORS.equals(type)) return;
            if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) {
                LOGGER.warn("Fluid colors config {}: missing or invalid 'entries' array", filePath.getFileName());
                return;
            }
            for (JsonElement e : root.getAsJsonArray(KEY_ENTRIES)) {
                if (!e.isJsonObject()) continue;
                FluidColorEntry entry = parseEntry(e.getAsJsonObject(), filePath.toString());
                if (entry != null) ENTRIES.add(entry);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing fluid colors file {}: {}", filePath, e.getMessage());
        }
    }

    private static FluidColorEntry parseEntry(JsonObject json, String filePath) {
        if (!json.has(KEY_FLUID) || !json.has(KEY_COLOR)) {
            LOGGER.warn("Fluid color entry in {}: missing 'fluid' or 'color'", filePath);
            return null;
        }
        String selector = json.get(KEY_FLUID).getAsString();
        int color = parseColor(json, KEY_COLOR);
        if (color == 0) {
            LOGGER.warn("Fluid color entry in {}: invalid color for fluid '{}'", filePath, selector);
            return null;
        }
        return new FluidColorEntry(selector, color);
    }

    private static int parseColor(JsonObject json, String key) {
        if (!json.has(key)) return 0;
        JsonElement el = json.get(key);
        if (el.isJsonPrimitive()) {
            if (el.getAsJsonPrimitive().isString()) {
                String hex = el.getAsString();
                if (hex.startsWith("#") && hex.length() >= 7) {
                    try {
                        int rgb = Integer.parseInt(hex.substring(1), 16);
                        return 0xFF000000 | (rgb & 0xFFFFFF);
                    } catch (NumberFormatException ignored) {}
                }
            } else if (el.getAsJsonPrimitive().isNumber()) {
                return el.getAsInt() & 0xFFFFFFFF;
            }
        }
        return 0;
    }

    /**
     * Returns ARGB color for the given fluid from fluid_colors entries. First matching entry wins.
     * Returns 0 when no entry matches (caller may fall back to coolant definition or default).
     */
    public static int getColorForFluid(Fluid fluid, RegistryAccess registryAccess) {
        if (fluid == null || fluid == Fluids.EMPTY) return 0;
        for (FluidColorEntry entry : ENTRIES) {
            if (fluidMatchesSelector(fluid, entry.selector(), registryAccess)) {
                return entry.color();
            }
        }
        return 0;
    }

    private static boolean fluidMatchesSelector(Fluid fluid, String selector, RegistryAccess registryAccess) {
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return false;
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
            var holder = registryAccess.registryOrThrow(Registries.FLUID).getHolder(ResourceKey.create(Registries.FLUID, fluidId));
            if (holder.isEmpty()) return false;
            return registryAccess.lookup(Registries.FLUID)
                    .flatMap(l -> l.get(tagKey))
                    .map(holders -> holders.contains(holder.get()))
                    .orElse(false);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        return id != null && id.equals(fluidId);
    }

    /**
     * Writes the default fluid_colors JSON into the given reactor directory. Called by /colossal_reactors dump.
     */
    public static void dumpDefaultFile(Path reactorDir) throws IOException {
        Files.createDirectories(reactorDir);
        Path file = reactorDir.resolve("default_fluid_colors.json");
        String content = """
            {
              "type": "colossal_reactors:fluid_colors",
              "entries": [
                { "fluid": "#c:water", "color": "#3498db" },
                { "fluid": "#c:steam", "color": "#e8f0f0" }
              ]
            }
            """;
        Files.writeString(file, content);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Dumped default fluid colors to {}", file.toAbsolutePath());
        }
    }

    private record FluidColorEntry(String selector, int color) {}
}
