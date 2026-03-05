package net.unfamily.colossal_reactors.heatsink;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.colossal_reactors.Config;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads heat sink definitions from external JSON (external_scripts_path/reactor/*.json with type "colossal_reactors:heat_sinks").
 * No hardcoded defaults: only entries from JSON (and air as implicit coolant). Interior valid_blocks and rod coolant
 * valid_liquids contribute to aggregate fuel/energy multipliers in simulation.
 */
public final class HeatSinkLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(HeatSinkLoader.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private static final String TYPE_HEAT_SINKS = "colossal_reactors:heat_sinks";
    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_VALID_BLOCKS = "valid_blocks";
    private static final String KEY_VALID_LIQUIDS = "valid_liquids";
    private static final String KEY_FUEL = "fuel";
    private static final String KEY_ENERGY = "energy";
    /** Overheating multiplier for stability; only used when reactor instability is enabled in config. If omitted, defaults to fuel. */
    private static final String KEY_OVERHEATING = "overheating";
    private static final String KEY_DISABLE = "disable";
    /** Optional, default true: when true, valid_liquids only match fluid source (e.g. water source), not flowing. */
    private static final String KEY_MUST_SOURCE = "must_source";

    private static final List<HeatSinkDefinition> DEFINITIONS = new ArrayList<>();

    private HeatSinkLoader() {}

    /** Internal defaults: overheating = same as fuel for each entry. JSON in reactor/ can override or add. */
    private static void registerInternalDefaults() {
        DEFINITIONS.add(new HeatSinkDefinition(
                List.of(),
                List.of("#c:water"),
                1.05, 1.15, 1.05, false)); // water: source and flowing both valid
        DEFINITIONS.add(new HeatSinkDefinition(
                List.of("#c:storage_blocks/diamond"),
                List.of(),
                1.8, 1.6, 1.8, true));
        DEFINITIONS.add(new HeatSinkDefinition(
                List.of("#c:storage_blocks/emerald"),
                List.of(),
                1.8, 1.6, 1.8, true));
        DEFINITIONS.add(new HeatSinkDefinition(
                List.of("#c:storage_blocks/netherite"),
                List.of(),
                1.7, 2.3, 1.7, true));
        DEFINITIONS.add(new HeatSinkDefinition(
                List.of("#c:storage_blocks/gold"),
                List.of(),
                1.7, 1.5, 1.7, true));
        DEFINITIONS.add(new HeatSinkDefinition(
                List.of("#c:storage_blocks/graphite", "#minecraft:ice"),
                List.of(),
                2.5, -5.0, 2.5, true));
    }

    /**
     * Scans the reactor config directory for heat_sinks JSON. If any JSON with type heat_sinks exists, only those entries are used (override mode).
     * Internal defaults are used only when the reactor directory is missing or contains no heat_sinks JSON.
     */
    public static void scanConfigDirectory() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Scanning heat sink configuration (reactor directory)...");
        }
        DEFINITIONS.clear();

        String basePath = Config.EXTERNAL_SCRIPTS_PATH.get();
        if (basePath == null || basePath.trim().isEmpty()) {
            basePath = "kubejs/external_scripts/colossal_reactors";
        }
        Path reactorPath = Paths.get(basePath, "reactor");
        if (Files.exists(reactorPath) && Files.isDirectory(reactorPath)) {
            try (var stream = Files.list(reactorPath)) {
                stream.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".json"))
                        .filter(p -> !p.getFileName().toString().startsWith("."))
                        .sorted()
                        .forEach(HeatSinkLoader::parseConfigFile);
            } catch (IOException e) {
                LOGGER.error("Error scanning reactor directory for heat sinks: {}", e.getMessage());
            }
        }
        if (DEFINITIONS.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No heat sink JSON found in reactor directory. Using internal defaults. Run dump to create heat_sink.json and override.");
            }
            registerInternalDefaults();
        } else if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Heat sink definitions loaded from JSON (override mode): {}", DEFINITIONS.size());
        }
    }

    private static void parseConfigFile(Path filePath) {
        try (Reader reader = Files.newBufferedReader(filePath)) {
            JsonElement el = GSON.fromJson(reader, JsonElement.class);
            if (el == null || !el.isJsonObject()) return;
            JsonObject root = el.getAsJsonObject();
            String type = root.has("type") ? root.get("type").getAsString() : "";
            if (!TYPE_HEAT_SINKS.equals(type)) {
                return;
            }
            if (!root.has(KEY_ENTRIES) || !root.get(KEY_ENTRIES).isJsonArray()) {
                LOGGER.warn("Heat sink config {}: missing or invalid 'entries' array", filePath.getFileName());
                return;
            }
            JsonArray entries = root.getAsJsonArray(KEY_ENTRIES);
            for (JsonElement e : entries) {
                if (!e.isJsonObject()) continue;
                JsonObject obj = e.getAsJsonObject();
                boolean disable = obj.has(KEY_DISABLE) && obj.get(KEY_DISABLE).getAsBoolean();
                if (disable) continue;
                HeatSinkDefinition def = parseEntry(obj, filePath.toString());
                if (def != null) DEFINITIONS.add(def);
            }
        } catch (Exception e) {
            LOGGER.error("Error parsing heat sink file {}: {}", filePath, e.getMessage());
        }
    }

    private static HeatSinkDefinition parseEntry(JsonObject json, String filePath) {
        List<String> validBlocks = new ArrayList<>();
        if (json.has(KEY_VALID_BLOCKS) && json.get(KEY_VALID_BLOCKS).isJsonArray()) {
            for (JsonElement i : json.getAsJsonArray(KEY_VALID_BLOCKS)) {
                if (i.isJsonPrimitive()) validBlocks.add(i.getAsString());
            }
        }
        List<String> validLiquids = new ArrayList<>();
        if (json.has(KEY_VALID_LIQUIDS) && json.get(KEY_VALID_LIQUIDS).isJsonArray()) {
            for (JsonElement i : json.getAsJsonArray(KEY_VALID_LIQUIDS)) {
                if (i.isJsonPrimitive()) validLiquids.add(i.getAsString());
            }
        }
        if (validBlocks.isEmpty() && validLiquids.isEmpty()) {
            LOGGER.debug("Heat sink entry in {}: skipped (no valid_blocks or valid_liquids)", filePath);
            return null;
        }
        double fuel = json.has(KEY_FUEL) ? json.get(KEY_FUEL).getAsDouble() : 1.0;
        double energy = json.has(KEY_ENERGY) ? json.get(KEY_ENERGY).getAsDouble() : 1.0;
        // overheating: used only when config REACTOR_UNSTABILITY is enabled; default = fuel
        double overheating = json.has(KEY_OVERHEATING) ? json.get(KEY_OVERHEATING).getAsDouble() : fuel;
        boolean mustSource = !json.has(KEY_MUST_SOURCE) || json.get(KEY_MUST_SOURCE).getAsBoolean();
        return new HeatSinkDefinition(validBlocks, validLiquids, fuel, energy, overheating, mustSource);
    }

    /**
     * True if this block state is allowed as interior coolant: air, any block matching valid_blocks, or a liquid block (using fluid type/source identity for valid_liquids). Flowing or unknown liquid is allowed and treated as air (does not block the reactor).
     */
    public static boolean isHeatSinkBlock(BlockState state, RegistryAccess registryAccess) {
        if (state.isAir()) return true;
        return getModifiersForBlock(state, registryAccess) != null;
    }

    /**
     * Returns fuel and energy multipliers for this block (air = 1.0, 1.0). Checks valid_blocks first, then if the state is a liquid block uses the fluid type (source identity) for valid_liquids/tag checks; flowing blocks use that same identity. When must_source is true, flowing is not counted as heat sink but is allowed and treated as air (1.0, 1.0, 1.0). Non-source that cannot be counted as valid heat sink (unknown liquid or flowing when must_source) does not block the reactor and is treated as air.
     */
    public static HeatSinkModifiers getModifiersForBlock(BlockState state, RegistryAccess registryAccess) {
        if (state.isAir()) return new HeatSinkModifiers(1.0, 1.0, 1.0);
        for (HeatSinkDefinition def : DEFINITIONS) {
            for (String selector : def.validBlocks()) {
                if (blockMatches(state, selector, registryAccess)) {
                    return new HeatSinkModifiers(def.fuelMultiplier(), def.energyMultiplier(), def.overheatingMultiplier());
                }
            }
        }
        FluidState fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            // Always resolve to source for id/tag checks (flowing -> getSource())
            Fluid fluid = resolveToSource(fluidState.getType());
            for (HeatSinkDefinition def : DEFINITIONS) {
                for (String selector : def.validLiquids()) {
                    if (fluidMatches(fluid, selector, registryAccess)) {
                        // must_source and flowing: do not count as heat sink, but allow block (treat as air)
                        if (def.mustSource() && !fluidState.isSource()) {
                            return new HeatSinkModifiers(1.0, 1.0, 1.0);
                        }
                        return new HeatSinkModifiers(def.fuelMultiplier(), def.energyMultiplier(), def.overheatingMultiplier());
                    }
                }
            }
            // Unknown liquid (no valid_liquids match): do not block reactor, treat as air
            return new HeatSinkModifiers(1.0, 1.0, 1.0);
        }
        return null;
    }

    /**
     * Same as getModifiersForBlock but returns (1.0, 1.0) for unknown blocks (e.g. when only used after validation).
     */
    public static HeatSinkModifiers getModifiersForBlockOrDefault(BlockState state, RegistryAccess registryAccess) {
        HeatSinkModifiers m = getModifiersForBlock(state, registryAccess);
        return m != null ? m : new HeatSinkModifiers(1.0, 1.0, 1.0);
    }

    /**
     * Returns fuel, energy and overheating multipliers for this fluid from valid_liquids entries, or null if no match.
     */
    public static HeatSinkModifiers getModifiersForFluid(Fluid fluid, RegistryAccess registryAccess) {
        if (fluid == null || fluid == Fluids.EMPTY) return null;
        for (HeatSinkDefinition def : DEFINITIONS) {
            for (String selector : def.validLiquids()) {
                if (fluidMatches(fluid, selector, registryAccess)) {
                    if (def.mustSource() && !fluid.defaultFluidState().isSource()) {
                        continue; // flowing fluid not valid when must_source is true
                    }
                    return new HeatSinkModifiers(def.fuelMultiplier(), def.energyMultiplier(), def.overheatingMultiplier());
                }
            }
        }
        return null;
    }

    /** Same as getModifiersForFluid but returns (1.0, 1.0, 1.0) when fluid has no heat sink entry. */
    public static HeatSinkModifiers getModifiersForFluidOrDefault(Fluid fluid, RegistryAccess registryAccess) {
        HeatSinkModifiers m = getModifiersForFluid(fluid, registryAccess);
        return m != null ? m : new HeatSinkModifiers(1.0, 1.0, 1.0);
    }

    /**
     * Returns modifiers for the builder heat sink option index (0 = Air = 1,1,1; 1.. = definition by order).
     * Used by GUI simulation when all heat sink positions are the same selected type.
     */
    public static HeatSinkModifiers getModifiersForHeatSinkIndex(RegistryAccess registryAccess, int index) {
        if (index <= 0) return new HeatSinkModifiers(1.0, 1.0, 1.0);
        int defIdx = index - 1;
        if (defIdx >= DEFINITIONS.size()) return new HeatSinkModifiers(1.0, 1.0, 1.0);
        HeatSinkDefinition def = DEFINITIONS.get(defIdx);
        if (!def.validBlocks().isEmpty()) {
            BlockState state = getFirstBlockStateFromSelector(def.validBlocks().getFirst(), registryAccess);
            return state != null ? getModifiersForBlockOrDefault(state, registryAccess) : new HeatSinkModifiers(1.0, 1.0, 1.0);
        }
        if (!def.validLiquids().isEmpty()) {
            Fluid fluid = getFirstFluidFromSelector(def.validLiquids().getFirst(), registryAccess);
            return getModifiersForFluidOrDefault(fluid, registryAccess);
        }
        return new HeatSinkModifiers(1.0, 1.0, 1.0);
    }

    @Nullable
    private static BlockState getFirstBlockStateFromSelector(String selector, RegistryAccess registryAccess) {
        var blockReg = registryAccess.registryOrThrow(Registries.BLOCK);
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return null;
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
            return blockReg.getTag(tagKey)
                    .flatMap(holders -> holders.stream().findFirst())
                    .map(h -> h.value().defaultBlockState())
                    .orElse(null);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        if (id == null || !blockReg.containsKey(id)) return null;
        return blockReg.get(id).defaultBlockState();
    }

    @Nullable
    private static Fluid getFirstFluidFromSelector(String selector, RegistryAccess registryAccess) {
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return null;
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
            return registryAccess.lookup(Registries.FLUID)
                    .flatMap(l -> l.get(tagKey))
                    .flatMap(holders -> holders.stream().findFirst())
                    .map(h -> h.value())
                    .orElse(null);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        return id != null ? BuiltInRegistries.FLUID.get(id) : null;
    }

    /** Returns a copy of all heat sink definitions (e.g. for JEI or other display). */
    public static List<HeatSinkDefinition> getAllDefinitions() {
        return List.copyOf(DEFINITIONS);
    }

    /** Number of heat sink options for builder GUI: 1 (Air) + one per definition. */
    public static int getHeatSinkOptionCount() {
        return 1 + DEFINITIONS.size();
    }

    /** Display name for option index (0 = Air, 1.. = first block/fluid name from that definition). */
    public static Component getOptionDisplayName(RegistryAccess registryAccess, int index) {
        if (index <= 0) return Component.translatable("block.minecraft.air");
        int defIdx = index - 1;
        if (defIdx >= DEFINITIONS.size()) return Component.literal("?");
        HeatSinkDefinition def = DEFINITIONS.get(defIdx);
        if (!def.validBlocks().isEmpty()) {
            String selector = def.validBlocks().getFirst();
            var blockReg = registryAccess.registryOrThrow(Registries.BLOCK);
            if (selector.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
                if (tagId != null) {
                    TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
                    return blockReg.getTag(tagKey)
                            .flatMap(holders -> holders.stream().findFirst())
                            .map(h -> Component.translatable(h.value().getDescriptionId()))
                            .orElse(Component.literal(selector));
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(selector);
                if (id != null && blockReg.containsKey(id)) {
                    return Component.translatable(blockReg.get(id).getDescriptionId());
                }
            }
            return Component.literal(selector);
        }
        if (!def.validLiquids().isEmpty()) {
            String selector = def.validLiquids().getFirst();
            var fluidReg = registryAccess.registryOrThrow(Registries.FLUID);
            if (selector.startsWith("#")) {
                ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
                if (tagId != null) {
                    TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
                    return fluidReg.getTag(tagKey)
                            .flatMap(holders -> holders.stream().findFirst())
                            .map(h -> Component.translatable(h.value().getFluidType().getDescriptionId()))
                            .orElse(Component.literal(selector));
                }
            } else {
                ResourceLocation id = ResourceLocation.tryParse(selector);
                if (id != null && fluidReg.containsKey(id)) {
                    return Component.translatable(fluidReg.get(id).getFluidType().getDescriptionId());
                }
            }
            return Component.literal(selector);
        }
        return Component.literal("?");
    }

    /** Resolves flowing fluid to its source for id/tag lookup. For safety, identity checks always use the source. */
    private static Fluid resolveToSource(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return fluid;
        if (fluid instanceof FlowingFluid flowing) return flowing.getSource();
        return fluid;
    }

    private static boolean fluidMatches(Fluid fluid, String selector, RegistryAccess registryAccess) {
        Fluid source = resolveToSource(fluid);
        ResourceLocation fluidId = BuiltInRegistries.FLUID.getKey(source);
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return false;
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
            var holder = registryAccess.registryOrThrow(Registries.FLUID).getHolder(ResourceKey.create(Registries.FLUID, fluidId));
            if (holder.isEmpty()) return false;
            return registryAccess.lookup(Registries.FLUID)
                    .flatMap(l -> l.get(tagKey))
                    .map(holders -> holders.contains(holder.get()))
                    .orElse(false);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        return id != null && id.equals(fluidId);
    }

    /**
     * Block matches selector: tag (#namespace:path) or direct id.
     * Uses RegistryAccess so datapack/c block tags are resolved from the level's registry, not only built-in.
     */
    private static boolean blockMatches(BlockState state, String selector, RegistryAccess registryAccess) {
        var blockRegistry = registryAccess.registryOrThrow(Registries.BLOCK);
        ResourceLocation blockId = blockRegistry.getKey(state.getBlock());
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return false;
            TagKey<net.minecraft.world.level.block.Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
            var holder = blockRegistry.getHolder(ResourceKey.create(Registries.BLOCK, blockId));
            if (holder.isEmpty()) return false;
            return registryAccess.lookup(Registries.BLOCK)
                    .flatMap(reg -> reg.get(tagKey))
                    .map(holders -> holders.contains(holder.get()))
                    .orElse(false);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        return id != null && blockId.equals(id);
    }

    /**
     * Writes the default heat sink JSON into the given reactor directory. Called only by /colossal_reactors dump.
     * Edits to the file override internal defaults when the mod loads.
     */
    public static void dumpDefaultFile(Path reactorDir) throws IOException {
        Files.createDirectories(reactorDir);
        Path file = reactorDir.resolve("default_heat_sinks.json");
        String content = """
            {
              "type": "colossal_reactors:heat_sinks",
              "_comment_overheating": "overheating is only used when reactor instability is enabled in config (evil_things)",
              "entries": [
                {
                  "valid_liquids": ["#c:water"],
                  "must_source": false,
                  "fuel": 1.05,
                  "energy": 1.15,
                  "overheating": 1.05
                },
                {
                  "valid_blocks": ["#c:storage_blocks/diamond"],
                  "fuel": 1.8,
                  "energy": 1.6,
                  "overheating": 1.8
                },
                {
                  "valid_blocks": ["#c:storage_blocks/emerald"],
                  "fuel": 1.8,
                  "energy": 1.6,
                  "overheating": 1.8
                },
                {
                  "valid_blocks": ["#c:storage_blocks/netherite"],
                  "fuel": 1.7,
                  "energy": 2.3,
                  "overheating": 1.7
                },
                {
                  "valid_blocks": ["#c:storage_blocks/gold"],
                  "fuel": 1.7,
                  "energy": 1.5,
                  "overheating": 1.7
                },
                {
                  "valid_blocks": ["#c:storage_blocks/graphite", "#minecraft:ice"],
                  "fuel": 2.5,
                  "energy": -5.0,
                  "overheating": 2.5
                }
              ]
            }
            """;
        Files.writeString(file, content);
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Dumped default heat sink config to {}", file.toAbsolutePath());
        }
    }

    public record HeatSinkModifiers(double fuelMultiplier, double energyMultiplier, double overheatingMultiplier) {}

    /**
     * Result for simulation: weighted averages plus adjacent/non-adjacent energy, fuel and overheating sums.
     * RF = Base * (sumEnergyAdj + countNon*rodCount) * efficiencyFactor / rodCount.
     * sumOverheatingAdj + sumOverheatingNon are used for stability (surriscaldamento); default in JSON = same as fuel.
     */
    public record HeatSinkModifiersResult(
            double fuelMultiplier,
            double energyMultiplier,
            double sumEnergyAdj,
            double sumFuelAdj,
            double sumEnergyNon,
            double sumFuelNon,
            double sumOverheatingAdj,
            double sumOverheatingNon,
            int countAdj,
            int countNon
    ) {}
}
