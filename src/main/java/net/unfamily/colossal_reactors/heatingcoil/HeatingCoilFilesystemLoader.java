package net.unfamily.colossal_reactors.heatingcoil;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.unfamily.iskalib.load.LoadFilesystemBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads heating coil JSON from Library bootstrap datapack roots before block registration.
 * Uses the shared {@code iska_lib} path list (KubeJS, Open Loader, Global Packs, datapacks).
 */
public final class HeatingCoilFilesystemLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(HeatingCoilFilesystemLoader.class);
    private static final String TYPE_HEATING_COILS = "colossal_reactors:heating_coils";

    private HeatingCoilFilesystemLoader() {}

    public static Map<Identifier, HeatingCoilDefinition> loadFromGameDir() {
        Map<Identifier, JsonElement> jsons = new LinkedHashMap<>();
        int files = LoadFilesystemBootstrap.mergeIntoForType(jsons, TYPE_HEATING_COILS);
        Map<Identifier, HeatingCoilDefinition> merged = new LinkedHashMap<>();
        for (Map.Entry<Identifier, JsonElement> entry : jsons.entrySet()) {
            JsonElement raw = entry.getValue();
            if (raw == null || !raw.isJsonObject()) {
                continue;
            }
            JsonObject root = raw.getAsJsonObject();
            List<HeatingCoilDefinition> list =
                    HeatingCoilLoader.parseFromRoot(root, "filesystem:" + entry.getKey());
            for (HeatingCoilDefinition def : list) {
                merged.put(def.id(), def);
            }
        }
        if (files > 0) {
            LOGGER.info("Heating coils from game dir ({} file(s), {} definition(s))", files, merged.size());
        }
        return merged;
    }
}
