package net.unfamily.colossal_reactors.datapack;

import net.minecraft.client.Minecraft;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilDefinition;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Validates datapack selectors (item/block/fluid id or tag) against the active {@link RegistryAccess}
 * (server or client level) when available, otherwise {@link BuiltInRegistries}.
 * Empty tags and missing ids are dropped so they never appear in JEI, builder, or gameplay.
 */
public final class DatapackSelectorValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatapackSelectorValidator.class);
    private static final TagKey<Block> PROBE_BLOCK_TAG = TagKey.create(Registries.BLOCK, Identifier.parse("minecraft:stone"));

    private DatapackSelectorValidator() {}

    /**
     * When false (block tags not resolvable yet), filters are no-ops so reload does not drop every
     * {@code valid_blocks} entry. Must not key off item ids alone — iron ingot exists before block tags bind.
     */
    /** Exposed so loaders can rebuild when tag resolution becomes available (e.g. after joining a world). */
    public static boolean isValidationActive() {
        return tagHasEntries(PROBE_BLOCK_TAG, blockLookup());
    }

    private static boolean validationEnabled() {
        return isValidationActive();
    }

    @Nullable
    private static RegistryAccess activeRegistryAccess() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            return server.registryAccess();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            if (mc.level != null) {
                return mc.level.registryAccess();
            }
            MinecraftServer sp = mc.getSingleplayerServer();
            if (sp != null) {
                return sp.registryAccess();
            }
        }
        return null;
    }

    private static HolderLookup.RegistryLookup<Item> itemLookup() {
        RegistryAccess access = activeRegistryAccess();
        if (access != null) {
            return access.lookupOrThrow(Registries.ITEM);
        }
        return BuiltInRegistries.ITEM;
    }

    private static HolderLookup.RegistryLookup<Block> blockLookup() {
        RegistryAccess access = activeRegistryAccess();
        if (access != null) {
            return access.lookupOrThrow(Registries.BLOCK);
        }
        return BuiltInRegistries.BLOCK;
    }

    private static HolderLookup.RegistryLookup<Fluid> fluidLookup() {
        RegistryAccess access = activeRegistryAccess();
        if (access != null) {
            return access.lookupOrThrow(Registries.FLUID);
        }
        return BuiltInRegistries.FLUID;
    }

    public static boolean isResolvableItemSelector(String selector) {
        if (selector == null || selector.isBlank()) return false;
        if (selector.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(selector.substring(1));
            if (tagId == null) return false;
            return tagHasEntries(TagKey.create(Registries.ITEM, tagId), itemLookup());
        }
        Identifier id = Identifier.tryParse(selector);
        return itemExists(id);
    }

    public static boolean isResolvableBlockSelector(String selector) {
        if (selector == null || selector.isBlank()) return false;
        if (selector.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(selector.substring(1));
            if (tagId == null) return false;
            return tagHasEntries(TagKey.create(Registries.BLOCK, tagId), blockLookup());
        }
        Identifier id = Identifier.tryParse(selector);
        return blockExists(id);
    }

    public static boolean isResolvableFluidSelector(String selector) {
        if (selector == null || selector.isBlank()) return false;
        if (selector.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(selector.substring(1));
            if (tagId == null) return false;
            return tagHasEntries(TagKey.create(Registries.FLUID, tagId), fluidLookup());
        }
        Identifier id = Identifier.tryParse(selector);
        return fluidExists(id);
    }

    private static <T> boolean tagHasEntries(TagKey<T> tagKey, HolderLookup.RegistryLookup<T> lookup) {
        return lookup.get(tagKey)
                .map(tag -> tag.stream().anyMatch(h -> {
                    Object value = h.value();
                    return !(value instanceof Fluid fluid && fluid == Fluids.EMPTY);
                }))
                .orElse(false);
    }

    private static boolean itemExists(@Nullable Identifier id) {
        if (id == null) return false;
        return itemLookup().get(ResourceKey.create(Registries.ITEM, id))
                .map(h -> h.value() != Items.AIR)
                .orElse(false);
    }

    private static boolean blockExists(@Nullable Identifier id) {
        if (id == null) return false;
        if (Blocks.AIR.builtInRegistryHolder().is(id)) return true;
        return blockLookup().get(ResourceKey.create(Registries.BLOCK, id))
                .map(h -> h.value() != Blocks.AIR)
                .orElse(false);
    }

    private static boolean fluidExists(@Nullable Identifier id) {
        if (id == null) return false;
        return fluidLookup().get(ResourceKey.create(Registries.FLUID, id))
                .map(h -> h.value() != Fluids.EMPTY)
                .orElse(false);
    }

    public static List<String> filterItemSelectors(List<String> selectors) {
        if (!validationEnabled()) return List.copyOf(selectors);
        List<String> out = new ArrayList<>();
        for (String selector : selectors) {
            if (isResolvableItemSelector(selector)) {
                out.add(selector);
            } else {
                LOGGER.debug("Dropped unresolved item selector: {}", selector);
            }
        }
        return out;
    }

    public static List<String> filterBlockSelectors(List<String> selectors) {
        if (!validationEnabled()) return List.copyOf(selectors);
        List<String> out = new ArrayList<>();
        for (String selector : selectors) {
            if (isResolvableBlockSelector(selector)) {
                out.add(selector);
            } else {
                LOGGER.debug("Dropped unresolved block selector: {}", selector);
            }
        }
        return out;
    }

    public static List<String> filterFluidSelectors(List<String> selectors) {
        if (!validationEnabled()) return List.copyOf(selectors);
        List<String> out = new ArrayList<>();
        for (String selector : selectors) {
            if (isResolvableFluidSelector(selector)) {
                out.add(selector);
            } else {
                LOGGER.debug("Dropped unresolved fluid selector: {}", selector);
            }
        }
        return out;
    }

    @Nullable
    public static FuelDefinition sanitizeFuel(FuelDefinition def) {
        if (!validationEnabled()) return def;
        List<String> inputs = filterItemSelectors(def.inputs());
        if (inputs.isEmpty()) {
            String fallback = def.fuelId().toString();
            if (isResolvableItemSelector(fallback)) {
                inputs = List.of(fallback);
            } else {
                LOGGER.debug("Skipped fuel {}: no resolvable inputs", def.fuelId());
                return null;
            }
        }
        String output = def.output();
        if (output == null || output.isBlank() || !isResolvableItemSelector(output)) {
            LOGGER.debug("Skipped fuel {}: unresolved output '{}'", def.fuelId(), output);
            return null;
        }
        return new FuelDefinition(def.fuelId(), inputs, output, def.unitsPerFuel(), def.unitsPerWaste(),
                def.baseRfPerTick(), def.baseFuelUnitsPerTick(), def.overwritable());
    }

    @Nullable
    public static CoolantDefinition sanitizeCoolant(CoolantDefinition def) {
        if (!validationEnabled()) return def;
        List<String> inputs = filterFluidSelectors(def.inputs());
        if (inputs.isEmpty()) {
            String fallback = def.coolantId().toString();
            if (isResolvableFluidSelector(fallback)) {
                inputs = List.of(fallback);
            } else {
                LOGGER.debug("Skipped coolant {}: no resolvable inputs", def.coolantId());
                return null;
            }
        }
        String output = def.output();
        if (output == null || output.isBlank() || !isResolvableFluidSelector(output)) {
            LOGGER.debug("Skipped coolant {}: unresolved output '{}'", def.coolantId(), output);
            return null;
        }
        return new CoolantDefinition(def.coolantId(), inputs, output, def.rfIncrementPercent(), def.mbDecrementPercent(),
                def.reduceRfProduction(), def.rfToCoolantFactor(), def.steamPerCoolant(), def.overheatingMultiplier(),
                def.fluidColor(), def.outputColor(), def.overwritable());
    }

    @Nullable
    public static HeatSinkDefinition sanitizeHeatSink(HeatSinkDefinition def) {
        if (!validationEnabled()) return def;
        List<String> blocks = filterBlockSelectors(def.validBlocks());
        List<String> liquids = filterFluidSelectors(def.validLiquids());
        if (blocks.isEmpty() && liquids.isEmpty()) {
            LOGGER.debug("Skipped heat sink entry: no resolvable valid_blocks or valid_liquids");
            return null;
        }
        return new HeatSinkDefinition(blocks, liquids, def.fuelMultiplier(), def.energyMultiplier(),
                def.overheatingMultiplier(), def.mustSource());
    }

    public static boolean isMelterRecipeResolvable(MelterRecipe recipe) {
        if (!validationEnabled()) return true;
        boolean inputOk = recipe.inputIsTag()
                ? isResolvableItemSelector("#" + recipe.inputId())
                : isResolvableItemSelector(recipe.inputId().toString());
        boolean outputOk = recipe.outputIsTag()
                ? isResolvableFluidSelector("#" + recipe.outputFluidId())
                : isResolvableFluidSelector(recipe.outputFluidId().toString());
        if (!inputOk) LOGGER.debug("Skipped melter recipe: unresolved input {}", recipe.inputId());
        if (!outputOk) LOGGER.debug("Skipped melter recipe: unresolved output {}", recipe.outputFluidId());
        return inputOk && outputOk;
    }

    @Nullable
    public static MelterHeatEntry sanitizeMelterHeat(MelterHeatEntry entry) {
        if (!validationEnabled()) return entry;
        List<Identifier> blockIds = new ArrayList<>();
        List<Boolean> blockIdIsTag = new ArrayList<>();
        for (int i = 0; i < entry.blockIds().size(); i++) {
            Identifier id = entry.blockIds().get(i);
            boolean isTag = i < entry.blockIdIsTag().size() && entry.blockIdIsTag().get(i);
            String selector = isTag ? "#" + id : id.toString();
            if (isResolvableBlockSelector(selector)) {
                blockIds.add(id);
                blockIdIsTag.add(isTag);
            } else {
                LOGGER.debug("Dropped unresolved melter heat block selector: {}", selector);
            }
        }
        List<Identifier> fluidIds = new ArrayList<>();
        List<Boolean> fluidIdIsTag = new ArrayList<>();
        for (int i = 0; i < entry.fluidIds().size(); i++) {
            Identifier id = entry.fluidIds().get(i);
            boolean isTag = i < entry.fluidIdIsTag().size() && entry.fluidIdIsTag().get(i);
            String selector = isTag ? "#" + id : id.toString();
            if (isResolvableFluidSelector(selector)) {
                fluidIds.add(id);
                fluidIdIsTag.add(isTag);
            } else {
                LOGGER.debug("Dropped unresolved melter heat fluid selector: {}", selector);
            }
        }
        if (blockIds.isEmpty() && fluidIds.isEmpty()) {
            LOGGER.debug("Skipped melter heat entry: no resolvable blocks or fluids");
            return null;
        }
        return new MelterHeatEntry(blockIds, blockIdIsTag, fluidIds, fluidIdIsTag, entry.factor(), entry.notValid());
    }

    public static List<String> filterCatalystSelectors(List<String> selectors) {
        return filterItemSelectors(selectors);
    }

    @Nullable
    public static ConsumeOption sanitizeConsumeOption(ConsumeOption opt) {
        if (!validationEnabled()) return opt;
        ConsumeOption.FluidRequirement fluid = opt.fluid();
        if (fluid != null) {
            String selector = fluid.isTag() ? "#" + fluid.tagOrId() : fluid.tagOrId().toString();
            if (!isResolvableFluidSelector(selector)) {
                LOGGER.debug("Dropped unresolved heating coil fluid selector: {}", selector);
                fluid = null;
            }
        }
        ConsumeOption.ItemRequirement item = opt.item();
        if (item != null) {
            String selector = item.isTag() ? "#" + item.tagOrId() : item.tagOrId().toString();
            if (!isResolvableItemSelector(selector)) {
                LOGGER.debug("Dropped unresolved heating coil item selector: {}", selector);
                item = null;
            }
        }
        if (fluid == null && item == null && opt.energy() == null && opt.burnable() == null) {
            return null;
        }
        return new ConsumeOption(fluid, item, opt.energy(), opt.burnable());
    }

    public static HeatingCoilDefinition sanitizeHeatingCoil(HeatingCoilDefinition def) {
        if (!validationEnabled()) return def;
        List<ConsumeOption> consume = new ArrayList<>();
        for (ConsumeOption opt : def.consume()) {
            ConsumeOption sanitized = sanitizeConsumeOption(opt);
            if (sanitized != null && !sanitized.isEmpty()) {
                consume.add(sanitized);
            }
        }
        return new HeatingCoilDefinition(def.id(), def.duration(), consume, def.allSides(),
                def.noItem(), def.noFluid(), def.noEnergy());
    }
}
