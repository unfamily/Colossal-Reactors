package net.unfamily.colossal_reactors.compat.jei;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.RegistryAccess;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Resolves script selectors (tags or registry ids) to ingredient lists for JEI display.
 * Also provides ratio simplification for consume/produce labels.
 */
public final class JeiIngredientsHelper {

    private static final int DISPLAY_AMOUNT_MB = 1000;
    private static final int DISPLAY_AMOUNT_ITEMS = 1;

    private JeiIngredientsHelper() {}

    /**
     * Simplifies the consume/produce ratio by dividing both by the max until one is 1 or less.
     * Returns two formatted strings: integer if whole, otherwise 2 decimals.
     */
    public static String[] formatSimplifiedRatio(double consume, double produce) {
        double max = Math.max(consume, produce);
        if (max <= 0) return new String[] { "0", "0" };
        double c = consume / max;
        double p = produce / max;
        return new String[] { formatRatioValue(c), formatRatioValue(p) };
    }

    private static String formatRatioValue(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }

    /** Resolves coolant input selectors to bucket item stacks for JEI (excluded inputs skipped). */
    public static List<ItemStack> getCoolantInputBuckets(List<String> inputs, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        for (String input : inputs) {
            if (CoolantLoader.isInputExcluded(input)) continue;
            list.addAll(selectorToBucketStacks(input, registryAccess));
        }
        return list;
    }

    /** Resolves coolant input selectors to fluid stacks (1000 mB display) for JEI (excluded inputs skipped). */
    public static List<FluidStack> getCoolantInputFluidStacks(List<String> inputs, RegistryAccess registryAccess) {
        List<FluidStack> list = new ArrayList<>();
        for (String input : inputs) {
            if (CoolantLoader.isInputExcluded(input)) continue;
            list.addAll(selectorToFluidStacks(input, registryAccess));
        }
        return list;
    }

    /** Resolves one output selector (e.g. "#c:steam") to bucket item stacks for JEI. */
    public static List<ItemStack> getOutputBuckets(String output, RegistryAccess registryAccess) {
        if (output == null || output.isEmpty()) return List.of();
        return selectorToBucketStacks(output, registryAccess);
    }

    /** Resolves one output selector to fluid stacks (1000 mB display) for JEI. */
    public static List<FluidStack> getOutputFluidStacks(String output, RegistryAccess registryAccess) {
        if (output == null || output.isEmpty()) return List.of();
        return selectorToFluidStacks(output, registryAccess);
    }

    /** Resolves fuel input selectors to item stacks (excluded inputs skipped). */
    public static List<ItemStack> getFuelInputStacks(List<String> inputs, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        for (String input : inputs) {
            if (FuelLoader.isInputExcluded(input)) continue;
            list.addAll(selectorToItemStacks(input, registryAccess));
        }
        return list;
    }

    /** Resolves one output selector (waste) to item stacks. */
    public static List<ItemStack> getWasteOutputStacks(String output, RegistryAccess registryAccess) {
        if (output == null || output.isEmpty()) return List.of();
        return selectorToItemStacks(output, registryAccess);
    }

    /** Resolves block selectors (valid_blocks) to item stacks (block as item). */
    public static List<ItemStack> getBlockStacks(List<String> validBlocks, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        for (String selector : validBlocks) {
            list.addAll(blockSelectorToItemStacks(selector, registryAccess));
        }
        return list;
    }

    /** Resolves liquid selectors (valid_liquids) to bucket item stacks for JEI. */
    public static List<ItemStack> getLiquidBuckets(List<String> validLiquids, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        for (String selector : validLiquids) {
            list.addAll(selectorToBucketStacks(selector, registryAccess));
        }
        return list;
    }

    /** Resolves liquid selectors (valid_liquids) to fluid stacks (1000 mB display) for JEI. */
    public static List<FluidStack> getLiquidFluidStacks(List<String> validLiquids, RegistryAccess registryAccess) {
        List<FluidStack> list = new ArrayList<>();
        for (String selector : validLiquids) {
            list.addAll(selectorToFluidStacks(selector, registryAccess));
        }
        return list;
    }

    /** Converts fluid selectors to bucket ItemStacks (one bucket per fluid) for JEI display. */
    private static List<ItemStack> selectorToBucketStacks(String selector, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        for (FluidStack fs : selectorToFluidStacks(selector, registryAccess)) {
            ItemStack bucket = fluidToBucket(fs.getFluid());
            if (!bucket.isEmpty()) list.add(bucket);
        }
        return list;
    }

    private static ItemStack fluidToBucket(Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return ItemStack.EMPTY;
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack stack = new ItemStack(item, 1);
            FluidStack in = FluidUtil.getFirstStackContained(stack);
            if (!in.isEmpty() && in.getFluid() == fluid) {
                return new ItemStack(item, DISPLAY_AMOUNT_ITEMS);
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<FluidStack> selectorToFluidStacks(String selector, RegistryAccess registryAccess) {
        List<FluidStack> list = new ArrayList<>();
        if (selector.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(selector.substring(1));
            if (tagId == null) return list;
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
            registryAccess.lookup(Registries.FLUID).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> list.add(new FluidStack(h.value(), DISPLAY_AMOUNT_MB)))));
        } else {
            Identifier id = Identifier.tryParse(selector);
            if (id != null) {
                Fluid fluid = BuiltInRegistries.FLUID.get(id).map(h -> h.value()).orElse(Fluids.EMPTY);
                if (fluid != Fluids.EMPTY) {
                    list.add(new FluidStack(fluid, DISPLAY_AMOUNT_MB));
                }
            }
        }
        return list;
    }

    private static List<ItemStack> selectorToItemStacks(String selector, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        if (selector.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(selector.substring(1));
            if (tagId == null) return list;
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            registryAccess.lookup(Registries.ITEM).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> list.add(new ItemStack(h.value(), DISPLAY_AMOUNT_ITEMS)))));
        } else {
            Identifier id = Identifier.tryParse(selector);
            if (id != null) {
                Item item = BuiltInRegistries.ITEM.get(id).map(h -> h.value()).orElse(Items.AIR);
                if (item != Items.AIR) {
                    list.add(new ItemStack(item, DISPLAY_AMOUNT_ITEMS));
                }
            }
        }
        return list;
    }

    private static List<ItemStack> blockSelectorToItemStacks(String selector, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        if (selector.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(selector.substring(1));
            if (tagId == null) return list;
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
            registryAccess.lookup(Registries.BLOCK).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> {
                                ItemStack stack = new ItemStack(h.value().asItem(), DISPLAY_AMOUNT_ITEMS);
                                if (!stack.isEmpty()) list.add(stack);
                            })));
        } else {
            Identifier id = Identifier.tryParse(selector);
            if (id != null) {
                Block block = BuiltInRegistries.BLOCK.get(id).map(h -> h.value()).orElse(Blocks.AIR);
                if (block != Blocks.AIR) {
                    list.add(new ItemStack(block.asItem(), DISPLAY_AMOUNT_ITEMS));
                }
            }
        }
        return list;
    }

    /** Resolves MelterHeatEntry block ids/tags to item stacks (block as item) for JEI. */
    public static List<ItemStack> getBlockStacksFromMelterEntry(MelterHeatEntry entry, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        var blockIds = entry.blockIds();
        var blockIdIsTag = entry.blockIdIsTag();
        for (int i = 0; i < blockIds.size(); i++) {
            Identifier id = blockIds.get(i);
            boolean isTag = i < blockIdIsTag.size() && blockIdIsTag.get(i);
            if (isTag) {
                TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, id);
                registryAccess.lookup(Registries.BLOCK).ifPresent(lookup ->
                        lookup.get(tagKey).ifPresent(holders ->
                                holders.forEach(h -> {
                                    ItemStack stack = new ItemStack(h.value().asItem(), DISPLAY_AMOUNT_ITEMS);
                                    if (!stack.isEmpty()) list.add(stack);
                                })));
            } else {
                Block block = BuiltInRegistries.BLOCK.get(id).map(h -> h.value()).orElse(Blocks.AIR);
                if (block != Blocks.AIR) {
                    list.add(new ItemStack(block.asItem(), DISPLAY_AMOUNT_ITEMS));
                }
            }
        }
        return list;
    }

    /** Resolves MelterHeatEntry fluid ids/tags to fluid stacks (1000 mB) for JEI. */
    public static List<FluidStack> getFluidStacksFromMelterEntry(MelterHeatEntry entry, RegistryAccess registryAccess) {
        List<FluidStack> list = new ArrayList<>();
        var fluidIds = entry.fluidIds();
        var fluidIdIsTag = entry.fluidIdIsTag();
        for (int i = 0; i < fluidIds.size(); i++) {
            Identifier id = fluidIds.get(i);
            boolean isTag = i < fluidIdIsTag.size() && fluidIdIsTag.get(i);
            if (isTag) {
                TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, id);
                registryAccess.lookup(Registries.FLUID).ifPresent(lookup ->
                        lookup.get(tagKey).ifPresent(holders ->
                                holders.forEach(h -> list.add(new FluidStack(h.value(), DISPLAY_AMOUNT_MB)))));
            } else {
                Fluid fluid = BuiltInRegistries.FLUID.get(id).map(h -> h.value()).orElse(Fluids.EMPTY);
                if (fluid != Fluids.EMPTY) {
                    list.add(new FluidStack(fluid, DISPLAY_AMOUNT_MB));
                }
            }
        }
        return list;
    }
}
