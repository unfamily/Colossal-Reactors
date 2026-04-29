package net.unfamily.colossal_reactors.heatingcoil;

import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry of heating coil definitions by id. Builtin definitions loaded at init from mod jar;
 * datapack reload merges from data/&lt;namespace&gt;/load/*.json (later overrides).
 */
public final class HeatingCoilRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatingCoilRegistry.class);
    private static final String BUILTIN_PATH = "data/colossal_reactors/load/heating_coils.json";

    private static final Map<Identifier, HeatingCoilDefinition> DEFINITIONS = new HashMap<>();
    private static List<Identifier> builtinCoilIds;

    private HeatingCoilRegistry() {}

    /** Parses {@value #BUILTIN_PATH} from the mod jar (same content used at init and after reload merge). */
    private static List<HeatingCoilDefinition> parseBuiltinFile() {
        try (var stream = ColossalReactors.class.getResourceAsStream("/" + BUILTIN_PATH)) {
            if (stream == null) {
                LOGGER.warn("Builtin heating coils not found: {}", BUILTIN_PATH);
                return List.of();
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return HeatingCoilLoader.parse(reader, "builtin");
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load builtin heating coils: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Loads builtin heating_coils.json from the mod jar and caches coil ids for block registration.
     * Call once at mod init (before registering blocks).
     */
    public static synchronized List<Identifier> getBuiltinCoilIds() {
        if (builtinCoilIds != null) return builtinCoilIds;
        List<HeatingCoilDefinition> list = parseBuiltinFile();
        if (list.isEmpty()) {
            builtinCoilIds = List.of();
            return builtinCoilIds;
        }
        for (HeatingCoilDefinition def : list) {
            DEFINITIONS.put(def.id(), def);
        }
        builtinCoilIds = list.stream().map(HeatingCoilDefinition::id).toList();
        LOGGER.info("Loaded {} builtin heating coil(s)", builtinCoilIds.size());
        return new ArrayList<>(builtinCoilIds);
    }

    /**
     * Applies datapack reload (called by LoadDataReloadListener).
     * Builtin jar definitions are merged first, then {@code loaded} overrides per id — same id from datapack wins.
     * This avoids losing flags like {@code all_sides} when reload aggregation omits or partially replaces entries.
     */
    public static synchronized void setFromReload(Map<Identifier, HeatingCoilDefinition> loaded) {
        if (loaded == null || loaded.isEmpty()) {
            LOGGER.debug("Load data reload: no heating coil definitions, keeping existing registry");
            return;
        }
        Map<Identifier, HeatingCoilDefinition> merged = new HashMap<>();
        for (HeatingCoilDefinition def : parseBuiltinFile()) {
            merged.put(def.id(), def);
        }
        merged.putAll(loaded);
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
