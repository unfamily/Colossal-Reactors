package net.unfamily.colossal_reactors.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Loads reactor fuel, coolant and heat sink definitions from datapack JSON.
 * Scans all namespaces under the standard path: data/&lt;namespace&gt;/recipe/ (and any subdirs).
 * The only constant is "recipe" in the path; namespace and subdirectories are free (e.g.
 * colossal_reactors/recipe/, kubejs/recipe/fuels/, mymod/recipe/reactor/). The "type" field in each
 * JSON decides the kind: "colossal_reactors:fuel", "colossal_reactors:coolant", "colossal_reactors:heat_sinks".
 * Merges from all datapacks; later packs override by fuel_id/coolant_id.
 */
public class ReactorDataReloadListener implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorDataReloadListener.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final String TYPE_FUEL = "colossal_reactors:fuel";
    private static final String TYPE_COOLANT = "colossal_reactors:coolant";
    private static final String TYPE_HEAT_SINKS = "colossal_reactors:heat_sinks";
    private static final String KEY_TYPE = "type";
    private static final String KEY_ENTRIES = "entries";

    /** Path prefix: only constant is "recipe"; scans data/&lt;any_namespace&gt;/recipe/ and all subdirs. */
    private static final String REACTOR_DATA_PATH = "recipe";

    @Override
    public String getName() {
        return ColossalReactors.MODID + "_reactor_data";
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier stage, ResourceManager resourceManager,
                                          ProfilerFiller prepareProfiler, ProfilerFiller applyProfiler,
                                          Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            prepareProfiler.push("Colossal Reactors reactor data");
            Map<ResourceLocation, FuelDefinition> fuel = new HashMap<>();
            Map<ResourceLocation, CoolantDefinition> coolant = new HashMap<>();
            List<HeatSinkDefinition> heatSinks = new ArrayList<>();
            Map<ResourceLocation, List<Resource>> stacks = resourceManager.listResourceStacks(REACTOR_DATA_PATH,
                    rl -> rl.getPath().endsWith(".json"));
            for (Map.Entry<ResourceLocation, List<Resource>> entry : stacks.entrySet()) {
                ResourceLocation location = entry.getKey();
                for (Resource resource : entry.getValue()) {
                    processOneResource(location, resource, fuel, coolant, heatSinks);
                }
            }
            prepareProfiler.pop();
            return new LoadedData(fuel, coolant, heatSinks);
        }, prepareExecutor).thenCompose(stage::wait).thenAcceptAsync(data -> {
            applyProfiler.push("Colossal Reactors apply");
            FuelLoader.applyLoaded(data.fuel());
            CoolantLoader.applyLoaded(data.coolant());
            HeatSinkLoader.applyLoaded(data.heatSinks());
            applyProfiler.pop();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Reactor data loaded: {} fuel, {} coolant, {} heat sink entries",
                        data.fuel().size(), data.coolant().size(), data.heatSinks().size());
            }
        }, applyExecutor);
    }

    private static void processOneResource(ResourceLocation location, Resource resource,
                                          Map<ResourceLocation, FuelDefinition> fuel,
                                          Map<ResourceLocation, CoolantDefinition> coolant,
                                          List<HeatSinkDefinition> heatSinks) {
        String source = location + " from " + resource.sourcePackId();
        try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has(KEY_TYPE)) return;
            String type = root.get(KEY_TYPE).getAsString();
            if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) return;
            JsonArray entries = root.getAsJsonArray(KEY_ENTRIES);

            if (TYPE_FUEL.equals(type)) {
                boolean overwritable = !root.has("overwritable") || root.get("overwritable").getAsBoolean();
                for (JsonElement e : entries) {
                    if (!e.isJsonObject()) continue;
                    FuelDefinition def = FuelLoader.parseEntry(e.getAsJsonObject(), source, overwritable);
                    if (def != null) fuel.put(def.fuelId(), def);
                }
            } else if (TYPE_COOLANT.equals(type)) {
                boolean overwritable = !root.has("overwritable") || root.get("overwritable").getAsBoolean();
                for (JsonElement e : entries) {
                    if (!e.isJsonObject()) continue;
                    CoolantDefinition def = CoolantLoader.parseEntry(e.getAsJsonObject(), source, overwritable);
                    if (def != null) coolant.put(def.coolantId(), def);
                }
            } else if (TYPE_HEAT_SINKS.equals(type)) {
                if (root.has("overwrite") && root.get("overwrite").getAsBoolean()) heatSinks.clear();
                for (JsonElement e : entries) {
                    if (!e.isJsonObject()) continue;
                    HeatSinkDefinition def = HeatSinkLoader.parseEntry(e.getAsJsonObject(), source);
                    if (def != null) heatSinks.add(def);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load {} from {}: {}", location, resource.sourcePackId(), e.getMessage());
        }
    }

    private record LoadedData(
            Map<ResourceLocation, FuelDefinition> fuel,
            Map<ResourceLocation, CoolantDefinition> coolant,
            List<HeatSinkDefinition> heatSinks
    ) {}
}
