package net.unfamily.colossal_reactors.compat.jei;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.core.RegistryAccess;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

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

    public static boolean jeiChemicalsAvailable() {
        return ModList.get().isLoaded("mekanism") && ModList.get().isLoaded("jei");
    }

    /** Mek {@link mekanism.api.chemical.ChemicalStack} ingredient type ({@code MekanismJEI#TYPE_CHEMICAL}). */
    @SuppressWarnings("unchecked")
    public static <T> IIngredientType<T> getMekChemicalIngredientType() {
        if (!jeiChemicalsAvailable()) return null;
        try {
            Class<?> cls = Class.forName("mekanism.client.recipe_viewer.jei.MekanismJEI");
            return (IIngredientType<T>) cls.getField("TYPE_CHEMICAL").get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static List<Object> getChemicalStacks(List<String> selectors) {
        List<Object> list = new ArrayList<>();
        if (!MekChemicalHelper.isLoaded()) return list;
        for (String selector : selectors) {
            if (!MaterialSelector.isChemicalPrefix(selector)) continue;
            list.addAll(MekChemicalHelper.stacksForSelector(selector));
        }
        return list;
    }

    public static void partitionSelectors(List<String> selectors, List<String> fluidOrItem, List<String> chemical) {
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) continue;
            if (MaterialSelector.isChemicalPrefix(selector)) {
                chemical.add(selector);
            } else {
                fluidOrItem.add(selector);
            }
        }
    }

    /**
     * Adds a Mek gas slot when selectors resolve (same renderer as Mek JEI chemical tank).
     */
    public static void addChemicalSlot(
            IRecipeLayoutBuilder builder,
            RecipeIngredientRole role,
            int slotX,
            int slotY,
            List<String> chemicalSelectors) {
        if (chemicalSelectors.isEmpty()) return;
        IIngredientType<Object> type = getMekChemicalIngredientType();
        List<Object> stacks = getChemicalStacks(chemicalSelectors);
        if (type == null || stacks.isEmpty()) return;
        builder.addSlot(role, slotX + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                        slotY + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y)
                .addIngredients(type, stacks);
    }

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

    /**
     * Formats recipe duration ticks as hours/minutes/seconds plus remainder ticks (e.g. {@code 2m 30s + 10 t}).
     */
    public static String formatDefaultDuration(int ticks) {
        if (ticks <= 0) return "0s";
        int remainderTicks = ticks % 20;
        int totalSeconds = ticks / 20;
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append('h').append(' ');
        if (minutes > 0 || hours > 0) sb.append(minutes).append('m').append(' ');
        sb.append(seconds).append('s');
        if (remainderTicks > 0) sb.append(" + ").append(remainderTicks).append(" t");
        return sb.toString().trim();
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

    /** Display-only stack for {@code minecraft:air} heat sink entries (no block item in vanilla). */
    public static ItemStack heatSinkAirInteriorDisplayStack() {
        ItemStack stack = new ItemStack(Items.STRUCTURE_VOID);
        stack.set(DataComponents.CUSTOM_NAME, Component.translatable("jei.colossal_reactors.heat_sink.air_interior"));
        return stack;
    }

    /** First display stack for an elec coil entry (skips air-only selectors). */
    public static List<ItemStack> getElecCoilDisplayStacks(List<String> validBlocks, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        for (String selector : validBlocks) {
            if (selector != null && selector.startsWith("#")) {
                list.addAll(blockSelectorToItemStacks(selector, registryAccess));
            } else if (selector != null) {
                ResourceLocation id = ResourceLocation.tryParse(selector);
                if (id != null && "minecraft".equals(id.getNamespace()) && "air".equals(id.getPath())) {
                    list.add(heatSinkAirInteriorDisplayStack());
                } else {
                    list.addAll(blockSelectorToItemStacks(selector, registryAccess));
                }
            }
            if (!list.isEmpty()) break;
        }
        return list;
    }

    /** Resolves turbine generation input selectors to fluid stacks for JEI. */
    public static List<FluidStack> getTurbineGenerationInputFluids(List<String> inputs, RegistryAccess registryAccess) {
        List<FluidStack> list = new ArrayList<>();
        for (String input : inputs) {
            if (input == null || input.isBlank()) continue;
            list.addAll(selectorToFluidStacks(input, registryAccess));
            if (!list.isEmpty()) break;
        }
        return list;
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
            IFluidHandlerItem cap = stack.getCapability(Capabilities.FluidHandler.ITEM);
            if (cap != null) {
                FluidStack in = cap.getFluidInTank(0);
                if (!in.isEmpty() && in.getFluid() == fluid) {
                    return new ItemStack(item, DISPLAY_AMOUNT_ITEMS);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    private static List<FluidStack> selectorToFluidStacks(String selector, RegistryAccess registryAccess) {
        List<FluidStack> list = new ArrayList<>();
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return list;
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, tagId);
            registryAccess.lookup(Registries.FLUID).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> list.add(new FluidStack(h.value(), DISPLAY_AMOUNT_MB)))));
        } else {
            ResourceLocation id = ResourceLocation.tryParse(selector);
            if (id != null) {
                Fluid fluid = BuiltInRegistries.FLUID.get(id);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    list.add(new FluidStack(fluid, DISPLAY_AMOUNT_MB));
                }
            }
        }
        return list;
    }

    private static List<ItemStack> selectorToItemStacks(String selector, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return list;
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            registryAccess.lookup(Registries.ITEM).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> list.add(new ItemStack(h.value(), DISPLAY_AMOUNT_ITEMS)))));
        } else {
            ResourceLocation id = ResourceLocation.tryParse(selector);
            if (id != null) {
                Item item = BuiltInRegistries.ITEM.get(id);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    list.add(new ItemStack(item, DISPLAY_AMOUNT_ITEMS));
                }
            }
        }
        return list;
    }

    private static List<ItemStack> blockSelectorToItemStacks(String selector, RegistryAccess registryAccess) {
        List<ItemStack> list = new ArrayList<>();
        if (selector.startsWith("#")) {
            ResourceLocation tagId = ResourceLocation.tryParse(selector.substring(1));
            if (tagId == null) return list;
            TagKey<Block> tagKey = TagKey.create(Registries.BLOCK, tagId);
            registryAccess.lookup(Registries.BLOCK).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> {
                                ItemStack stack = new ItemStack(h.value().asItem(), DISPLAY_AMOUNT_ITEMS);
                                if (!stack.isEmpty()) list.add(stack);
                            })));
        } else {
            ResourceLocation id = ResourceLocation.tryParse(selector);
            if (id != null) {
                Block block = BuiltInRegistries.BLOCK.get(id);
                if (block != null && block == Blocks.AIR) {
                    list.add(heatSinkAirInteriorDisplayStack());
                } else if (block != null && block != Blocks.AIR) {
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
            ResourceLocation id = blockIds.get(i);
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
                Block block = BuiltInRegistries.BLOCK.get(id);
                if (block != null && block != net.minecraft.world.level.block.Blocks.AIR) {
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
            ResourceLocation id = fluidIds.get(i);
            boolean isTag = i < fluidIdIsTag.size() && fluidIdIsTag.get(i);
            if (isTag) {
                TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, id);
                registryAccess.lookup(Registries.FLUID).ifPresent(lookup ->
                        lookup.get(tagKey).ifPresent(holders ->
                                holders.forEach(h -> list.add(new FluidStack(h.value(), DISPLAY_AMOUNT_MB)))));
            } else {
                Fluid fluid = BuiltInRegistries.FLUID.get(id);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    list.add(new FluidStack(fluid, DISPLAY_AMOUNT_MB));
                }
            }
        }
        return list;
    }

    /** First resolvable hover name for a script selector (tag, block id, fluid, item, or Mek chemical). */
    public static Component displayNameForSelector(String selector, RegistryAccess registryAccess) {
        if (selector == null || selector.isBlank()) {
            return Component.literal("?");
        }
        if (MaterialSelector.isChemicalPrefix(selector)) {
            List<Object> chem = getChemicalStacks(List.of(selector));
            if (!chem.isEmpty()) {
                Object stack = chem.get(0);
                try {
                    var name = stack.getClass().getMethod("getHoverName");
                    Object result = name.invoke(stack);
                    if (result instanceof Component c) {
                        return c;
                    }
                } catch (Throwable ignored) {
                    // fall through
                }
            }
        }
        List<ItemStack> stacks = new ArrayList<>();
        stacks.addAll(blockSelectorToItemStacks(selector, registryAccess));
        if (stacks.isEmpty()) {
            stacks.addAll(selectorToItemStacks(selector, registryAccess));
        }
        if (stacks.isEmpty() && !MaterialSelector.isChemicalPrefix(selector)) {
            List<FluidStack> fluids = selectorToFluidStacks(selector, registryAccess);
            if (!fluids.isEmpty()) {
                Fluid fluid = fluids.get(0).getFluid();
                if (fluid != null && fluid != Fluids.EMPTY) {
                    return Component.translatable(fluid.getFluidType().getDescriptionId());
                }
            }
        }
        if (stacks.isEmpty()) {
            stacks.addAll(selectorToBucketStacks(selector, registryAccess));
        }
        if (!stacks.isEmpty()) {
            return stacks.get(0).getHoverName();
        }
        return Component.literal(selector);
    }

    /** First non-empty display name from an ordered selector list (builder GUI cycling). */
    public static Component displayNameForFirstSelector(List<String> selectors, RegistryAccess registryAccess) {
        if (selectors == null || registryAccess == null) {
            return Component.literal("?");
        }
        for (String selector : selectors) {
            if (selector == null || selector.isBlank()) {
                continue;
            }
            Component name = displayNameForSelector(selector, registryAccess);
            if (!name.getString().isEmpty() && !name.getString().equals(selector)) {
                return name;
            }
            List<ItemStack> stacks = getBlockStacks(List.of(selector), registryAccess);
            if (!stacks.isEmpty()) {
                return stacks.get(0).getHoverName();
            }
            stacks = getElecCoilDisplayStacks(List.of(selector), registryAccess);
            if (!stacks.isEmpty()) {
                return stacks.get(0).getHoverName();
            }
        }
        return Component.literal("?");
    }
}
