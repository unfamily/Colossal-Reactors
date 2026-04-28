package net.unfamily.colossal_reactors.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilDefinition;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilLoader;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Loads data from the fixed path data/&lt;namespace&gt;/load/ (and subdirs).
 * Only constant is "load" in the path; namespace and filenames are free.
 * Parses each JSON; if type is colossal_reactors:heating_coils, merges coil definitions.
 */
public class LoadDataReloadListener implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoadDataReloadListener.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final String LOAD_PATH = "load";
    private static final String TYPE_HEATING_COILS = "colossal_reactors:heating_coils";
    private static final String KEY_TYPE = "type";

    @Override
    public String getName() {
        return "colossal_reactors_load_data";
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
                                          Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            prepareProfiler.push("Colossal Reactors load data");
            Map<Identifier, HeatingCoilDefinition> coils = new HashMap<>();
            Map<Identifier, List<Resource>> stacks = resourceManager.listResourceStacks(LOAD_PATH,
                    rl -> rl.getPath().endsWith(".json"));
            for (Map.Entry<Identifier, List<Resource>> entry : stacks.entrySet()) {
                Identifier location = entry.getKey();
                for (Resource resource : entry.getValue()) {
                    processOne(location, resource, coils);
                }
            }
            // Fallback: ensure mod's builtin path is loaded if listResourceStacks did not find it
            Identifier builtinLocation = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "load/heating_coils.json");
            if (coils.isEmpty() || !stacks.containsKey(builtinLocation)) {
                try {
                    List<Resource> builtinStack = resourceManager.getResourceStack(builtinLocation);
                    for (Resource resource : builtinStack) {
                        processOne(builtinLocation, resource, coils);
                    }
                } catch (Exception e) {
                    LOGGER.debug("Could not load builtin heating_coils.json: {}", e.getMessage());
                }
            }
            prepareProfiler.pop();
            return coils;
        }, prepareExecutor).thenCompose(stage::wait).thenAcceptAsync(coils -> {
            applyProfiler.push("Colossal Reactors apply load data");
            HeatingCoilRegistry.setFromReload(coils);
            applyProfiler.pop();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Load data: {} heating coil definition(s)", coils.size());
            }
        }, applyExecutor);
    }

    private static void processOne(Identifier location, Resource resource,
                                  Map<Identifier, HeatingCoilDefinition> coils) {
        String source = location + " from " + resource.sourcePackId();
        try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has(KEY_TYPE)) return;
            if (!TYPE_HEATING_COILS.equals(root.get(KEY_TYPE).getAsString())) return;
            List<HeatingCoilDefinition> list = HeatingCoilLoader.parseFromRoot(root, source);
            for (HeatingCoilDefinition def : list) {
                coils.put(def.id(), def);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load {} from {}: {}", location, resource.sourcePackId(), e.getMessage());
        }
    }
}
