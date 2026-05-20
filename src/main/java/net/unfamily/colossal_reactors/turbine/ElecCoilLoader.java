package net.unfamily.colossal_reactors.turbine;

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
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.datapack.DatapackSelectorValidator;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Loads elec coil block definitions for turbine coil zone matching.
 */
public final class ElecCoilLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElecCoilLoader.class);

    private static final String KEY_VALID_BLOCKS = "valid_blocks";
    private static final String KEY_EFF_COE = "eff_coe";
    private static final String KEY_EFF_MAX = "eff_max";

    private static final List<ElecCoilDefinition> DEFINITIONS = new ArrayList<>();

    private ElecCoilLoader() {}

    public static void applyLoaded(List<ElecCoilDefinition> loaded) {
        DEFINITIONS.clear();
        if (loaded != null && !loaded.isEmpty()) {
            for (ElecCoilDefinition def : loaded) {
                ElecCoilDefinition sanitized = DatapackSelectorValidator.sanitizeElecCoil(def);
                if (sanitized != null) {
                    DEFINITIONS.add(sanitized);
                }
            }
        }
        if (DEFINITIONS.isEmpty()) {
            DEFINITIONS.addAll(buildInternalDefaults());
        }
    }

    private static List<ElecCoilDefinition> buildInternalDefaults() {
        double empty = Config.TURBINE_EMPTY_COIL_EFFICIENCY.get();
        return List.of(new ElecCoilDefinition(List.of("minecraft:air"), empty, empty));
    }

    @Nullable
    public static ElecCoilDefinition parseEntry(JsonObject json, String sourcePath) {
        List<String> validBlocks = new ArrayList<>();
        if (json.has(KEY_VALID_BLOCKS) && json.get(KEY_VALID_BLOCKS).isJsonArray()) {
            for (JsonElement el : json.getAsJsonArray(KEY_VALID_BLOCKS)) {
                if (el.isJsonPrimitive()) validBlocks.add(el.getAsString());
            }
        }
        if (validBlocks.isEmpty()) {
            LOGGER.debug("Elec coil entry in {}: no valid_blocks", sourcePath);
            return null;
        }
        double effCoe = json.has(KEY_EFF_COE) ? json.get(KEY_EFF_COE).getAsDouble() : 1.0;
        double effMax = json.has(KEY_EFF_MAX) ? json.get(KEY_EFF_MAX).getAsDouble() : effCoe;
        return new ElecCoilDefinition(validBlocks, effCoe, effMax);
    }

    @Nullable
    public static ElecCoilModifiers getModifiersForBlock(BlockState state, RegistryAccess registryAccess) {
        for (ElecCoilDefinition def : DEFINITIONS) {
            for (String selector : def.validBlocks()) {
                if (blockMatches(state, selector, registryAccess)) {
                    return new ElecCoilModifiers(def.effCoe(), def.effMax());
                }
            }
        }
        if (state.isAir()) {
            double empty = Config.TURBINE_EMPTY_COIL_EFFICIENCY.get();
            return new ElecCoilModifiers(empty, empty);
        }
        return null;
    }

    public static ElecCoilModifiers getModifiersForBlockOrDefault(BlockState state, RegistryAccess registryAccess) {
        ElecCoilModifiers m = getModifiersForBlock(state, registryAccess);
        if (m != null) return m;
        double empty = Config.TURBINE_EMPTY_COIL_EFFICIENCY.get();
        return new ElecCoilModifiers(empty, empty);
    }

    public static boolean isCoilBlock(BlockState state, RegistryAccess registryAccess) {
        return getModifiersForBlock(state, registryAccess) != null;
    }

    public static List<ElecCoilDefinition> getAllDefinitions() {
        return List.copyOf(DEFINITIONS);
    }

    public static int getCoilOptionCount() {
        return DEFINITIONS.size();
    }

    public static boolean shouldSkipSolidCoilAutoPlacement(int selectedCoilIndex) {
        if (selectedCoilIndex < 0 || selectedCoilIndex >= DEFINITIONS.size()) return true;
        ElecCoilDefinition def = DEFINITIONS.get(selectedCoilIndex);
        if (def.validBlocks().isEmpty()) return true;
        return def.validBlocks().stream().allMatch(ElecCoilLoader::isMinecraftAirSelector);
    }

    public static boolean isBlockMatchingSelectedCoil(BlockState state, int selectedCoilIndex, RegistryAccess registryAccess) {
        if (selectedCoilIndex < 0 || selectedCoilIndex >= DEFINITIONS.size()) return false;
        for (String selector : DEFINITIONS.get(selectedCoilIndex).validBlocks()) {
            if (blockMatches(state, selector, registryAccess)) return true;
        }
        return false;
    }

    public static Component getOptionDisplayName(RegistryAccess registryAccess, int index) {
        if (index < 0 || index >= DEFINITIONS.size()) return Component.literal("?");
        ElecCoilDefinition def = DEFINITIONS.get(index);
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
        return Component.literal("?");
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

    private static boolean isMinecraftAirSelector(String selector) {
        if (selector.startsWith("#")) return false;
        ResourceLocation id = ResourceLocation.tryParse(selector);
        return id != null && ResourceLocation.DEFAULT_NAMESPACE.equals(id.getNamespace()) && "air".equals(id.getPath());
    }

    private static boolean blockMatches(BlockState state, String selector, RegistryAccess registryAccess) {
        var blockRegistry = registryAccess.registryOrThrow(Registries.BLOCK);
        ResourceLocation blockId = blockRegistry.getKey(state.getBlock());
        if (blockId == null) return false;
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return false;
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
            var holder = blockRegistry.getHolder(ResourceKey.create(Registries.BLOCK, blockId));
            if (holder.isEmpty()) return false;
            return registryAccess.lookup(Registries.BLOCK)
                    .flatMap(reg -> reg.get(tagKey))
                    .map(holders -> holders.contains(holder.get()))
                    .orElse(false);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        if (id == null) return false;
        if (isMinecraftAirSelector(selector)) {
            return state.isAir();
        }
        return blockId.equals(id);
    }

    public record ElecCoilModifiers(double effCoe, double effMax) {}
}
