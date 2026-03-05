package net.unfamily.colossal_reactors.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
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
 * Three files: reactor_fuel.json, reactor_coolant.json, reactor_heat_sinks.json.
 * Each has "type": "colossal_reactors:fuel" (or coolant/heat_sinks) and "entries": [ ... ].
 */
public class ReactorDataReloadListener implements PreparableReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorDataReloadListener.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final String TYPE_FUEL = "colossal_reactors:fuel";
    private static final String TYPE_COOLANT = "colossal_reactors:coolant";
    private static final String TYPE_HEAT_SINKS = "colossal_reactors:heat_sinks";
    private static final String KEY_TYPE = "type";
    private static final String KEY_ENTRIES = "entries";

    private static final ResourceLocation FUEL_RESOURCE = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_fuel.json");
    private static final ResourceLocation COOLANT_RESOURCE = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_coolant.json");
    private static final ResourceLocation HEAT_SINKS_RESOURCE = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_heat_sinks.json");

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
            Map<ResourceLocation, FuelDefinition> fuel = loadFuel(resourceManager);
            Map<ResourceLocation, CoolantDefinition> coolant = loadCoolant(resourceManager);
            List<HeatSinkDefinition> heatSinks = loadHeatSinks(resourceManager);
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

    private static Map<ResourceLocation, FuelDefinition> loadFuel(ResourceManager rm) {
        Map<ResourceLocation, FuelDefinition> out = new HashMap<>();
        var stack = rm.getResourceStack(FUEL_RESOURCE);
        if (stack.isEmpty()) return out;
        try (Reader reader = new InputStreamReader(stack.get(stack.size() - 1).open(), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has(KEY_TYPE) || !TYPE_FUEL.equals(root.get(KEY_TYPE).getAsString())) return out;
            if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) return out;
            JsonArray entries = root.getAsJsonArray(KEY_ENTRIES);
            boolean overwritable = !root.has("overwritable") || root.get("overwritable").getAsBoolean();
            for (JsonElement e : entries) {
                if (!e.isJsonObject()) continue;
                FuelDefinition def = FuelLoader.parseEntry(e.getAsJsonObject(), FUEL_RESOURCE.toString(), overwritable);
                if (def != null) out.put(def.fuelId(), def);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load reactor_fuel.json: {}", e.getMessage());
        }
        return out;
    }

    private static Map<ResourceLocation, CoolantDefinition> loadCoolant(ResourceManager rm) {
        Map<ResourceLocation, CoolantDefinition> out = new HashMap<>();
        var stack = rm.getResourceStack(COOLANT_RESOURCE);
        if (stack.isEmpty()) return out;
        try (Reader reader = new InputStreamReader(stack.get(stack.size() - 1).open(), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has(KEY_TYPE) || !TYPE_COOLANT.equals(root.get(KEY_TYPE).getAsString())) return out;
            if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) return out;
            JsonArray entries = root.getAsJsonArray(KEY_ENTRIES);
            boolean overwritable = !root.has("overwritable") || root.get("overwritable").getAsBoolean();
            for (JsonElement e : entries) {
                if (!e.isJsonObject()) continue;
                CoolantDefinition def = CoolantLoader.parseEntry(e.getAsJsonObject(), COOLANT_RESOURCE.toString(), overwritable);
                if (def != null) out.put(def.coolantId(), def);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load reactor_coolant.json: {}", e.getMessage());
        }
        return out;
    }

    private static List<HeatSinkDefinition> loadHeatSinks(ResourceManager rm) {
        List<HeatSinkDefinition> out = new ArrayList<>();
        var stack = rm.getResourceStack(HEAT_SINKS_RESOURCE);
        if (stack.isEmpty()) return out;
        try (Reader reader = new InputStreamReader(stack.get(stack.size() - 1).open(), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has(KEY_TYPE) || !TYPE_HEAT_SINKS.equals(root.get(KEY_TYPE).getAsString())) return out;
            if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) return out;
            JsonArray entries = root.getAsJsonArray(KEY_ENTRIES);
            for (JsonElement e : entries) {
                if (!e.isJsonObject()) continue;
                HeatSinkDefinition def = HeatSinkLoader.parseEntry(e.getAsJsonObject(), HEAT_SINKS_RESOURCE.toString());
                if (def != null) out.add(def);
            }
        } catch (Exception e) {
            LOGGER.warn("Failed to load reactor_heat_sinks.json: {}", e.getMessage());
        }
        return out;
    }

    private record LoadedData(
            Map<ResourceLocation, FuelDefinition> fuel,
            Map<ResourceLocation, CoolantDefinition> coolant,
            List<HeatSinkDefinition> heatSinks
    ) {}
}
