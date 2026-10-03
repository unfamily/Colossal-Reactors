package net.unfamily.colossal_reactors.compat.rei;

import java.util.ArrayList;
import java.util.List;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import me.shedaniel.rei.api.common.util.EntryStacks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.compat.jei.CoolantJeiRecipe;
import net.unfamily.colossal_reactors.compat.jei.HeatingCoilJeiRecipe;
import net.unfamily.colossal_reactors.compat.jei.JeiIngredientsHelper;
import net.unfamily.colossal_reactors.compat.jei.JeiMedium;
import net.unfamily.colossal_reactors.compat.jei.JeiRecipeBackgroundDrawable;
import net.unfamily.colossal_reactors.compat.jei.TurbineJeiRecipe;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import net.unfamily.colossal_reactors.turbine.ElecCoilDefinition;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;

/**
 * Builds REI displays from the same data JEI uses (no EMI dependency on NeoForge 26.x).
 */
public final class ColossalReiDisplays {
    private ColossalReiDisplays() {}

    public static ColossalReiDisplay fuel(FuelDefinition recipe) {
        RegistryAccess access = registryAccess();
        List<EntryIngredient> inputs = new ArrayList<>();
        List<EntryIngredient> outputs = new ArrayList<>();
        if (access != null) {
            List<String> itemSelectors = new ArrayList<>();
            List<String> chemicalSelectors = new ArrayList<>();
            JeiIngredientsHelper.partitionSelectors(recipe.inputs(), itemSelectors, chemicalSelectors);
            addItems(inputs, JeiIngredientsHelper.withCount(
                    JeiIngredientsHelper.getFuelInputStacks(itemSelectors, access), recipe.consume()));
            String output = recipe.output();
            if (output == null || !net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector.isChemicalPrefix(output)) {
                addItems(outputs, JeiIngredientsHelper.withCount(
                        JeiIngredientsHelper.getWasteOutputStacks(output, access), recipe.produce()));
            }
        }
        return display(ColossalReiCategories.FUEL, recipe.fuelId(), inputs, outputs, (g, ox, oy) -> drawFuel(g, ox, oy, recipe));
    }

    public static ColossalReiDisplay coolant(CoolantJeiRecipe recipe) {
        RegistryAccess access = registryAccess();
        List<EntryIngredient> inputs = new ArrayList<>();
        List<EntryIngredient> outputs = new ArrayList<>();
        if (access != null && recipe.medium() == JeiMedium.LIQUID) {
            addFluids(inputs, JeiIngredientsHelper.getCoolantInputFluidStacks(recipe.inputSelectors(), access));
            List<FluidStack> out = new ArrayList<>();
            for (String sel : recipe.outputSelectors()) {
                out.addAll(JeiIngredientsHelper.getOutputFluidStacks(sel, access));
            }
            addFluids(outputs, out);
        }
        return display(ColossalReiCategories.COOLANT, recipe.jeiId(), inputs, outputs, (g, ox, oy) -> drawCoolant(g, ox, oy, recipe));
    }

    public static ColossalReiDisplay heatSink(HeatSinkDefinition recipe) {
        Identifier id = Identifier.fromNamespaceAndPath("colossal_reactors", "heat_sink/" + Integer.toHexString(recipe.hashCode()));
        RegistryAccess access = registryAccess();
        List<EntryIngredient> inputs = new ArrayList<>();
        if (access != null) {
            List<ItemStack> blocks = JeiIngredientsHelper.getBlockStacks(recipe.validBlocks(), access);
            List<FluidStack> liquids = JeiIngredientsHelper.getLiquidFluidStacks(recipe.validLiquids(), access);
            if (!blocks.isEmpty()) {
                addItems(inputs, blocks);
            } else {
                addFluids(inputs, liquids);
            }
        }
        return display(ColossalReiCategories.HEAT_SINK, id, inputs, List.of(), (g, ox, oy) -> drawHeatSink(g, ox, oy, recipe));
    }

    public static ColossalReiDisplay melter(MelterRecipe recipe) {
        Identifier id = Identifier.fromNamespaceAndPath("colossal_reactors", "melter/" + recipe.inputId().getPath());
        RegistryAccess access = registryAccess();
        List<EntryIngredient> inputs = new ArrayList<>();
        List<EntryIngredient> outputs = new ArrayList<>();
        if (access != null) {
            addItems(inputs, resolveMelterInputs(recipe, access));
            Fluid fluid = MelterRecipesLoader.getOutputFluid(recipe, access);
            if (fluid != null && fluid != Fluids.EMPTY) {
                addFluids(outputs, List.of(new FluidStack(fluid, 1000)));
            }
        }
        return display(ColossalReiCategories.MELTER, id, inputs, outputs, (g, ox, oy) -> drawMelter(g, ox, oy, recipe));
    }

    public static ColossalReiDisplay melterHeat(MelterHeatEntry entry) {
        Identifier id = Identifier.fromNamespaceAndPath("colossal_reactors", "melter_heat/" + Integer.toHexString(entry.hashCode()));
        RegistryAccess access = registryAccess();
        List<EntryIngredient> inputs = new ArrayList<>();
        if (access != null) {
            addItems(inputs, JeiIngredientsHelper.getBlockStacksFromMelterEntry(entry, access));
            addFluids(inputs, JeiIngredientsHelper.getFluidStacksFromMelterEntry(entry, access));
        }
        return display(ColossalReiCategories.MELTER_HEAT, id, inputs, List.of(), (g, ox, oy) -> drawMelterHeat(g, ox, oy, entry));
    }

    public static ColossalReiDisplay heatingCoil(HeatingCoilJeiRecipe recipe) {
        Identifier id = Identifier.fromNamespaceAndPath(
                "colossal_reactors", "heating_coil/" + recipe.coilId().getPath() + "/" + recipe.optionIndex());
        return display(ColossalReiCategories.HEATING_COIL, id, List.of(), List.of(), (g, ox, oy) -> drawHeatingCoil(g, ox, oy, recipe));
    }

    public static ColossalReiDisplay elecCoil(ElecCoilDefinition recipe) {
        Identifier id = Identifier.fromNamespaceAndPath("colossal_reactors", "elec_coil/" + Integer.toHexString(recipe.hashCode()));
        RegistryAccess access = registryAccess();
        List<EntryIngredient> inputs = new ArrayList<>();
        if (access != null) {
            addItems(inputs, JeiIngredientsHelper.getElecCoilDisplayStacks(recipe.validBlocks(), access));
        }
        return display(ColossalReiCategories.ELEC_COIL, id, inputs, List.of(), (g, ox, oy) -> drawElecCoil(g, ox, oy, recipe));
    }

    public static ColossalReiDisplay turbineGeneration(TurbineJeiRecipe recipe) {
        RegistryAccess access = registryAccess();
        List<EntryIngredient> inputs = new ArrayList<>();
        List<EntryIngredient> outputs = new ArrayList<>();
        if (access != null && recipe.medium() == JeiMedium.LIQUID) {
            addFluids(inputs, JeiIngredientsHelper.getTurbineGenerationInputFluids(recipe.inputSelectors(), access));
            List<FluidStack> out = new ArrayList<>();
            for (String sel : recipe.outputSelectors()) {
                out.addAll(JeiIngredientsHelper.getOutputFluidStacks(sel, access));
            }
            addFluids(outputs, out);
        }
        return display(ColossalReiCategories.TURBINE, recipe.jeiId(), inputs, outputs, (g, ox, oy) -> drawTurbine(g, ox, oy, recipe));
    }

    private static ColossalReiDisplay display(
            CategoryIdentifier<ColossalReiDisplay> category,
            Identifier id,
            List<EntryIngredient> inputs,
            List<EntryIngredient> outputs,
            TextDrawer drawer) {
        return new ColossalReiDisplay(category, id, inputs, outputs) {
            @Override
            void drawText(GuiGraphics graphics, int originX, int originY) {
                drawer.draw(graphics, originX, originY);
            }
        };
    }

    private static void drawFuel(GuiGraphics g, int ox, int oy, FuelDefinition recipe) {
        var font = Minecraft.getInstance().font;
        int textY = oy + JeiRecipeBackgroundDrawable.TEXT_Y;
        int lineHeight = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        int margin = ox + JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        int consume = recipe.consume();
        int produce = recipe.produce();
        g.text(font, Component.translatable("jei.colossal_reactors.consume_fuel", consume), margin, textY, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.produce_waste", produce), margin, textY + lineHeight, color, false);
        double fuelPower = recipe.baseRfPerTick() * Config.PRODUCTION_MULTIPLIER.get();
        g.text(font, Component.translatable("jei.colossal_reactors.fuel.power", formatNumber(fuelPower)),
                margin, textY + lineHeight * 2, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.fuel.consume_factor", formatNumber(recipe.baseFuelUnitsPerTick())),
                margin, textY + lineHeight * 3, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.fuel.units_per_fuel", recipe.unitsPerFuel()),
                margin, textY + lineHeight * 4, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.fuel.units_per_waste", recipe.unitsPerWaste()),
                margin, textY + lineHeight * 5, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.fuel.burn_to_waste", recipe.unitsPerWaste(), produce),
                margin, textY + lineHeight * 6, color, false);
    }

    private static void drawCoolant(GuiGraphics g, int ox, int oy, CoolantJeiRecipe recipe) {
        CoolantDefinition def = recipe.definition();
        var font = Minecraft.getInstance().font;
        int textY = oy + JeiRecipeBackgroundDrawable.TEXT_Y;
        int line = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        int margin = ox + JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        String[] ratio = JeiIngredientsHelper.formatSimplifiedRatio(def.mbMultiplier(), def.steamPerCoolant());
        g.text(font, Component.translatable("jei.colossal_reactors.consume_coolant", ratio[1]), margin, textY, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.produce_exhaust_coolant", ratio[0]), margin, textY + line, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.coolant.heat_reduction", formatMultiplier(def.overheatingMultiplier())),
                margin, textY + line * 2, color, false);
        Component rfBehavior = def.reduceRfProduction()
                ? Component.translatable("jei.colossal_reactors.coolant.suppress_rf_steam")
                : Component.translatable("jei.colossal_reactors.coolant.suppress_rf_none", formatMultiplier(def.rfMultiplier()));
        g.text(font, rfBehavior, margin, textY + line * 3, color, false);
    }

    private static void drawHeatSink(GuiGraphics g, int ox, int oy, HeatSinkDefinition recipe) {
        var font = Minecraft.getInstance().font;
        int textY = oy + JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = ox + JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int line = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        g.text(font, Component.translatable("jei.colossal_reactors.heat_sink.fuel_reduction", formatMultiplier(recipe.fuelMultiplier())),
                margin, textY, 0xFF404040, false);
        g.text(font, Component.translatable("jei.colossal_reactors.heat_sink.rf_increment", formatMultiplier(recipe.energyMultiplier())),
                margin, textY + line, 0xFF404040, false);
        g.text(font, Component.translatable("jei.colossal_reactors.heat_sink.heat_reduction", formatMultiplier(recipe.overheatingMultiplier())),
                margin, textY + 2 * line, 0xFF404040, false);
    }

    private static void drawMelter(GuiGraphics g, int ox, int oy, MelterRecipe recipe) {
        var font = Minecraft.getInstance().font;
        int textY = oy + JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = ox + JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int line = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        int color = 0xFF404040;
        g.text(font, Component.translatable("jei.colossal_reactors.melter.amount", recipe.amountMb()), margin, textY, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.melter.default_time",
                        JeiIngredientsHelper.formatDefaultDuration(recipe.timeTicks())),
                margin, textY + line, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.melter.heat_required_1"), margin, textY + line * 2, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.melter.heat_required_2"), margin, textY + line * 3, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.melter.heat_required_3"), margin, textY + line * 4, color, false);
    }

    private static void drawMelterHeat(GuiGraphics g, int ox, int oy, MelterHeatEntry entry) {
        var font = Minecraft.getInstance().font;
        int textY = oy + JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = ox + JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        g.text(font, Component.translatable("jei.colossal_reactors.melter_heat_source.factor", formatMultiplier(entry.factor())),
                margin, textY, 0xFF404040, false);
        if (entry.notValid()) {
            g.text(font, Component.translatable("jei.colossal_reactors.melter_heat_source.not_valid"),
                    margin, textY + 10, 0xFF808080, false);
        }
    }

    private static void drawHeatingCoil(GuiGraphics g, int ox, int oy, HeatingCoilJeiRecipe recipe) {
        var font = Minecraft.getInstance().font;
        int color = 0xFF404040;
        int textY = oy + 70;
        int margin = ox + JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int line = 0;
        int lineH = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        g.text(font, Component.translatable("jei.colossal_reactors.coil.duration", recipe.durationTicks()),
                margin, textY + (line++ * lineH), color, false);
        ConsumeOption opt = recipe.option();
        if (opt.fluid() != null) {
            g.text(font, Component.translatable("jei.colossal_reactors.coil.activate", opt.fluid().activation() + " mB"),
                    margin, textY + (line++ * lineH), color, false);
            g.text(font, Component.translatable("jei.colossal_reactors.coil.substain", opt.fluid().substain() + " mB"),
                    margin, textY + (line++ * lineH), color, false);
        }
        if (opt.item() != null) {
            g.text(font, Component.translatable("jei.colossal_reactors.coil.activate", opt.item().activation()),
                    margin, textY + (line++ * lineH), color, false);
            g.text(font, Component.translatable("jei.colossal_reactors.coil.substain", opt.item().substain()),
                    margin, textY + (line++ * lineH), color, false);
        }
        if (opt.burnable() != null) {
            g.text(font, Component.translatable("jei.colossal_reactors.coil.activate", opt.burnable().activation() + " t"),
                    margin, textY + (line++ * lineH), color, false);
            g.text(font, Component.translatable("jei.colossal_reactors.coil.substain", opt.burnable().substain() + " t"),
                    margin, textY + (line++ * lineH), color, false);
        }
        if (opt.energy() != null) {
            g.text(font, Component.translatable("jei.colossal_reactors.coil.activate", opt.energy().activation() + " RF"),
                    margin, textY + (line++ * lineH), color, false);
            g.text(font, Component.translatable("jei.colossal_reactors.coil.substain", opt.energy().substain() + " RF"),
                    margin, textY + (line++ * lineH), color, false);
        }
    }

    private static void drawElecCoil(GuiGraphics g, int ox, int oy, ElecCoilDefinition recipe) {
        var font = Minecraft.getInstance().font;
        int textY = oy + JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = ox + JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        g.text(font, Component.translatable("jei.colossal_reactors.elec_coil.eff_coe", formatMultiplier(recipe.effCoe())),
                margin, textY, color, false);
        g.text(font, Component.translatable("jei.colossal_reactors.elec_coil.eff_max", formatMultiplier(recipe.effMax())),
                margin, textY + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT, color, false);
    }

    private static void drawTurbine(GuiGraphics g, int ox, int oy, TurbineJeiRecipe recipe) {
        TurbineGenerationDefinition def = recipe.definition();
        var font = Minecraft.getInstance().font;
        g.text(font, Component.translatable("jei.colossal_reactors.turbine_generation.rf_per_bucket",
                        TurbineGenerationLoader.formatRfPerSteamBucket(def.rfProduction())),
                ox + JeiRecipeBackgroundDrawable.TEXT_MARGIN,
                oy + JeiRecipeBackgroundDrawable.TEXT_Y,
                0xFF404040, false);
    }

    private static List<ItemStack> resolveMelterInputs(MelterRecipe recipe, RegistryAccess access) {
        Identifier id = recipe.inputId();
        if (id == null) {
            return List.of();
        }
        int count = Math.max(1, recipe.count());
        List<ItemStack> out = new ArrayList<>();
        if (recipe.inputIsTag()) {
            var tagKey = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.ITEM, id);
            access.lookup(net.minecraft.core.registries.Registries.ITEM).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> out.add(new ItemStack(h.value(), count)))));
        } else {
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id).map(h -> h.value())
                    .orElse(net.minecraft.world.item.Items.AIR);
            if (item != net.minecraft.world.item.Items.AIR) {
                out.add(new ItemStack(item, count));
            }
        }
        return out;
    }

    private static void addItems(List<EntryIngredient> target, List<ItemStack> stacks) {
        if (stacks != null && !stacks.isEmpty()) {
            target.add(EntryIngredients.ofItemStacks(stacks));
        }
    }

    private static void addFluids(List<EntryIngredient> target, List<FluidStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return;
        }
        List<EntryStack<?>> entries = new ArrayList<>();
        for (FluidStack stack : stacks) {
            if (stack != null && !stack.isEmpty()) {
                entries.add(EntryStacks.of(stack.getFluid()));
            }
        }
        if (!entries.isEmpty()) {
            target.add(EntryIngredient.of(entries));
        }
    }

    private static RegistryAccess registryAccess() {
        Level level = Minecraft.getInstance().level;
        return level != null ? level.registryAccess() : null;
    }

    private static String formatMultiplier(double value) {
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.2f", value);
    }

    private static String formatNumber(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        if (value == (long) value) {
            return String.valueOf((long) value);
        }
        return String.format("%.1f", value);
    }

    @FunctionalInterface
    interface TextDrawer {
        void draw(GuiGraphics graphics, int originX, int originY);
    }
}
