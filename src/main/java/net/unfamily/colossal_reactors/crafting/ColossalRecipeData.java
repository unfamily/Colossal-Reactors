package net.unfamily.colossal_reactors.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeMap;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.datapack.DatapackSelectorValidator;
import net.unfamily.colossal_reactors.datapack.LoadDataReloadListener;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilDefinition;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilLoader;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilRegistry;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import net.unfamily.colossal_reactors.melter.MelterHeatsLoader;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import net.unfamily.colossal_reactors.radiation_scrubber.RadiationScrubberCatalystsLoader;
import net.unfamily.colossal_reactors.turbine.ElecCoilDefinition;
import net.unfamily.colossal_reactors.turbine.ElecCoilLoader;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Applies Colossal datapack recipes from {@link RecipeManager} into runtime loaders.
 * No file-scan merge / empty→builtin fallbacks. Heating coils also merge {@code load/} retrocompat.
 */
@EventBusSubscriber(modid = ColossalReactors.MODID)
public final class ColossalRecipeData {
    private static final Logger LOGGER = LoggerFactory.getLogger(ColossalRecipeData.class);
    private static final String SOURCE = "RecipeManager";

    private ColossalRecipeData() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        apply(event.getServer().getRecipeManager());
    }

    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (!event.shouldUpdateStaticData()) {
            return;
        }
        RecipeManager manager = resolveRecipeManager();
        if (manager != null) {
            apply(manager);
        }
    }

    private static RecipeManager resolveRecipeManager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getRecipeManager() : null;
    }

    public static void applyFromRecipeMap(RecipeMap recipeMap) {
        if (recipeMap == null) {
            return;
        }
        apply(type -> List.copyOf(recipeMap.byType(type)));
    }

    public static void apply(RecipeManager recipeManager) {
        if (recipeManager == null) {
            return;
        }
        apply(type -> recipeManager.getRecipes().stream()
                .filter(holder -> holder.value().getType().equals(type))
                .map(holder -> (RecipeHolder<ColossalJsonRecipe>) holder)
                .toList());
    }

    private static void apply(RecipeSource source) {
        Map<Identifier, FuelDefinition> fuel = new HashMap<>();
        Map<Identifier, CoolantDefinition> coolant = new HashMap<>();
        List<HeatSinkDefinition> heatSinks = new ArrayList<>();
        List<MelterRecipe> melterRecipes = new ArrayList<>();
        List<MelterHeatEntry> melterHeats = new ArrayList<>();
        List<String> catalysts = new ArrayList<>();
        int[] rsMult = {10};
        int[] rsGasMult = {1};
        Map<Identifier, TurbineGenerationDefinition> turbine = new HashMap<>();
        List<ElecCoilDefinition> elecCoils = new ArrayList<>();
        Map<Identifier, HeatingCoilDefinition> coils = new HashMap<>();

        for (RecipeHolder<ColossalJsonRecipe> holder : recipes(source, ModColossalRecipes.FUEL.get())) {
            for (JsonObject entry : expandEntries(holder.value().json())) {
                FuelDefinition def = FuelLoader.parseEntry(entry, SOURCE + "/" + recipeSourceId(holder), true);
                if (def != null) {
                    fuel.put(def.fuelId(), def);
                }
            }
        }
        for (RecipeHolder<ColossalJsonRecipe> holder : recipes(source, ModColossalRecipes.COOLANT.get())) {
            for (JsonObject entry : expandEntries(holder.value().json())) {
                CoolantDefinition def = CoolantLoader.parseEntry(entry, SOURCE + "/" + recipeSourceId(holder), true);
                if (def != null) {
                    coolant.put(def.coolantId(), def);
                }
            }
        }
        for (RecipeHolder<ColossalJsonRecipe> holder : recipes(source, ModColossalRecipes.HEAT_SINKS.get())) {
            for (JsonObject entry : expandEntries(holder.value().json())) {
                HeatSinkDefinition def = HeatSinkLoader.parseEntry(entry, SOURCE + "/" + recipeSourceId(holder));
                if (def != null) {
                    heatSinks.add(def);
                }
            }
        }
        for (RecipeHolder<ColossalJsonRecipe> holder :
                recipes(source, ModColossalRecipes.MELTER_RECIPES.get())) {
            for (JsonObject entry : expandEntries(holder.value().json())) {
                MelterRecipe r = MelterRecipesLoader.parseEntry(entry, SOURCE + "/" + recipeSourceId(holder));
                if (r != null) {
                    melterRecipes.add(r);
                }
            }
        }
        for (RecipeHolder<ColossalJsonRecipe> holder : recipes(source, ModColossalRecipes.MELTER_HEATS.get())) {
            JsonObject json = holder.value().json();
            List<MelterHeatEntry> list = MelterHeatsLoader.parseFromRoot(json, SOURCE + "/" + recipeSourceId(holder));
            if (list != null) {
                melterHeats.addAll(list);
            }
        }
        for (RecipeHolder<ColossalJsonRecipe> holder :
                recipes(source, ModColossalRecipes.RADIATION_SCRUBBER_CATALYSTS.get())) {
            var parsed = RadiationScrubberCatalystsLoader.parseFromRoot(
                    holder.value().json(), SOURCE + "/" + recipeSourceId(holder));
            if (parsed != null) {
                catalysts.addAll(parsed.catalysts());
                if (parsed.mult() >= 0) {
                    rsMult[0] = parsed.mult();
                }
                if (parsed.gasMult() >= 0) {
                    rsGasMult[0] = parsed.gasMult();
                }
            }
        }
        for (RecipeHolder<ColossalJsonRecipe> holder :
                recipes(source, ModColossalRecipes.TURBINE_GENERATION.get())) {
            for (JsonObject entry : expandEntries(holder.value().json())) {
                TurbineGenerationDefinition def =
                        TurbineGenerationLoader.parseEntry(entry, SOURCE + "/" + recipeSourceId(holder), true);
                if (def != null) {
                    turbine.put(def.generationId(), def);
                }
            }
        }
        for (RecipeHolder<ColossalJsonRecipe> holder : recipes(source, ModColossalRecipes.ELEC_COILS.get())) {
            for (JsonObject entry : expandEntries(holder.value().json())) {
                ElecCoilDefinition def = ElecCoilLoader.parseEntry(entry, SOURCE + "/" + recipeSourceId(holder));
                if (def != null) {
                    elecCoils.add(def);
                }
            }
        }
        for (RecipeHolder<ColossalJsonRecipe> holder :
                recipes(source, ModColossalRecipes.HEATING_COILS.get())) {
            for (HeatingCoilDefinition def :
                    HeatingCoilLoader.parseFromRoot(wrapCoilsRoot(holder), SOURCE + "/" + recipeSourceId(holder))) {
                // RecipeManager has one entry per consume option; merge back into full coil defs.
                coils.merge(def.id(), def, ColossalRecipeData::mergeHeatingCoil);
            }
        }
        Map<Identifier, HeatingCoilDefinition> loadPathCoils = LoadDataReloadListener.consumeLastLoaded();
        if (loadPathCoils != null) {
            coils.putAll(loadPathCoils);
        }

        FuelLoader.applyLoaded(fuel);
        CoolantLoader.applyLoaded(coolant);
        HeatSinkLoader.applyLoaded(heatSinks);
        MelterRecipesLoader.applyLoaded(melterRecipes);
        MelterHeatsLoader.applyLoaded(melterHeats);
        RadiationScrubberCatalystsLoader.applyLoaded(catalysts, rsMult[0], rsGasMult[0]);
        TurbineGenerationLoader.applyLoaded(turbine);
        ElecCoilLoader.applyLoaded(elecCoils);
        if (!coils.isEmpty()) {
            HeatingCoilRegistry.setFromReload(coils);
        }

        MelterRecipesLoader.rebuild();
        MelterHeatsLoader.rebuild();
        if (DatapackSelectorValidator.registriesReady()) {
            TurbineGenerationLoader.rebuildDefinitions();
            ElecCoilLoader.rebuildDefinitions();
        }

        LOGGER.info(
                "Colossal RecipeManager data: {} fuel, {} coolant, {} heat sinks, {} melter, {} heats, {} catalysts, {} turbine, {} elec, {} coils",
                fuel.size(),
                coolant.size(),
                heatSinks.size(),
                melterRecipes.size(),
                melterHeats.size(),
                catalysts.size(),
                turbine.size(),
                elecCoils.size(),
                coils.size());
    }

    @FunctionalInterface
    private interface RecipeSource {
        List<RecipeHolder<ColossalJsonRecipe>> list(RecipeType<ColossalJsonRecipe> type);
    }

    private static List<RecipeHolder<ColossalJsonRecipe>> recipes(
            RecipeSource source, RecipeType<ColossalJsonRecipe> type) {
        return source.list(type);
    }

    /**
     * If ModifyRecipeJsonsEvent did not split a parent bundle, the holder still contains an
     * {@code entries} array — expand it so loaders stay populated.
     */
    private static List<JsonObject> expandEntries(JsonObject json) {
        if (json != null && json.has("entries") && json.get("entries").isJsonArray()) {
            List<JsonObject> out = new ArrayList<>();
            for (JsonElement el : json.getAsJsonArray("entries")) {
                if (el != null && el.isJsonObject()) {
                    out.add(el.getAsJsonObject());
                }
            }
            if (!out.isEmpty()) {
                return out;
            }
        }
        return json == null ? List.of() : List.of(json);
    }

    private static String recipeSourceId(RecipeHolder<ColossalJsonRecipe> holder) {
        ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> id = holder.id();
        return id.identifier().toString();
    }

    private static HeatingCoilDefinition mergeHeatingCoil(HeatingCoilDefinition a, HeatingCoilDefinition b) {
        List<ConsumeOption> merged = new ArrayList<>(a.consume() != null ? a.consume() : List.of());
        if (b.consume() != null) {
            merged.addAll(b.consume());
        }
        return new HeatingCoilDefinition(
                a.id(), a.duration(), List.copyOf(merged), a.allSides(), a.noItem(), a.noFluid(), a.noEnergy());
    }

    private static JsonObject wrapCoilsRoot(RecipeHolder<ColossalJsonRecipe> holder) {
        JsonObject json = holder.value().json();
        if (json.has("coils") || json.has("entries") || json.has("id")) {
            return json;
        }
        JsonObject root = new JsonObject();
        root.add("entries", jsonArrayOf(json));
        return root;
    }

    private static com.google.gson.JsonArray jsonArrayOf(JsonObject one) {
        com.google.gson.JsonArray arr = new com.google.gson.JsonArray();
        arr.add(one);
        return arr;
    }
}
