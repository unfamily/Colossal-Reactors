package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.neoforge.NeoForgeTypes;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.types.IRecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MelterRecipeCategory implements IRecipeCategory<MelterRecipe> {

    public static final Identifier UID = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "melter");
    private static final int WIDTH = 180;
    private static final int HEIGHT = 64;

    public static final IRecipeType<MelterRecipe> RECIPE_TYPE = IRecipeType.create(UID, MelterRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    public MelterRecipeCategory(IGuiHelper helper) {
        this.background = new JeiRecipeBackgroundDrawable(WIDTH, HEIGHT, true);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MELTER.get()));
    }

    @Override
    public IRecipeType<MelterRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.colossal_reactors.melter");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MelterRecipe recipe, IFocusGroup focuses) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var registryAccess = level.registryAccess();

        List<ItemStack> inputs = resolveItemSelector(recipe.inputId(), recipe.inputIsTag(), recipe.count(), registryAccess);
        if (!inputs.isEmpty()) {
            builder.addSlot(RecipeIngredientRole.INPUT,
                    JeiRecipeBackgroundDrawable.SLOT_IN_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                    JeiRecipeBackgroundDrawable.SLOT_IN_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y
            ).addItemStacks(inputs);
        }

        var fluid = MelterRecipesLoader.getOutputFluid(recipe, registryAccess);
        if (fluid != null && fluid != Fluids.EMPTY) {
            FluidStack out = new FluidStack(fluid, 1000);
            builder.addSlot(RecipeIngredientRole.OUTPUT,
                    JeiRecipeBackgroundDrawable.SLOT_OUT_X + JeiRecipeBackgroundDrawable.ITEM_OFFSET_X,
                    JeiRecipeBackgroundDrawable.SLOT_OUT_Y + JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y
            ).addIngredient(NeoForgeTypes.FLUID_STACK, out);
        }
    }

    @Override
    public void draw(MelterRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphicsExtractor guiGraphics, double mouseX, double mouseY) {
        background.draw(guiGraphics);
        var font = Minecraft.getInstance().font;
        int textY = JeiRecipeBackgroundDrawable.TEXT_Y;
        int margin = JeiRecipeBackgroundDrawable.TEXT_MARGIN;
        int lineHeight = JeiRecipeBackgroundDrawable.TEXT_LINE_HEIGHT;
        int color = 0xFF404040;

        guiGraphics.text(font,
                Component.translatable("jei.colossal_reactors.melter.amount", recipe.amountMb()),
                margin, textY, color, false);
        guiGraphics.text(font, Component.translatable("jei.colossal_reactors.melter.heat_required_1"), margin, textY + lineHeight, color, false);
        guiGraphics.text(font, Component.translatable("jei.colossal_reactors.melter.heat_required_2"), margin, textY + lineHeight * 2, color, false);
        guiGraphics.text(font, Component.translatable("jei.colossal_reactors.melter.heat_required_3"), margin, textY + lineHeight * 3, color, false);
    }

    private static List<ItemStack> resolveItemSelector(Identifier id, boolean isTag, int count, net.minecraft.core.RegistryAccess registryAccess) {
        if (id == null) return List.of();
        int safeCount = Math.max(1, count);
        List<ItemStack> out = new ArrayList<>();

        if (isTag) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, id);
            registryAccess.lookup(Registries.ITEM).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> out.add(new ItemStack(h.value(), safeCount)))));
        } else {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id).map(h -> h.value()).orElse(net.minecraft.world.item.Items.AIR);
            if (item != net.minecraft.world.item.Items.AIR) {
                out.add(new ItemStack(item, safeCount));
            }
        }
        return out;
    }
}
