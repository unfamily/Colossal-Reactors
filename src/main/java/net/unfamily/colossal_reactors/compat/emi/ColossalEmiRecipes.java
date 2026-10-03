package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.compat.jei.CoolantJeiRecipe;
import net.unfamily.colossal_reactors.compat.jei.HeatingCoilJeiRecipe;
import net.unfamily.colossal_reactors.compat.jei.JeiHeatingCoilBackgroundDrawable;
import net.unfamily.colossal_reactors.compat.jei.JeiIngredientsHelper;
import net.unfamily.colossal_reactors.compat.jei.JeiMedium;
import net.unfamily.colossal_reactors.compat.jei.JeiRecipeBackgroundDrawable;
import net.unfamily.colossal_reactors.compat.jei.TurbineJeiRecipe;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import net.unfamily.colossal_reactors.turbine.ElecCoilDefinition;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;

/** EMI recipe factories aligned with JEI categories. */
public final class ColossalEmiRecipes {
    private ColossalEmiRecipes() {}

    public static EmiRecipe coolant(CoolantJeiRecipe recipe) {
        return new SimpleEmiRecipe(EmiCategories.COOLANT, recipe.jeiId(), 180, 62, true) {
            @Override
            protected void addContent(WidgetHolder widgets) {
                var reg = EmiStackHelper.registryOrThrow();
                if (recipe.medium() == JeiMedium.LIQUID) {
                    addIn(EmiStackHelper.ingredientOfFluids(JeiIngredientsHelper.getCoolantInputFluidStacks(recipe.inputSelectors(), reg)));
                    List<FluidStack> out = new ArrayList<>();
                    for (String sel : recipe.outputSelectors()) {
                        out.addAll(JeiIngredientsHelper.getOutputFluidStacks(sel, reg));
                    }
                    addOut(EmiStackHelper.ingredientOfFluids(out));
                } else {
                    addIn(EmiStackHelper.ingredientOfChemicalSelectors(recipe.inputSelectors()));
                    addOut(EmiStackHelper.ingredientOfChemicalSelectors(recipe.outputSelectors()));
                }
            }

            @Override
            protected void drawText(GuiGraphics g) {
                var def = recipe.definition();
                var font = Minecraft.getInstance().font;
                int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
                int line2 = textY + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
                int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
                int color = 0xFF404040;
                String[] ratio = JeiIngredientsHelper.formatSimplifiedRatio(def.mbMultiplier(), def.steamPerCoolant());
                g.drawString(font, Component.translatable("jei.colossal_reactors.consume_coolant", ratio[1]), margin, textY, color, false);
                g.drawString(font, Component.translatable("jei.colossal_reactors.produce_exhaust_coolant", ratio[0]), margin, line2, color, false);
                int line3 = line2 + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
                int line4 = line3 + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
                g.drawString(font, Component.translatable("jei.colossal_reactors.coolant.heat_reduction", fmt(def.overheatingMultiplier())), margin, line3, color, false);
                Component rf = def.reduceRfProduction()
                        ? Component.translatable("jei.colossal_reactors.coolant.suppress_rf_steam")
                        : Component.translatable("jei.colossal_reactors.coolant.suppress_rf_none", fmt(def.rfMultiplier()));
                g.drawString(font, rf, margin, line4, color, false);
            }
        };
    }

    public static EmiRecipe heatSink(HeatSinkDefinition recipe) {
        Identifier id = Identifier.fromNamespaceAndPath("colossal_reactors", "heat_sink/" + Integer.toHexString(recipe.hashCode()));
        return new SimpleEmiRecipe(EmiCategories.HEAT_SINK, id, 180, 54, false) {
            @Override
            protected void addContent(WidgetHolder widgets) {
                var reg = EmiStackHelper.registryOrThrow();
                List<ItemStack> blocks = JeiIngredientsHelper.getBlockStacks(recipe.validBlocks(), reg);
                List<FluidStack> fluids = JeiIngredientsHelper.getLiquidFluidStacks(recipe.validLiquids(), reg);
                if (!blocks.isEmpty()) {
                    addIn(EmiStackHelper.ingredientOf(blocks));
                } else if (!fluids.isEmpty()) {
                    addIn(EmiStackHelper.ingredientOfFluids(fluids));
                }
            }

            @Override
            protected void drawText(GuiGraphics g) {
                var font = Minecraft.getInstance().font;
                int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
                int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
                int lh = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
                int color = 0xFF404040;
                g.drawString(font, Component.translatable("jei.colossal_reactors.heat_sink.fuel_reduction", fmt(recipe.fuelMultiplier())), margin, textY, color, false);
                g.drawString(font, Component.translatable("jei.colossal_reactors.heat_sink.rf_increment", fmt(recipe.energyMultiplier())), margin, textY + lh, color, false);
                g.drawString(font, Component.translatable("jei.colossal_reactors.heat_sink.heat_reduction", fmt(recipe.overheatingMultiplier())), margin, textY + 2 * lh, color, false);
            }
        };
    }

    public static EmiRecipe melter(MelterRecipe recipe) {
        Identifier id = Identifier.fromNamespaceAndPath("colossal_reactors", "melter/" + recipe.inputId().getPath());
        return new SimpleEmiRecipe(EmiCategories.MELTER, id, 180, 78, true) {
            @Override
            protected void addContent(WidgetHolder widgets) {
                var reg = EmiStackHelper.registryOrThrow();
                List<ItemStack> inputs = MelterRecipeCategoryHelper.resolveInputs(recipe, reg);
                addIn(EmiStackHelper.ingredientOf(inputs));
                var fluid = MelterRecipesLoader.getOutputFluid(recipe, reg);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    addOut(EmiStackHelper.outputOf(new FluidStack(fluid, 1000)));
                }
            }

            @Override
            protected void drawText(GuiGraphics g) {
                var font = Minecraft.getInstance().font;
                int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
                int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
                int lh = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
                int color = 0xFF404040;
                g.drawString(font, Component.translatable("jei.colossal_reactors.melter.amount", recipe.amountMb()), margin, textY, color, false);
                g.drawString(font, Component.translatable("jei.colossal_reactors.melter.default_time", JeiIngredientsHelper.formatDefaultDuration(recipe.timeTicks())), margin, textY + lh, color, false);
                g.drawString(font, Component.translatable("jei.colossal_reactors.melter.heat_required_1"), margin, textY + lh * 2, color, false);
                g.drawString(font, Component.translatable("jei.colossal_reactors.melter.heat_required_2"), margin, textY + lh * 3, color, false);
                g.drawString(font, Component.translatable("jei.colossal_reactors.melter.heat_required_3"), margin, textY + lh * 4, color, false);
            }
        };
    }

    public static EmiRecipe melterHeat(MelterHeatEntry entry) {
        Identifier id = Identifier.fromNamespaceAndPath("colossal_reactors", "melter_heat/" + Integer.toHexString(entry.hashCode()));
        return new SimpleEmiRecipe(EmiCategories.MELTER_HEAT, id, 180, 52, false) {
            @Override
            protected void addContent(WidgetHolder widgets) {
                var reg = EmiStackHelper.registryOrThrow();
                List<ItemStack> blocks = JeiIngredientsHelper.getBlockStacksFromMelterEntry(entry, reg);
                List<FluidStack> fluids = JeiIngredientsHelper.getFluidStacksFromMelterEntry(entry, reg);
                if (!blocks.isEmpty()) {
                    addIn(EmiStackHelper.ingredientOf(blocks));
                } else if (!fluids.isEmpty()) {
                    addIn(EmiStackHelper.ingredientOfFluids(fluids));
                }
            }

            @Override
            protected void drawText(GuiGraphics g) {
                var font = Minecraft.getInstance().font;
                int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
                int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
                int color = 0xFF404040;
                String factorStr = entry.factor() == (long) entry.factor() ? String.valueOf((long) entry.factor()) : String.format("%.2f", entry.factor());
                g.drawString(font, Component.translatable("jei.colossal_reactors.melter_heat.factor", factorStr), margin, textY, color, false);
            }
        };
    }

    public static EmiRecipe elecCoil(ElecCoilDefinition recipe) {
        Identifier id = Identifier.fromNamespaceAndPath("colossal_reactors", "elec_coil/" + Integer.toHexString(recipe.hashCode()));
        return new SimpleEmiRecipe(EmiCategories.ELEC_COIL, id, 180, 54, false) {
            @Override
            protected void addContent(WidgetHolder widgets) {
                var reg = EmiStackHelper.registryOrThrow();
                addIn(EmiStackHelper.ingredientOf(JeiIngredientsHelper.getElecCoilDisplayStacks(recipe.validBlocks(), reg)));
            }

            @Override
            protected void drawText(GuiGraphics g) {
                var font = Minecraft.getInstance().font;
                int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
                int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
                int color = 0xFF404040;
                g.drawString(font, Component.translatable("jei.colossal_reactors.elec_coil.eff_coe", fmt(recipe.effCoe())), margin, textY, color, false);
                g.drawString(font, Component.translatable("jei.colossal_reactors.elec_coil.eff_max", fmt(recipe.effMax())), margin, textY + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT, color, false);
            }
        };
    }

    public static EmiRecipe turbineGeneration(TurbineJeiRecipe recipe) {
        return new SimpleEmiRecipe(EmiCategories.TURBINE_GENERATION, recipe.jeiId(), 180, 54, true) {
            @Override
            protected void addContent(WidgetHolder widgets) {
                var reg = EmiStackHelper.registryOrThrow();
                TurbineGenerationDefinition def = recipe.definition();
                if (recipe.medium() == JeiMedium.LIQUID) {
                    addIn(EmiStackHelper.ingredientOfFluids(JeiIngredientsHelper.getTurbineGenerationInputFluids(recipe.inputSelectors(), reg)));
                    List<FluidStack> out = new ArrayList<>();
                    for (String sel : recipe.outputSelectors()) {
                        out.addAll(JeiIngredientsHelper.getOutputFluidStacks(sel, reg));
                    }
                    if (out.isEmpty() && def.liquidOutputSelector() != null) {
                        out.addAll(JeiIngredientsHelper.getOutputFluidStacks(def.liquidOutputSelector(), reg));
                    }
                    addOut(EmiStackHelper.ingredientOfFluids(out));
                } else {
                    addIn(EmiStackHelper.ingredientOfChemicalSelectors(recipe.inputSelectors()));
                    addOut(EmiStackHelper.ingredientOfChemicalSelectors(recipe.outputSelectors()));
                }
            }

            @Override
            protected void drawText(GuiGraphics g) {
                TurbineGenerationDefinition def = recipe.definition();
                var font = Minecraft.getInstance().font;
                int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
                int color = 0xFF404040;
                g.drawString(font, Component.translatable("jei.colossal_reactors.turbine_generation.rf_per_bucket",
                        TurbineGenerationLoader.formatRfPerSteamBucket(def.rfProduction())), margin, JeiRecipeBackgroundDrawable.TEXT_Y, color, false);
            }
        };
    }

    public static EmiRecipe heatingCoil(HeatingCoilJeiRecipe recipe) {
        Identifier id = Identifier.fromNamespaceAndPath("colossal_reactors", "heating_coil/" + recipe.coilId().getPath() + "/" + recipe.optionIndex());
        return new EmiRecipe() {
            private final EmiIngredient input = buildInputs(recipe);
            private final EmiStack output = buildOutput(recipe);

            @Override
            public EmiRecipeCategory getCategory() { return EmiCategories.HEATING_COIL; }
            @Override
            public Identifier getId() { return id; }
            @Override
            public List<EmiIngredient> getInputs() { return List.of(input); }
            @Override
            public List<EmiStack> getOutputs() { return output.isEmpty() ? List.of() : List.of(output); }
            @Override
            public int getDisplayWidth() { return 170; }
            @Override
            public int getDisplayHeight() { return 112; }

            @Override
            public void addWidgets(WidgetHolder widgets) {
                JeiHeatingCoilBackgroundDrawable bg = new JeiHeatingCoilBackgroundDrawable(170, 112);
                widgets.addDrawable(0, 0, 170, 112, (g, mx, my, d) -> bg.draw(g, 0, 0));
                Block off = ModBlocks.getHeatingCoilBlock(recipe.coilId(), false);
                if (off != null) {
                    widgets.addSlot(EmiStack.of(off), JeiHeatingCoilBackgroundDrawable.OFF_X, JeiHeatingCoilBackgroundDrawable.OFF_Y);
                }
                int slotIdx = 0;
                ConsumeOption opt = recipe.option();
                var reg = EmiStackHelper.registryOrThrow();
                var level = Minecraft.getInstance().level;
                if (opt.fluid() != null) {
                    List<FluidStack> fluids = HeatingCoilEmiHelper.fluidStacks(opt.fluid(), reg);
                    if (!fluids.isEmpty()) {
                        widgets.addSlot(EmiStackHelper.ingredientOfFluids(fluids), slotX(slotIdx++), JeiHeatingCoilBackgroundDrawable.IN_Y);
                    }
                }
                if (opt.chemical() != null && MekChemicalHelper.isGasSupportEnabled()) {
                    var chem = EmiStackHelper.ingredientOfChemicalSelectors(List.of(opt.chemical().selector()));
                    if (!chem.isEmpty()) {
                        widgets.addSlot(chem, slotX(slotIdx++), JeiHeatingCoilBackgroundDrawable.IN_Y);
                    }
                }
                if (opt.item() != null) {
                    var items = HeatingCoilEmiHelper.itemStacks(opt.item(), reg);
                    if (!items.isEmpty()) {
                        widgets.addSlot(EmiStackHelper.ingredientOf(items), slotX(slotIdx++), JeiHeatingCoilBackgroundDrawable.IN_Y);
                    }
                }
                if (opt.burnable() != null && level != null) {
                    var burn = HeatingCoilEmiHelper.burnables(level);
                    if (!burn.isEmpty()) {
                        widgets.addSlot(EmiStackHelper.ingredientOf(burn), slotX(slotIdx), JeiHeatingCoilBackgroundDrawable.IN_Y);
                    }
                }
                Block on = ModBlocks.getHeatingCoilBlock(recipe.coilId(), true);
                if (on != null) {
                    widgets.addSlot(EmiStack.of(on), JeiHeatingCoilBackgroundDrawable.ON_X, JeiHeatingCoilBackgroundDrawable.ON_Y);
                }
                widgets.addDrawable(0, 0, 170, 112, (g, mx, my, d) -> drawCoilText(g, recipe));
            }

            private static int slotX(int idx) {
                return switch (idx) {
                    case 0 -> JeiHeatingCoilBackgroundDrawable.IN1_X;
                    case 1 -> JeiHeatingCoilBackgroundDrawable.IN2_X;
                    default -> JeiHeatingCoilBackgroundDrawable.IN3_X;
                };
            }

            private static EmiIngredient buildInputs(HeatingCoilJeiRecipe recipe) {
                return EmiStack.EMPTY;
            }

            private static EmiStack buildOutput(HeatingCoilJeiRecipe recipe) {
                Block on = ModBlocks.getHeatingCoilBlock(recipe.coilId(), true);
                return on == null ? EmiStack.EMPTY : EmiStack.of(on);
            }

            private static void drawCoilText(GuiGraphics g, HeatingCoilJeiRecipe recipe) {
                var font = Minecraft.getInstance().font;
                int color = 0xFF404040;
                g.drawString(font, "+", JeiHeatingCoilBackgroundDrawable.PLUS_X, JeiHeatingCoilBackgroundDrawable.PLUS_Y, color, false);
                if (recipe.option().energy() != null) {
                    g.drawString(font, "+ RF", JeiHeatingCoilBackgroundDrawable.RF_X, JeiHeatingCoilBackgroundDrawable.RF_Y, color, false);
                }
                int textY = JeiHeatingCoilBackgroundDrawable.TEXT_Y;
                int margin = JeiHeatingCoilBackgroundDrawable.TEXT_MARGIN;
                int line = 0;
                g.drawString(font, Component.translatable("jei.colossal_reactors.coil.duration", recipe.durationTicks()),
                        margin, textY + line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT, color, false);
                ConsumeOption opt = recipe.option();
                if (opt.fluid() != null) {
                    g.drawString(font, Component.translatable("jei.colossal_reactors.coil.activate", opt.fluid().activation() + " mB"),
                            margin, textY + line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT, color, false);
                    g.drawString(font, Component.translatable("jei.colossal_reactors.coil.substain", opt.fluid().substain() + " mB"),
                            margin, textY + line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT, color, false);
                }
            }
        };
    }

    private static String fmt(double value) {
        if (value == (long) value) return String.valueOf((long) value);
        return String.format("%.2f", value);
    }

    private abstract static class SimpleEmiRecipe implements EmiRecipe {
        private final EmiRecipeCategory category;
        private final Identifier id;
        private final int w;
        private final int h;
        private final boolean twoSlots;
        private EmiIngredient input = EmiStack.EMPTY;
        private EmiIngredient output = EmiStack.EMPTY;

        protected SimpleEmiRecipe(EmiRecipeCategory category, Identifier id, int w, int h, boolean twoSlots) {
            this.category = category;
            this.id = id;
            this.w = w;
            this.h = h;
            this.twoSlots = twoSlots;
        }

        protected void addIn(EmiIngredient ing) { this.input = ing; }
        protected void addOut(EmiIngredient ing) { this.output = ing; }
        protected abstract void addContent(WidgetHolder widgets);
        protected abstract void drawText(GuiGraphics g);

        @Override
        public EmiRecipeCategory getCategory() { return category; }
        @Override
        public Identifier getId() { return id; }
        @Override
        public List<EmiIngredient> getInputs() { return List.of(input); }
        @Override
        public List<EmiStack> getOutputs() {
            return output.isEmpty() ? List.of() : output.getEmiStacks();
        }
        @Override
        public int getDisplayWidth() { return w; }
        @Override
        public int getDisplayHeight() { return h; }

        @Override
        public void addWidgets(WidgetHolder widgets) {
            ColossalEmiRecipeLayout.addStandardBackground(widgets, w, h, twoSlots);
            addContent(widgets);
            if (!input.isEmpty()) {
                widgets.addSlot(input, ColossalEmiRecipeLayout.inSlotX(), ColossalEmiRecipeLayout.inSlotY());
            }
            if (twoSlots && !output.isEmpty()) {
                widgets.addSlot(output, ColossalEmiRecipeLayout.outSlotX(), ColossalEmiRecipeLayout.outSlotY());
            }
            ColossalEmiRecipeLayout.addTextDrawer(widgets, w, h, this::drawText);
        }
    }

    public static void drawCoolant(GuiGraphics g, CoolantJeiRecipe recipe) {
        CoolantJeiRecipe r = recipe;
        var def = r.definition();
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int line2 = textY + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        String[] ratio = JeiIngredientsHelper.formatSimplifiedRatio(def.mbMultiplier(), def.steamPerCoolant());
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.consume_coolant", ratio[1]), margin, textY, color, false);
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.produce_exhaust_coolant", ratio[0]), margin, line2, color, false);
    }

    public static void drawHeatSink(GuiGraphics g, HeatSinkDefinition recipe) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int lh = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        int color = 0xFF404040;
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.heat_sink.fuel_reduction", fmt(recipe.fuelMultiplier())), margin, textY, color, false);
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.heat_sink.rf_increment", fmt(recipe.energyMultiplier())), margin, textY + lh, color, false);
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.heat_sink.heat_reduction", fmt(recipe.overheatingMultiplier())), margin, textY + 2 * lh, color, false);
    }

    public static void drawMelter(GuiGraphics g, MelterRecipe recipe) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int lh = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        int color = 0xFF404040;
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.melter.amount", recipe.amountMb()), margin, textY, color, false);
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.melter.default_time", JeiIngredientsHelper.formatDefaultDuration(recipe.timeTicks())), margin, textY + lh, color, false);
    }

    public static void drawMelterHeat(GuiGraphics g, MelterHeatEntry entry) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        String factorStr = entry.factor() == (long) entry.factor() ? String.valueOf((long) entry.factor()) : String.format("%.2f", entry.factor());
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.melter_heat.factor", factorStr), margin, textY, color, false);
    }

    public static void drawElecCoil(GuiGraphics g, ElecCoilDefinition recipe) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.elec_coil.eff_coe", fmt(recipe.effCoe())), margin, textY, color, false);
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.elec_coil.eff_max", fmt(recipe.effMax())), margin, textY + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT, color, false);
    }

    public static void drawTurbine(GuiGraphics g, TurbineJeiRecipe recipe) {
        var def = recipe.definition();
        var font = net.minecraft.client.Minecraft.getInstance().font;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;
        g.drawString(font, net.minecraft.network.chat.Component.translatable("jei.colossal_reactors.turbine_generation.rf_per_bucket",
                TurbineGenerationLoader.formatRfPerSteamBucket(def.rfProduction())), margin, JeiRecipeBackgroundDrawable.TEXT_Y, color, false);
    }
}
