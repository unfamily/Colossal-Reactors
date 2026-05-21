package net.unfamily.colossal_reactors.turbine;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.datapack.DatapackSelectorValidator;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads turbine generation definitions from datapack JSON (steam in, optional fluid out).
 * {@link TurbineGenerationDefinition#rfProduction()} is RF per mB steam ({@code rf_production} in datapack JSON).
 */
public final class TurbineGenerationLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(TurbineGenerationLoader.class);

    private static final String KEY_GENERATION_ID = "coolant_id";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_RF_PRODUCTION = "rf_production";
    private static final String KEY_OVERWRITABLE = "overwritable";

    public static final Identifier DEFAULT_GENERATION_ID =
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "water");

    private static final Map<Identifier, TurbineGenerationDefinition> DEFINITIONS = new HashMap<>();
    /** Parsed datapack entries; sanitized when registries/tags are ready (see {@link #rebuildDefinitions()}). */
    private static Map<Identifier, TurbineGenerationDefinition> rawDatapackGeneration = Map.of();

    private TurbineGenerationLoader() {}

    public static void applyLoaded(Map<Identifier, TurbineGenerationDefinition> loaded) {
        synchronized (TurbineGenerationLoader.class) {
            rawDatapackGeneration = loaded != null ? Map.copyOf(loaded) : Map.of();
            rebuildDefinitions();
        }
    }

    private static void rebuildDefinitions() {
        DEFINITIONS.clear();
        registerInternalDefaults();
        for (TurbineGenerationDefinition def : rawDatapackGeneration.values()) {
            TurbineGenerationDefinition sanitized = DatapackSelectorValidator.sanitizeTurbineGeneration(def);
            if (sanitized != null) {
                processEntry(sanitized);
            }
        }
    }

    private static void registerInternalDefaults() {
        List<String> inputs = List.of(
                "colossal_reactors:gas_fluid_steam",
                "#c:steam");
        String output = "minecraft:water";
        double rfPerMb = Config.TURBINE_DEFAULT_RF_PER_STEAM_MB.get();
        DEFINITIONS.put(DEFAULT_GENERATION_ID, new TurbineGenerationDefinition(
                DEFAULT_GENERATION_ID, inputs, output, rfPerMb, true));
    }

    /** {@link TurbineGenerationDefinition#rfProduction()} is already RF per mB. */
    public static double rfPerSteamMb(double rfPerMb) {
        return rfPerMb;
    }

    public static String formatRfPerSteamMb(double rfPerMb) {
        if (rfPerMb == Math.rint(rfPerMb)) {
            return String.format("%.0f", rfPerMb);
        }
        return String.format("%.3f", rfPerMb);
    }

    /** Entries with resolvable steam input and output (for JEI and builder simulation). */
    public static List<TurbineGenerationDefinition> getVisibleDefinitions() {
        return DEFINITIONS.values().stream()
                .filter(def -> DatapackSelectorValidator.sanitizeTurbineGeneration(def) != null)
                .sorted(java.util.Comparator.comparing(d -> d.generationId().toString()))
                .toList();
    }

    private static void processEntry(TurbineGenerationDefinition def) {
        TurbineGenerationDefinition existing = DEFINITIONS.get(def.generationId());
        if (existing != null && !existing.overwritable()) {
            LOGGER.debug("Skipping turbine generation {}: not overwritable", def.generationId());
            return;
        }
        DEFINITIONS.put(def.generationId(), def);
    }

    @Nullable
    public static TurbineGenerationDefinition parseEntry(JsonObject json, String sourcePath, boolean defaultOverwritable) {
        if (!json.has(KEY_GENERATION_ID)) {
            LOGGER.warn("Turbine generation entry in {}: missing '{}'", sourcePath, KEY_GENERATION_ID);
            return null;
        }
        Identifier id = Identifier.tryParse(json.get(KEY_GENERATION_ID).getAsString());
        if (id == null) {
            LOGGER.warn("Turbine generation entry in {}: invalid id", sourcePath);
            return null;
        }
        List<String> inputs = new ArrayList<>();
        if (json.has(KEY_INPUTS) && json.get(KEY_INPUTS).isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray(KEY_INPUTS)) {
                if (el.isJsonPrimitive()) inputs.add(el.getAsString());
            }
        }
        String output = json.has(KEY_OUTPUT) ? json.get(KEY_OUTPUT).getAsString() : "";
        double rf = json.has(KEY_RF_PRODUCTION)
                ? json.get(KEY_RF_PRODUCTION).getAsDouble()
                : Config.TURBINE_DEFAULT_RF_PER_STEAM_MB.get();
        boolean overwritable = json.has(KEY_OVERWRITABLE)
                ? json.get(KEY_OVERWRITABLE).getAsBoolean()
                : defaultOverwritable;
        if (inputs.isEmpty()) {
            inputs.add("#c:steam");
        }
        return new TurbineGenerationDefinition(id, List.copyOf(inputs), output, rf, overwritable);
    }

    @Nullable
    public static TurbineGenerationDefinition get(Identifier id) {
        return DEFINITIONS.get(id);
    }

    @Nullable
    public static TurbineGenerationDefinition getDefault() {
        return DEFINITIONS.get(DEFAULT_GENERATION_ID);
    }

    public static Map<Identifier, TurbineGenerationDefinition> getAll() {
        return new HashMap<>(DEFINITIONS);
    }

    @Nullable
    public static Fluid getOutputFluid(@Nullable TurbineGenerationDefinition def, RegistryAccess registryAccess) {
        if (def == null || def.output() == null || def.output().isBlank()) {
            return null;
        }
        String output = def.output().trim();
        if (output.startsWith("#")) {
            return getFirstFluidFromTag(output, registryAccess);
        }
        Identifier id = Identifier.tryParse(output);
        if (id == null) {
            return null;
        }
        Fluid fluid = BuiltInRegistries.FLUID.getValue(id);
        return fluid != null && fluid != Fluids.EMPTY ? fluid : null;
    }

    @Nullable
    public static Fluid getFirstFluidFromTag(String selector, RegistryAccess registryAccess) {
        if (selector == null || !selector.startsWith("#")) return null;
        Identifier tagId = Identifier.tryParse(selector.substring(1));
        if (tagId == null) return null;
        TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
        return registryAccess.lookup(Registries.FLUID)
                .flatMap(l -> l.get(tagKey))
                .flatMap(holders -> holders.stream().findFirst())
                .map(h -> h.value())
                .orElse(null);
    }

    @Nullable
    public static TurbineGenerationDefinition getDefinitionForFluid(Fluid fluid, RegistryAccess registryAccess) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        Identifier fluidId = BuiltInRegistries.FLUID.getKey(fluid);
        for (TurbineGenerationDefinition def : DEFINITIONS.values()) {
            for (String input : def.inputs()) {
                if (input.startsWith("#")) {
                    Identifier tagId = Identifier.tryParse(input.substring(1));
                    if (tagId == null) continue;
                    TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
                    Registry<Fluid> fluids = registryAccess.lookupOrThrow(Registries.FLUID);
                    var fluidRef = fluids.get(fluidId);
                    if (fluidRef.isEmpty()) continue;
                    boolean inTag = fluids.get(tagKey).map(tag -> tag.contains(fluidRef.get())).orElse(false);
                    if (inTag) return def;
                } else {
                    if (Identifier.tryParse(input).equals(fluidId)) return def;
                }
            }
        }
        return null;
    }
}
