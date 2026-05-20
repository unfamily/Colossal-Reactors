package net.unfamily.colossal_reactors.heatingcoil;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Loads heating coil JSON from folders on disk before block registration.
 * Picks up KubeJS ({@code kubejs/data/&lt;namespace&gt;/load/*.json}) and world datapacks
 * ({@code datapacks/&lt;pack&gt;/data/&lt;namespace&gt;/load/*.json}) that are not in the mod jar yet.
 */
public final class HeatingCoilFilesystemLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatingCoilFilesystemLoader.class);
    private static final String TYPE_HEATING_COILS = "colossal_reactors:heating_coils";

    private HeatingCoilFilesystemLoader() {}

    /**
     * Merged definitions from external files (later files override same coil id).
     */
    public static Map<ResourceLocation, HeatingCoilDefinition> loadFromGameDir() {
        Path gameDir = FMLPaths.GAMEDIR.get();
        Map<ResourceLocation, HeatingCoilDefinition> merged = new LinkedHashMap<>();
        int files = 0;
        files += scanLoadJsonUnder(gameDir.resolve("kubejs/data"), merged);
        files += scanDatapackRoots(gameDir.resolve("datapacks"), merged);
        if (files > 0) {
            LOGGER.info("Heating coils from game dir ({} file(s), {} definition(s))", files, merged.size());
        }
        return merged;
    }

    private static int scanDatapackRoots(Path datapacksDir, Map<ResourceLocation, HeatingCoilDefinition> merged) {
        if (!Files.isDirectory(datapacksDir)) return 0;
        int count = 0;
        try (Stream<Path> packs = Files.list(datapacksDir)) {
            for (Path pack : packs.toList()) {
                if (!Files.isDirectory(pack)) continue;
                count += scanLoadJsonUnder(pack.resolve("data"), merged);
            }
        } catch (IOException e) {
            LOGGER.debug("Could not list datapacks folder: {}", e.getMessage());
        }
        return count;
    }

    private static int scanLoadJsonUnder(Path dataRoot, Map<ResourceLocation, HeatingCoilDefinition> merged) {
        if (!Files.isDirectory(dataRoot)) return 0;
        int count = 0;
        try (Stream<Path> walk = Files.walk(dataRoot)) {
            for (Path file : walk.filter(HeatingCoilFilesystemLoader::isLoadJson).toList()) {
                if (parseFile(file, merged)) count++;
            }
        } catch (IOException e) {
            LOGGER.debug("Could not scan {}: {}", dataRoot, e.getMessage());
        }
        return count;
    }

    private static boolean isLoadJson(Path file) {
        if (!Files.isRegularFile(file)) return false;
        String path = file.toString().replace('\\', '/');
        return path.endsWith(".json") && path.contains("/load/");
    }

    private static boolean parseFile(Path file, Map<ResourceLocation, HeatingCoilDefinition> merged) {
        String source = "filesystem:" + file;
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            if (root == null || !root.has("type")) return false;
            if (!TYPE_HEATING_COILS.equals(root.get("type").getAsString())) return false;
            List<HeatingCoilDefinition> list = HeatingCoilLoader.parseFromRoot(root, source);
            for (HeatingCoilDefinition def : list) {
                merged.put(def.id(), def);
            }
            return !list.isEmpty();
        } catch (Exception e) {
            LOGGER.warn("Failed to load heating coils from {}: {}", file, e.getMessage());
            return false;
        }
    }
}
