package net.unfamily.colossal_reactors.melter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads melter heat definitions from datapack (type colossal_reactors:melter_heats).
 * valid_blocks: array of { "blocks": ["id"] or ["#tag"], "fluids": ["#tag"], "factor": number }.
 */
public final class MelterHeatsLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MelterHeatsLoader.class);
    private static final String KEY_BLOCKS = "blocks";
    private static final String KEY_FLUIDS = "fluids";
    private static final String KEY_FACTOR = "factor";

    private static final List<MelterHeatEntry> ENTRIES = new ArrayList<>();

    public static void applyLoaded(List<MelterHeatEntry> loaded) {
        ENTRIES.clear();
        if (loaded != null) ENTRIES.addAll(loaded);
    }

    public static List<MelterHeatEntry> getAll() {
        return Collections.unmodifiableList(ENTRIES);
    }

    /**
     * Returns the heat factor for the block at the given position in the given direction.
     * Direction is from melter to the adjacent block (e.g. Direction.UP = block above melter).
     * Returns 1.0 if no matching entry (sides without heating source).
     */
    public static double getFactorForBlock(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos melterPos, net.minecraft.core.Direction direction) {
        net.minecraft.core.BlockPos adjacent = melterPos.relative(direction);
        net.minecraft.world.level.block.state.BlockState state = level.getBlockState(adjacent);
        net.minecraft.world.level.block.Block block = state.getBlock();
        ResourceLocation blockId = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(block);
        for (MelterHeatEntry e : ENTRIES) {
            if (e.blockIds().isEmpty()) continue;
            for (int i = 0; i < e.blockIds().size(); i++) {
                if (e.blockIdIsTag().get(i)) {
                    var tagKey = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, e.blockIds().get(i));
                    if (state.is(tagKey)) return e.factor();
                } else {
                    if (e.blockIds().get(i).equals(blockId)) return e.factor();
                }
            }
        }
        return 1.0;
    }

    /**
     * Returns the heat factor for the fluid at the given position (adjacent block may be fluid).
     * Checks fluid state of the adjacent block. Returns 1.0 if no matching entry.
     */
    public static double getFactorForFluid(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos melterPos, net.minecraft.core.Direction direction) {
        net.minecraft.core.BlockPos adjacent = melterPos.relative(direction);
        net.minecraft.world.level.material.FluidState fluidState = level.getFluidState(adjacent);
        if (fluidState.isEmpty()) return 1.0;
        net.minecraft.world.level.material.Fluid fluid = fluidState.getType();
        ResourceLocation fluidId = net.minecraft.core.registries.BuiltInRegistries.FLUID.getKey(fluid);
        for (MelterHeatEntry e : ENTRIES) {
            if (e.fluidIds().isEmpty()) continue;
            for (int i = 0; i < e.fluidIds().size(); i++) {
                if (e.fluidIdIsTag().get(i)) {
                    var tagKey = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.FLUID, e.fluidIds().get(i));
                    if (fluid.is(tagKey)) return e.factor();
                } else {
                    if (e.fluidIds().get(i).equals(fluidId)) return e.factor();
                }
            }
        }
        return 1.0;
    }

    /**
     * Combined factor for one direction: check block first, then fluid (e.g. fluid in same block as liquid block).
     * If block matches, use block factor; else if fluid matches, use fluid factor; else 1.0.
     */
    public static double getFactorFor(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos melterPos, net.minecraft.core.Direction direction) {
        double blockFactor = getFactorForBlock(level, melterPos, direction);
        if (blockFactor != 1.0) return blockFactor;
        return getFactorForFluid(level, melterPos, direction);
    }

    @Nullable
    public static List<MelterHeatEntry> parseFromRoot(JsonObject root, String source) {
        if (!root.has("valid_blocks") || !root.get("valid_blocks").isJsonArray()) {
            LOGGER.warn("Melter heats in {}: missing valid_blocks array", source);
            return null;
        }
        List<MelterHeatEntry> out = new ArrayList<>();
        JsonArray arr = root.getAsJsonArray("valid_blocks");
        for (JsonElement el : arr) {
            if (!el.isJsonObject()) continue;
            MelterHeatEntry entry = parseOne(el.getAsJsonObject(), source);
            if (entry != null) out.add(entry);
        }
        return out;
    }

    @Nullable
    private static MelterHeatEntry parseOne(JsonObject o, String source) {
        if (!o.has(KEY_FACTOR)) return null;
        double factor = o.get(KEY_FACTOR).getAsDouble();
        List<ResourceLocation> blockIds = new ArrayList<>();
        List<Boolean> blockIdIsTag = new ArrayList<>();
        if (o.has(KEY_BLOCKS) && o.get(KEY_BLOCKS).isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray(KEY_BLOCKS)) {
                String s = e.getAsString();
                boolean isTag = s.startsWith("#");
                ResourceLocation id = ResourceLocation.tryParse(isTag ? s.substring(1) : s);
                if (id != null) {
                    blockIds.add(id);
                    blockIdIsTag.add(isTag);
                }
            }
        }
        List<ResourceLocation> fluidIds = new ArrayList<>();
        List<Boolean> fluidIdIsTag = new ArrayList<>();
        if (o.has(KEY_FLUIDS) && o.get(KEY_FLUIDS).isJsonArray()) {
            for (JsonElement e : o.getAsJsonArray(KEY_FLUIDS)) {
                String s = e.getAsString();
                boolean isTag = s.startsWith("#");
                ResourceLocation id = ResourceLocation.tryParse(isTag ? s.substring(1) : s);
                if (id != null) {
                    fluidIds.add(id);
                    fluidIdIsTag.add(isTag);
                }
            }
        }
        if (blockIds.isEmpty() && fluidIds.isEmpty()) return null;
        return new MelterHeatEntry(blockIds, blockIdIsTag, fluidIds, fluidIdIsTag, factor);
    }
}
