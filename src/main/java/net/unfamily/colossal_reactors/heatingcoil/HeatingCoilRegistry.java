package net.unfamily.colossal_reactors.heatingcoil;

import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.datapack.DatapackSelectorValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of heating coil definitions by id. Builtin definitions loaded at init from mod jar;
 * datapack reload merges from recipe + {@code data/<namespace>/load/} retrocompat (later overrides).
 */
public final class HeatingCoilRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatingCoilRegistry.class);
    private static final String BUILTIN_PATH = "data/colossal_reactors/recipe/heating_coils.json";
    private static final String BUILTIN_PATH_LEGACY = "data/colossal_reactors/load/heating_coils.json";

    private static final Map<Identifier, HeatingCoilDefinition> DEFINITIONS = new HashMap<>();
    private static List<Identifier> builtinCoilIds;

    private HeatingCoilRegistry() {}

    /**
     * Loads builtin heating_coils.json from the mod jar and caches coil ids for block registration.
     * Call once at mod init (before registering blocks).
     */
    public static synchronized List<Identifier> getBuiltinCoilIds() {
        if (builtinCoilIds != null) return builtinCoilIds;
        Map<Identifier, HeatingCoilDefinition> merged = new LinkedHashMap<>();
        for (HeatingCoilDefinition def : parseBuiltinFile()) {
            putSanitized(merged, def);
        }
        int jarCount = merged.size();
        for (var entry : HeatingCoilFilesystemLoader.loadFromGameDir().entrySet()) {
            putSanitized(merged, entry.getValue());
        }
        DEFINITIONS.putAll(merged);
        builtinCoilIds = List.copyOf(merged.keySet());
        LOGGER.info("Heating coils for block registration: {} (jar={}, external={})",
                builtinCoilIds.size(), jarCount, merged.size() - jarCount);
        return new ArrayList<>(builtinCoilIds);
    }

    private static void putSanitized(Map<Identifier, HeatingCoilDefinition> merged, HeatingCoilDefinition def) {
        putSanitized(merged, def.id(), def);
    }

    private static void putSanitized(Map<Identifier, HeatingCoilDefinition> merged, Identifier id, HeatingCoilDefinition def) {
        if (!DatapackSelectorValidator.registriesReady()) {
            merged.put(id, def);
            return;
        }
        HeatingCoilDefinition sanitized = DatapackSelectorValidator.sanitizeHeatingCoil(def);
        merged.put(id, sanitized != null ? sanitized : def);
    }

    private static List<HeatingCoilDefinition> parseBuiltinFile() {
        List<HeatingCoilDefinition> fromRecipe = parseBuiltinAt(BUILTIN_PATH);
        if (!fromRecipe.isEmpty()) {
            return fromRecipe;
        }
        return parseBuiltinAt(BUILTIN_PATH_LEGACY);
    }

    private static List<HeatingCoilDefinition> parseBuiltinAt(String path) {
        try (var stream = ColossalReactors.class.getResourceAsStream("/" + path)) {
            if (stream == null) {
                return List.of();
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return HeatingCoilLoader.parse(reader, "builtin:" + path);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load builtin heating coils from {}: {}", path, e.getMessage());
            return List.of();
        }
    }

    /**
     * Replaces definitions from datapack reload (called by ColossalRecipeData and client refresh).
     * Only applies when loaded is non-empty so we never wipe the registry (builtin stays if reload finds no files).
     */
    public static synchronized void setFromReload(Map<Identifier, HeatingCoilDefinition> loaded) {
        if (loaded == null || loaded.isEmpty()) {
            LOGGER.debug("Load data reload: no heating coil definitions, keeping existing registry");
            return;
        }
        Map<Identifier, HeatingCoilDefinition> merged = new HashMap<>();
        for (HeatingCoilDefinition def : parseBuiltinFile()) {
            putSanitized(merged, def);
        }
        for (HeatingCoilDefinition def : loaded.values()) {
            putSanitized(merged, def);
        }
        DEFINITIONS.clear();
        DEFINITIONS.putAll(merged);
    }

    @Nullable
    public static HeatingCoilDefinition get(Identifier id) {
        return DEFINITIONS.get(id);
    }

    public static Map<Identifier, HeatingCoilDefinition> getAll() {
        return Collections.unmodifiableMap(new HashMap<>(DEFINITIONS));
    }
}
