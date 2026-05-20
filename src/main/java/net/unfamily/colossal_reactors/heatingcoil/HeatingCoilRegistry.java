package net.unfamily.colossal_reactors.heatingcoil;

import net.minecraft.resources.ResourceLocation;
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
 * datapack reload merges from data/&lt;namespace&gt;/load/*.json (later overrides).
 */
public final class HeatingCoilRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatingCoilRegistry.class);
    private static final String BUILTIN_PATH = "data/colossal_reactors/load/heating_coils.json";

    private static final Map<ResourceLocation, HeatingCoilDefinition> DEFINITIONS = new HashMap<>();
    private static List<ResourceLocation> builtinCoilIds;

    private HeatingCoilRegistry() {}

    /**
     * Loads builtin heating_coils.json from the mod jar and caches coil ids for block registration.
     * Call once at mod init (before registering blocks).
     */
    public static synchronized List<ResourceLocation> getBuiltinCoilIds() {
        if (builtinCoilIds != null) return builtinCoilIds;
        Map<ResourceLocation, HeatingCoilDefinition> merged = new LinkedHashMap<>();
        for (HeatingCoilDefinition def : parseBuiltinFile()) {
            merged.put(def.id(), DatapackSelectorValidator.sanitizeHeatingCoil(def));
        }
        int jarCount = merged.size();
        for (var entry : HeatingCoilFilesystemLoader.loadFromGameDir().entrySet()) {
            merged.put(entry.getKey(), DatapackSelectorValidator.sanitizeHeatingCoil(entry.getValue()));
        }
        DEFINITIONS.putAll(merged);
        builtinCoilIds = List.copyOf(merged.keySet());
        LOGGER.info("Heating coils for block registration: {} (jar={}, external={})",
                builtinCoilIds.size(), jarCount, merged.size() - jarCount);
        return new ArrayList<>(builtinCoilIds);
    }

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
     * Replaces definitions from datapack reload (called by LoadDataReloadListener).
     * Only applies when loaded is non-empty so we never wipe the registry (builtin stays if reload finds no files).
     */
    public static synchronized void setFromReload(Map<ResourceLocation, HeatingCoilDefinition> loaded) {
        if (loaded == null || loaded.isEmpty()) {
            LOGGER.debug("Load data reload: no heating coil definitions, keeping existing registry");
            return;
        }
        Map<ResourceLocation, HeatingCoilDefinition> merged = new HashMap<>();
        for (HeatingCoilDefinition def : parseBuiltinFile()) {
            merged.put(def.id(), def);
        }
        for (HeatingCoilDefinition def : loaded.values()) {
            merged.put(def.id(), DatapackSelectorValidator.sanitizeHeatingCoil(def));
        }
        DEFINITIONS.clear();
        DEFINITIONS.putAll(merged);
    }

    @Nullable
    public static HeatingCoilDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    public static Map<ResourceLocation, HeatingCoilDefinition> getAll() {
        return Collections.unmodifiableMap(new HashMap<>(DEFINITIONS));
    }
}
