package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class HeatingCoilRecipeCategory implements IRecipeCategory<HeatingCoilJeiRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "heating_coil");
    private static final int WIDTH = 170;
    private static final int HEIGHT = 112;
    private static final int FLUID_DISPLAY_AMOUNT_MB = 1000;

    public static final RecipeType<HeatingCoilJeiRecipe> RECIPE_TYPE = new RecipeType<>(UID, HeatingCoilJeiRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public HeatingCoilRecipeCategory(IGuiHelper helper) {
        this.background = new JeiHeatingCoilBackgroundDrawable(WIDTH, HEIGHT);
        // Pick the first registered coil block as icon (fallback to MELTER if none).
        ItemStack stack = ModBlocks.HEATING_COIL_BLOCKS.isEmpty()
                ? new ItemStack(ModBlocks.MELTER.get())
                : new ItemStack(ModBlocks.HEATING_COIL_BLOCKS.get(0).get());
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, stack);
    }

    @Override
    public RecipeType<HeatingCoilJeiRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.colossal_reactors.heating_coil");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, HeatingCoilJeiRecipe recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();

        Block offBlock = ModBlocks.getHeatingCoilBlock(recipe.coilId(), false);
        Block onBlock = ModBlocks.getHeatingCoilBlock(recipe.coilId(), true);

        if (offBlock != null) {
            builder.addSlot(RecipeIngredientRole.CATALYST,
                    JeiHeatingCoilBackgroundDrawable.OFF_X + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_X,
                    JeiHeatingCoilBackgroundDrawable.OFF_Y + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_Y
            ).addItemStack(new ItemStack(offBlock));
        }

        // Requirement slots in order: fluid, item, burnable
        int slotIdx = 0;
        ConsumeOption opt = recipe.option();

        if (opt.fluid() != null) {
            ConsumeOption.FluidRequirement fluidReq = opt.fluid();
            List<FluidStack> fluidStacks = resolveFluidRequirementToStacks(fluidReq, registryAccess);
            if (!fluidStacks.isEmpty()) {
                int x = getInputSlotX(slotIdx++);
                builder.addSlot(RecipeIngredientRole.INPUT, x + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_X,
                        JeiHeatingCoilBackgroundDrawable.IN_Y + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_Y)
                        .addIngredients(NeoForgeTypes.FLUID_STACK, fluidStacks);
            }
        }

        if (opt.item() != null) {
            ConsumeOption.ItemRequirement itemReq = opt.item();
            List<ItemStack> items = resolveItemSelector(itemReq, registryAccess);
            if (!items.isEmpty()) {
                int x = getInputSlotX(slotIdx++);
                builder.addSlot(RecipeIngredientRole.INPUT, x + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_X,
                        JeiHeatingCoilBackgroundDrawable.IN_Y + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_Y)
                        .addItemStacks(items);
            }
        }

        if (opt.burnable() != null) {
            List<ItemStack> burnables = List.of(new ItemStack(Items.COAL), new ItemStack(Items.CHARCOAL));
            int x = getInputSlotX(slotIdx++);
            builder.addSlot(RecipeIngredientRole.INPUT, x + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_X,
                    JeiHeatingCoilBackgroundDrawable.IN_Y + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_Y)
                    .addItemStacks(burnables);
        }

        if (onBlock != null) {
            builder.addSlot(RecipeIngredientRole.OUTPUT,
                    JeiHeatingCoilBackgroundDrawable.ON_X + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_X,
                    JeiHeatingCoilBackgroundDrawable.ON_Y + JeiHeatingCoilBackgroundDrawable.ITEM_OFFSET_Y
            ).addItemStack(new ItemStack(onBlock));
        }
    }

    @Override
    public void draw(HeatingCoilJeiRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int color = 0xFF404040;

        // First row markers: '+' always; 'RF' only if energy requirement exists
        guiGraphics.drawString(font, "+", JeiHeatingCoilBackgroundDrawable.PLUS_X, JeiHeatingCoilBackgroundDrawable.PLUS_Y, color, false);
        if (recipe.option().energy() != null) {
            guiGraphics.drawString(font, "RF", JeiHeatingCoilBackgroundDrawable.RF_X, JeiHeatingCoilBackgroundDrawable.RF_Y, color, false);
        }

        int textY = JeiHeatingCoilBackgroundDrawable.TEXT_Y;
        int margin = JeiHeatingCoilBackgroundDrawable.TEXT_MARGIN;
        int line = 0;

        guiGraphics.drawString(font,
                Component.translatable("jei.colossal_reactors.coil.duration", recipe.durationTicks()),
                margin, textY + (line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT), color, false);

        ConsumeOption opt = recipe.option();
        if (opt.fluid() != null) {
            ConsumeOption.FluidRequirement fluidReq = opt.fluid();
            guiGraphics.drawString(font,
                    Component.translatable("jei.colossal_reactors.coil.activate", fluidReq.activation() + " mB"),
                    margin, textY + (line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT), color, false);
            guiGraphics.drawString(font,
                    Component.translatable("jei.colossal_reactors.coil.substain", fluidReq.substain() + " mB"),
                    margin, textY + (line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT), color, false);
        }
        if (opt.item() != null) {
            ConsumeOption.ItemRequirement itemReq = opt.item();
            guiGraphics.drawString(font,
                    Component.translatable("jei.colossal_reactors.coil.activate", itemReq.activation()),
                    margin, textY + (line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT), color, false);
            guiGraphics.drawString(font,
                    Component.translatable("jei.colossal_reactors.coil.substain", itemReq.substain()),
                    margin, textY + (line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT), color, false);
        }
        if (opt.burnable() != null) {
            ConsumeOption.BurnableRequirement burnReq = opt.burnable();
            guiGraphics.drawString(font,
                    Component.translatable("jei.colossal_reactors.coil.activate", burnReq.activation() + " t"),
                    margin, textY + (line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT), color, false);
            guiGraphics.drawString(font,
                    Component.translatable("jei.colossal_reactors.coil.substain", burnReq.substain() + " t"),
                    margin, textY + (line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT), color, false);
        }
        if (opt.energy() != null) {
            ConsumeOption.EnergyRequirement energyReq = opt.energy();
            guiGraphics.drawString(font,
                    Component.translatable("jei.colossal_reactors.coil.activate", energyReq.activation() + " RF"),
                    margin, textY + (line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT), color, false);
            guiGraphics.drawString(font,
                    Component.translatable("jei.colossal_reactors.coil.substain", energyReq.substain() + " RF"),
                    margin, textY + (line++ * JeiHeatingCoilBackgroundDrawable.TEXT_LINE_HEIGHT), color, false);
        }
    }

    private static int getInputSlotX(int slotIdx) {
        return switch (slotIdx) {
            case 0 -> JeiHeatingCoilBackgroundDrawable.IN1_X;
            case 1 -> JeiHeatingCoilBackgroundDrawable.IN2_X;
            default -> JeiHeatingCoilBackgroundDrawable.IN3_X;
        };
    }

    private static List<ItemStack> resolveItemSelector(ConsumeOption.ItemRequirement req, net.minecraft.core.RegistryAccess registryAccess) {
        int count = Math.max(1, req.activation());
        List<ItemStack> out = new ArrayList<>();
        if (req.isTag()) {
            TagKey<Item> tagKey = TagKey.create(net.minecraft.core.registries.Registries.ITEM, req.tagOrId());
            registryAccess.lookup(net.minecraft.core.registries.Registries.ITEM).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> out.add(new ItemStack(h.value(), count)))));
        } else {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(req.tagOrId());
            if (item != null && item != Items.AIR) out.add(new ItemStack(item, count));
        }
        return out;
    }

    private static List<FluidStack> resolveFluidRequirementToStacks(ConsumeOption.FluidRequirement req, net.minecraft.core.RegistryAccess registryAccess) {
        List<FluidStack> out = new ArrayList<>();
        if (req.isTag()) {
            TagKey<Fluid> tagKey = TagKey.create(net.minecraft.core.registries.Registries.FLUID, req.tagOrId());
            registryAccess.lookup(net.minecraft.core.registries.Registries.FLUID).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> out.add(new FluidStack(h.value(), FLUID_DISPLAY_AMOUNT_MB)))));
        } else {
            Fluid fluid = net.minecraft.core.registries.BuiltInRegistries.FLUID.get(req.tagOrId());
            if (fluid != null && fluid != Fluids.EMPTY) {
                out.add(new FluidStack(fluid, FLUID_DISPLAY_AMOUNT_MB));
            }
        }
        return out;
    }
}

