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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CoolantRecipeCategory implements IRecipeCategory<CoolantDefinition> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_coolant");
    private static final int WIDTH = 180;
    private static final int HEIGHT = 52;

    public static final RecipeType<CoolantDefinition> RECIPE_TYPE = new RecipeType<>(UID, CoolantDefinition.class);

    private final IDrawable background;
    private final IDrawable icon;

    public CoolantRecipeCategory(IGuiHelper helper) {
        this.background = new JeiRecipeBackgroundDrawable(WIDTH, HEIGHT, true);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.RESOURCE_PORT.get()));
    }

    @Override
    public RecipeType<CoolantDefinition> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.colossal_reactors.reactor_coolant");
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
    public void setRecipe(IRecipeLayoutBuilder builder, CoolantDefinition recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();

        List<FluidStack> inputFluids = JeiIngredientsHelper.getCoolantInputFluidStacks(recipe.inputs(), registryAccess);
        List<FluidStack> outputFluids = JeiIngredientsHelper.getOutputFluidStacks(recipe.output(), registryAccess);
        if (!inputFluids.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT,
                    JeiRecipeBackgroundDrawable.SLOT_IN_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                    JeiRecipeBackgroundDrawable.SLOT_IN_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, inputFluids);
        }
        if (!outputFluids.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.OUTPUT,
                    JeiRecipeBackgroundDrawable.SLOT_OUT_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                    JeiRecipeBackgroundDrawable.SLOT_OUT_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y)
                    .addIngredients(NeoForgeTypes.FLUID_STACK, outputFluids);
        }
    }

    @Override
    public void draw(CoolantDefinition recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int line2 = textY + JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int color = 0xFF404040;

        String[] ratio = JeiIngredientsHelper.formatSimplifiedRatio(recipe.mbMultiplier(), recipe.steamPerCoolant());
        Component consumeCoolant = Component.translatable("jei.colossal_reactors.consume_coolant", ratio[1]);
        Component produceExhaust = Component.translatable("jei.colossal_reactors.produce_exhaust_coolant", ratio[0]);
        guiGraphics.drawString(font, consumeCoolant, margin, textY, color, false);
        guiGraphics.drawString(font, produceExhaust, margin, line2, color, false);
    }
}
