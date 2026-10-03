package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.compat.RecipeViewerLayout;

/**
 * JEI {@link IDrawable} wrapper around {@link RecipeViewerLayout}.
 */
public class JeiRecipeBackgroundDrawable implements IDrawable {

    public static final ResourceLocation SLOT_TEXTURE = RecipeViewerLayout.SLOT_TEXTURE;
    public static final ResourceLocation ARROW_TEXTURE = RecipeViewerLayout.ARROW_TEXTURE;
    public static final int SLOT_SIZE = RecipeViewerLayout.SLOT_SIZE;
    public static final int SLOT_IN_X = RecipeViewerLayout.SLOT_IN_X;
    public static final int SLOT_IN_Y = RecipeViewerLayout.SLOT_IN_Y;
    public static final int ARROW_X = RecipeViewerLayout.ARROW_X;
    public static final int ARROW_Y = RecipeViewerLayout.ARROW_Y;
    public static final int ARROW_W = RecipeViewerLayout.ARROW_W;
    public static final int ARROW_H = RecipeViewerLayout.ARROW_H;
    public static final int SLOT_OUT_X = RecipeViewerLayout.SLOT_OUT_X;
    public static final int SLOT_OUT_Y = RecipeViewerLayout.SLOT_OUT_Y;
    public static final int AUX_SLOT_X = RecipeViewerLayout.AUX_SLOT_X;
    public static final int ITEM_OFFSET_X = RecipeViewerLayout.ITEM_OFFSET_X;
    public static final int ITEM_OFFSET_Y = RecipeViewerLayout.ITEM_OFFSET_Y;
    public static final int TEXT_Y = RecipeViewerLayout.TEXT_Y;
    public static final int TEXT_LINE_HEIGHT = RecipeViewerLayout.TEXT_LINE_HEIGHT;
    public static final int TEXT_MARGIN = RecipeViewerLayout.TEXT_MARGIN;

    private final int width;
    private final int height;
    private final boolean twoSlotsWithArrow;

    public JeiRecipeBackgroundDrawable(int width, int height, boolean twoSlotsWithArrow) {
        this.width = width;
        this.height = height;
        this.twoSlotsWithArrow = twoSlotsWithArrow;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
        RecipeViewerLayout.draw(guiGraphics, xOffset, yOffset, twoSlotsWithArrow);
    }
}
