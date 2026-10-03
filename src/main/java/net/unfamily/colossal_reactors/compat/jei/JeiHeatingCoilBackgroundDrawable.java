package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphics;
import net.unfamily.colossal_reactors.compat.RecipeViewerHeatingCoilLayout;

/**
 * JEI {@link IDrawable} wrapper around {@link RecipeViewerHeatingCoilLayout}.
 */
public class JeiHeatingCoilBackgroundDrawable implements IDrawable {

    public static final int SLOT_SIZE = RecipeViewerHeatingCoilLayout.SLOT_SIZE;
    public static final int ITEM_OFFSET_X = RecipeViewerHeatingCoilLayout.ITEM_OFFSET_X;
    public static final int ITEM_OFFSET_Y = RecipeViewerHeatingCoilLayout.ITEM_OFFSET_Y;
    public static final int OFF_X = RecipeViewerHeatingCoilLayout.OFF_X;
    public static final int OFF_Y = RecipeViewerHeatingCoilLayout.OFF_Y;
    public static final int PLUS_X = RecipeViewerHeatingCoilLayout.PLUS_X;
    public static final int PLUS_Y = RecipeViewerHeatingCoilLayout.PLUS_Y;
    public static final int IN1_X = RecipeViewerHeatingCoilLayout.IN1_X;
    public static final int IN2_X = RecipeViewerHeatingCoilLayout.IN2_X;
    public static final int IN3_X = RecipeViewerHeatingCoilLayout.IN3_X;
    public static final int IN_Y = RecipeViewerHeatingCoilLayout.IN_Y;
    public static final int RF_X = RecipeViewerHeatingCoilLayout.RF_X;
    public static final int RF_Y = RecipeViewerHeatingCoilLayout.RF_Y;
    public static final int ARROW_X = RecipeViewerHeatingCoilLayout.ARROW_X;
    public static final int ARROW_Y = RecipeViewerHeatingCoilLayout.ARROW_Y;
    public static final int ON_X = RecipeViewerHeatingCoilLayout.ON_X;
    public static final int ON_Y = RecipeViewerHeatingCoilLayout.ON_Y;
    public static final int TEXT_Y = RecipeViewerHeatingCoilLayout.TEXT_Y;
    public static final int TEXT_LINE_HEIGHT = RecipeViewerHeatingCoilLayout.TEXT_LINE_HEIGHT;
    public static final int TEXT_MARGIN = RecipeViewerHeatingCoilLayout.TEXT_MARGIN;

    private final int width;
    private final int height;

    public JeiHeatingCoilBackgroundDrawable(int width, int height) {
        this.width = width;
        this.height = height;
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
        RecipeViewerHeatingCoilLayout.draw(guiGraphics, xOffset, yOffset);
    }
}
