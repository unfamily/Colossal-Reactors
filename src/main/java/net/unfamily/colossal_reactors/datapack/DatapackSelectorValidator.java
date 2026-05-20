package net.unfamily.colossal_reactors.datapack;

import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
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
 * Validates datapack selectors (item/block/fluid id or tag) against the live registry.
 * Empty tags and missing ids are dropped so they never appear in JEI, builder, or gameplay.
 */
public final class DatapackSelectorValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatapackSelectorValidator.class);

    private DatapackSelectorValidator() {}

    public static RegistryAccess registryAccess() {
        return RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
    }

    public static boolean isResolvableItemSelector(String selector, RegistryAccess access) {
        if (selector == null || selector.isBlank()) return false;
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return false;
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            return access.lookup(Registries.ITEM)
                    .flatMap(lookup -> lookup.get(tagKey))
                    .map(holders -> holders.stream().findFirst().isPresent())
                    .orElse(false);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        if (id == null) return false;
        return access.lookup(Registries.ITEM)
                .flatMap(lookup -> lookup.get(ResourceKey.create(Registries.ITEM, id)))
                .map(Holder::value)
                .filter(item -> item != Items.AIR)
                .isPresent();
    }

    public static boolean isResolvableBlockSelector(String selector, RegistryAccess access) {
        if (selector == null || selector.isBlank()) return false;
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return false;
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
            return access.lookup(Registries.BLOCK)
                    .flatMap(lookup -> lookup.get(tagKey))
                    .map(holders -> holders.stream().findFirst().isPresent())
                    .orElse(false);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        if (id == null) return false;
        return access.lookup(Registries.BLOCK)
                .flatMap(lookup -> lookup.get(ResourceKey.create(Registries.BLOCK, id)))
                .map(Holder::value)
                .filter(block -> block != Blocks.AIR)
                .isPresent();
    }

    public static boolean isResolvableFluidSelector(String selector, RegistryAccess access) {
        if (selector == null || selector.isBlank()) return false;
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return false;
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
            return access.lookup(Registries.FLUID)
                    .flatMap(lookup -> lookup.get(tagKey))
                    .map(holders -> holders.stream().anyMatch(h -> h.value() != Fluids.EMPTY))
                    .orElse(false);
        }
        ResourceLocation id = ResourceLocation.tryParse(selector);
        if (id == null) return false;
        return access.lookup(Registries.FLUID)
                .flatMap(lookup -> lookup.get(ResourceKey.create(Registries.FLUID, id)))
                .map(Holder::value)
                .filter(fluid -> fluid != Fluids.EMPTY)
                .isPresent();
    }

    public static List<String> filterItemSelectors(List<String> selectors, RegistryAccess access) {
        List<String> out = new ArrayList<>();
        for (String selector : selectors) {
            if (isResolvableItemSelector(selector, access)) {
                out.add(selector);
            } else {
                LOGGER.debug("Dropped unresolved item selector: {}", selector);
            }
        }
        return out;
    }

    public static List<String> filterBlockSelectors(List<String> selectors, RegistryAccess access) {
        List<String> out = new ArrayList<>();
        for (String selector : selectors) {
            if (isResolvableBlockSelector(selector, access)) {
                out.add(selector);
            } else {
                LOGGER.debug("Dropped unresolved block selector: {}", selector);
            }
        }
        return out;
    }

    public static List<String> filterFluidSelectors(List<String> selectors, RegistryAccess access) {
        List<String> out = new ArrayList<>();
        for (String selector : selectors) {
            if (isResolvableFluidSelector(selector, access)) {
                out.add(selector);
            } else {
                LOGGER.debug("Dropped unresolved fluid selector: {}", selector);
            }
        }
        return out;
    }

    @Nullable
    public static FuelDefinition sanitizeFuel(FuelDefinition def, RegistryAccess access) {
        List<String> inputs = filterItemSelectors(def.inputs(), access);
        if (inputs.isEmpty()) {
            String fallback = def.fuelId().toString();
            if (isResolvableItemSelector(fallback, access)) {
                inputs = List.of(fallback);
            } else {
                LOGGER.debug("Skipped fuel {}: no resolvable inputs", def.fuelId());
                return null;
            }
        }
        String output = def.output();
        if (output == null || output.isBlank() || !isResolvableItemSelector(output, access)) {
            LOGGER.debug("Skipped fuel {}: unresolved output '{}'", def.fuelId(), output);
            return null;
        }
        return new FuelDefinition(def.fuelId(), inputs, output, def.unitsPerFuel(), def.unitsPerWaste(),
                def.baseRfPerTick(), def.baseFuelUnitsPerTick(), def.overwritable());
    }

    @Nullable
    public static CoolantDefinition sanitizeCoolant(CoolantDefinition def, RegistryAccess access) {
        List<String> inputs = filterFluidSelectors(def.inputs(), access);
        if (inputs.isEmpty()) {
            String fallback = def.coolantId().toString();
            if (isResolvableFluidSelector(fallback, access)) {
                inputs = List.of(fallback);
            } else {
                LOGGER.debug("Skipped coolant {}: no resolvable inputs", def.coolantId());
                return null;
            }
        }
        String output = def.output();
        if (output == null || output.isBlank() || !isResolvableFluidSelector(output, access)) {
            LOGGER.debug("Skipped coolant {}: unresolved output '{}'", def.coolantId(), output);
            return null;
        }
        return new CoolantDefinition(def.coolantId(), inputs, output, def.rfIncrementPercent(), def.mbDecrementPercent(),
                def.reduceRfProduction(), def.rfToCoolantFactor(), def.steamPerCoolant(), def.overheatingMultiplier(),
                def.fluidColor(), def.outputColor(), def.overwritable());
    }

    @Nullable
    public static HeatSinkDefinition sanitizeHeatSink(HeatSinkDefinition def, RegistryAccess access) {
        List<String> blocks = filterBlockSelectors(def.validBlocks(), access);
        List<String> liquids = filterFluidSelectors(def.validLiquids(), access);
        if (blocks.isEmpty() && liquids.isEmpty()) {
            LOGGER.debug("Skipped heat sink entry: no resolvable valid_blocks or valid_liquids");
            return null;
        }
        return new HeatSinkDefinition(blocks, liquids, def.fuelMultiplier(), def.energyMultiplier(),
                def.overheatingMultiplier(), def.mustSource());
    }

    public static boolean isMelterRecipeResolvable(MelterRecipe recipe, RegistryAccess access) {
        boolean inputOk = recipe.inputIsTag()
                ? isResolvableItemSelector("#" + recipe.inputId(), access)
                : isResolvableItemSelector(recipe.inputId().toString(), access);
        boolean outputOk = recipe.outputIsTag()
                ? isResolvableFluidSelector("#" + recipe.outputFluidId(), access)
                : isResolvableFluidSelector(recipe.outputFluidId().toString(), access);
        if (!inputOk) LOGGER.debug("Skipped melter recipe: unresolved input {}", recipe.inputId());
        if (!outputOk) LOGGER.debug("Skipped melter recipe: unresolved output {}", recipe.outputFluidId());
        return inputOk && outputOk;
    }

    @Nullable
    public static MelterHeatEntry sanitizeMelterHeat(MelterHeatEntry entry, RegistryAccess access) {
        List<ResourceLocation> blockIds = new ArrayList<>();
        List<Boolean> blockIdIsTag = new ArrayList<>();
        for (int i = 0; i < entry.blockIds().size(); i++) {
            ResourceLocation id = entry.blockIds().get(i);
            boolean isTag = i < entry.blockIdIsTag().size() && entry.blockIdIsTag().get(i);
            String selector = isTag ? "#" + id : id.toString();
            if (isResolvableBlockSelector(selector, access)) {
                blockIds.add(id);
                blockIdIsTag.add(isTag);
            } else {
                LOGGER.debug("Dropped unresolved melter heat block selector: {}", selector);
            }
        }
        List<ResourceLocation> fluidIds = new ArrayList<>();
        List<Boolean> fluidIdIsTag = new ArrayList<>();
        for (int i = 0; i < entry.fluidIds().size(); i++) {
            ResourceLocation id = entry.fluidIds().get(i);
            boolean isTag = i < entry.fluidIdIsTag().size() && entry.fluidIdIsTag().get(i);
            String selector = isTag ? "#" + id : id.toString();
            if (isResolvableFluidSelector(selector, access)) {
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

    public static List<String> filterCatalystSelectors(List<String> selectors, RegistryAccess access) {
        return filterItemSelectors(selectors, access);
    }

    @Nullable
    public static ConsumeOption sanitizeConsumeOption(ConsumeOption opt, RegistryAccess access) {
        ConsumeOption.FluidRequirement fluid = opt.fluid();
        if (fluid != null) {
            String selector = fluid.isTag() ? "#" + fluid.tagOrId() : fluid.tagOrId().toString();
            if (!isResolvableFluidSelector(selector, access)) {
                LOGGER.debug("Dropped unresolved heating coil fluid selector: {}", selector);
                fluid = null;
            }
        }
        ConsumeOption.ItemRequirement item = opt.item();
        if (item != null) {
            String selector = item.isTag() ? "#" + item.tagOrId() : item.tagOrId().toString();
            if (!isResolvableItemSelector(selector, access)) {
                LOGGER.debug("Dropped unresolved heating coil item selector: {}", selector);
                item = null;
            }
        }
        if (fluid == null && item == null && opt.energy() == null && opt.burnable() == null) {
            return null;
        }
        return new ConsumeOption(fluid, item, opt.energy(), opt.burnable());
    }

    public static HeatingCoilDefinition sanitizeHeatingCoil(HeatingCoilDefinition def, RegistryAccess access) {
        List<ConsumeOption> consume = new ArrayList<>();
        for (ConsumeOption opt : def.consume()) {
            ConsumeOption sanitized = sanitizeConsumeOption(opt, access);
            if (sanitized != null && !sanitized.isEmpty()) {
                consume.add(sanitized);
            }
        }
        return new HeatingCoilDefinition(def.id(), def.duration(), consume, def.allSides(),
                def.noItem(), def.noFluid(), def.noEnergy());
    }
}
