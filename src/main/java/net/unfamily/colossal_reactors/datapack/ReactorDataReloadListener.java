package net.unfamily.colossal_reactors.datapack;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import net.unfamily.colossal_reactors.melter.MelterHeatsLoader;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import net.unfamily.colossal_reactors.radiation_scrubber.RadiationScrubberCatalystsLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Intermediate reload result for {@link ReactorDataReloadListener} (must be top-level for generic superclass binding).
 */
record ReactorReloadLoadedData(
        Map<Identifier, FuelDefinition> fuel,
        Map<Identifier, CoolantDefinition> coolant,
        List<HeatSinkDefinition> heatSinks,
        List<MelterRecipe> melterRecipes,
        List<MelterHeatEntry> melterHeats,
        List<String> radiationScrubberCatalysts,
        int radiationScrubberMult,
        int radiationScrubberGasMult
) {}

/**
 * Loads reactor fuel, coolant and heat sink definitions from datapack JSON.
 * Scans all namespaces under the standard path: data/&lt;namespace&gt;/recipe/ (and any subdirs).
 * The only constant is "recipe" in the path; namespace and subdirectories are free (e.g.
 * colossal_reactors/recipe/, kubejs/recipe/fuels/, mymod/recipe/reactor/). The "type" field in each
 * JSON decides the kind: "colossal_reactors:fuel", "colossal_reactors:coolant", "colossal_reactors:heat_sinks".
 * Merges from all datapacks; later packs override by fuel_id/coolant_id.
 */
public class ReactorDataReloadListener extends SimplePreparableReloadListener<ReactorReloadLoadedData> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReactorDataReloadListener.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

    private static final String TYPE_FUEL = "colossal_reactors:fuel";
    private static final String TYPE_COOLANT = "colossal_reactors:coolant";
    private static final String TYPE_HEAT_SINKS = "colossal_reactors:heat_sinks";
    private static final String TYPE_MELTER_RECIPES = "colossal_reactors:melter_recipes";
    private static final String TYPE_MELTER_HEATS = "colossal_reactors:melter_heats";
    private static final String TYPE_RADIATION_SCRUBBER_CATALYSTS = "colossal_reactors:radiation_scrubber_catalysts";
    private static final String KEY_TYPE = "type";
    private static final String KEY_ENTRIES = "entries";

    /** Path prefix: only constant is "recipe"; scans data/&lt;any_namespace&gt;/recipe/ and all subdirs. */
    private static final String REACTOR_DATA_PATH = "recipe";

    @Override
    public String getName() {
        return ColossalReactors.MODID + "_reactor_data";
    }

    @Override
    protected ReactorReloadLoadedData prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("Colossal Reactors reactor data");
        Map<Identifier, FuelDefinition> fuel = new HashMap<>();
        Map<Identifier, CoolantDefinition> coolant = new HashMap<>();
        List<HeatSinkDefinition> heatSinks = new ArrayList<>();
        List<MelterRecipe> melterRecipes = new ArrayList<>();
        List<MelterHeatEntry> melterHeats = new ArrayList<>();
        List<String> radiationScrubberCatalysts = new ArrayList<>();
        int[] rsMult = new int[]{10}; // mult; last file wins
        int[] rsGasMult = new int[]{1}; // gas_mult; last file wins
        Map<Identifier, List<Resource>> stacks = resourceManager.listResourceStacks(REACTOR_DATA_PATH,
                rl -> rl.getPath().endsWith(".json"));
        for (Map.Entry<Identifier, List<Resource>> entry : stacks.entrySet()) {
            Identifier location = entry.getKey();
            for (Resource resource : entry.getValue()) {
                processOneResource(location, resource, fuel, coolant, heatSinks, melterRecipes, melterHeats,
                        radiationScrubberCatalysts, rsMult, rsGasMult);
            }
        }
        Identifier builtinRecipes = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "recipe/melter/melter_recipes.json");
        Identifier builtinHeats = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "recipe/melter/melter_heats.json");
        if (melterRecipes.isEmpty() || !stacks.containsKey(builtinRecipes)) {
            try {
                for (Resource resource : resourceManager.getResourceStack(builtinRecipes)) {
                    processOneResource(builtinRecipes, resource, fuel, coolant, heatSinks, melterRecipes, melterHeats,
                            radiationScrubberCatalysts, rsMult, rsGasMult);
                }
            } catch (Exception e) {
                LOGGER.debug("Could not load builtin melter_recipes.json: {}", e.getMessage());
            }
        }
        if (melterHeats.isEmpty() || !stacks.containsKey(builtinHeats)) {
            try {
                for (Resource resource : resourceManager.getResourceStack(builtinHeats)) {
                    processOneResource(builtinHeats, resource, fuel, coolant, heatSinks, melterRecipes, melterHeats,
                            radiationScrubberCatalysts, rsMult, rsGasMult);
                }
            } catch (Exception e) {
                LOGGER.debug("Could not load builtin melter_heats.json: {}", e.getMessage());
            }
        }
        Identifier builtinCatalysts = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "recipe/radiation_scrubber_catalysts.json");
        if (radiationScrubberCatalysts.isEmpty() || !stacks.containsKey(builtinCatalysts)) {
            try {
                for (Resource resource : resourceManager.getResourceStack(builtinCatalysts)) {
                    processOneResource(builtinCatalysts, resource, fuel, coolant, heatSinks, melterRecipes, melterHeats,
                            radiationScrubberCatalysts, rsMult, rsGasMult);
                }
            } catch (Exception e) {
                LOGGER.debug("Could not load builtin radiation_scrubber_catalysts.json: {}", e.getMessage());
            }
        }
        profiler.pop();
        return new ReactorReloadLoadedData(fuel, coolant, heatSinks, melterRecipes, melterHeats, radiationScrubberCatalysts, rsMult[0], rsGasMult[0]);
    }

    @Override
    protected void apply(ReactorReloadLoadedData data, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("Colossal Reactors apply");
        FuelLoader.applyLoaded(data.fuel());
        CoolantLoader.applyLoaded(data.coolant());
        HeatSinkLoader.applyLoaded(data.heatSinks());
        MelterRecipesLoader.applyLoaded(data.melterRecipes());
        MelterHeatsLoader.applyLoaded(data.melterHeats());
        RadiationScrubberCatalystsLoader.applyLoaded(data.radiationScrubberCatalysts(), data.radiationScrubberMult(), data.radiationScrubberGasMult());
        profiler.pop();
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Reactor data loaded: {} fuel, {} coolant, {} heat sink, {} melter recipe, {} melter heat, {} radiation scrubber catalyst(s)",
                    data.fuel().size(), data.coolant().size(), data.heatSinks().size(),
                    data.melterRecipes().size(), data.melterHeats().size(), data.radiationScrubberCatalysts().size());
        }
    }

    private static void processOneResource(Identifier location, Resource resource,
                                          Map<Identifier, FuelDefinition> fuel,
                                          Map<Identifier, CoolantDefinition> coolant,
                                          List<HeatSinkDefinition> heatSinks,
                                          List<MelterRecipe> melterRecipes,
                                          List<MelterHeatEntry> melterHeats,
                                          List<String> radiationScrubberCatalysts,
                                          int[] radiationScrubberMult,
                                          int[] radiationScrubberGasMult) {
        String source = location + " from " + resource.sourcePackId();
        try (Reader reader = new InputStreamReader(resource.open(), StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null || !root.has(KEY_TYPE)) return;
            String type = root.get(KEY_TYPE).getAsString();

            if (TYPE_MELTER_RECIPES.equals(type)) {
                if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) return;
                for (JsonElement e : root.getAsJsonArray(KEY_ENTRIES)) {
                    if (!e.isJsonObject()) continue;
                    MelterRecipe r = MelterRecipesLoader.parseEntry(e.getAsJsonObject(), source);
                    if (r != null) melterRecipes.add(r);
                }
                return;
            }
            if (TYPE_MELTER_HEATS.equals(type)) {
                List<MelterHeatEntry> list = MelterHeatsLoader.parseFromRoot(root, source);
                if (list != null) melterHeats.addAll(list);
                return;
            }
            if (TYPE_RADIATION_SCRUBBER_CATALYSTS.equals(type)) {
                var parsed = RadiationScrubberCatalystsLoader.parseFromRoot(root, source);
                if (parsed != null) {
                    radiationScrubberCatalysts.addAll(parsed.catalysts());
                    if (parsed.mult() >= 0) radiationScrubberMult[0] = parsed.mult();
                    if (parsed.gasMult() >= 0) radiationScrubberGasMult[0] = parsed.gasMult();
                }
                return;
            }

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
}
