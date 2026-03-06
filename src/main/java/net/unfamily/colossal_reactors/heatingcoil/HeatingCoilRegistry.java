package net.unfamily.colossal_reactors.heatingcoil;

import net.minecraft.resources.ResourceLocation;
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

    private static final Map<ResourceLocation, HeatingCoilDefinition> DEFINITIONS = new HashMap<>();
    private static List<ResourceLocation> builtinCoilIds;

    private HeatingCoilRegistry() {}

    /**
     * Loads builtin heating_coils.json from the mod jar and caches coil ids for block registration.
     * Call once at mod init (before registering blocks).
     */
    public static synchronized List<ResourceLocation> getBuiltinCoilIds() {
        if (builtinCoilIds != null) return builtinCoilIds;
        try (var stream = ColossalReactors.class.getResourceAsStream("/" + BUILTIN_PATH)) {
            if (stream == null) {
                LOGGER.warn("Builtin heating coils not found: {}", BUILTIN_PATH);
                builtinCoilIds = List.of();
                return builtinCoilIds;
            }
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                List<HeatingCoilDefinition> list = HeatingCoilLoader.parse(reader, "builtin");
                for (HeatingCoilDefinition def : list) {
                    DEFINITIONS.put(def.id(), def);
                }
                builtinCoilIds = list.stream().map(HeatingCoilDefinition::id).toList();
                LOGGER.info("Loaded {} builtin heating coil(s)", builtinCoilIds.size());
                return new ArrayList<>(builtinCoilIds);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load builtin heating coils: {}", e.getMessage());
            builtinCoilIds = List.of();
            return builtinCoilIds;
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
        DEFINITIONS.clear();
        DEFINITIONS.putAll(loaded);
    }

    @Nullable
    public static HeatingCoilDefinition get(ResourceLocation id) {
        return DEFINITIONS.get(id);
    }

    public static Map<ResourceLocation, HeatingCoilDefinition> getAll() {
        return Collections.unmodifiableMap(new HashMap<>(DEFINITIONS));
    }
}
