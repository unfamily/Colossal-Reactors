package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;

/**
 * Background for Heating Coil JEI recipes.
 * Draws fixed OFF slot, up to 3 input slots, arrow, and ON slot.
 */
public class JeiHeatingCoilBackgroundDrawable implements IDrawable {

    public static final int SLOT_SIZE = JeiRecipeBackgroundDrawable.SLOT_SIZE;
    public static final int ITEM_OFFSET_X = JeiRecipeBackgroundDrawable.ITEM_OFFSET_X;
    public static final int ITEM_OFFSET_Y = JeiRecipeBackgroundDrawable.ITEM_OFFSET_Y;

    public static final int OFF_X = 0;
    public static final int OFF_Y = 0;

    /** Centered in the gap between OFF (0..18) and IN1 (32). */
    public static final int PLUS_X = 23;
    public static final int PLUS_Y = 5;

    public static final int IN1_X = 32;
    public static final int IN2_X = 52;
    public static final int IN3_X = 72;
    public static final int IN_Y = 0;

    /** Just after IN3 (72+18), before the arrow at 118 — avoids overlapping the third slot. */
    public static final int RF_X = 92;
    public static final int RF_Y = 5;

    public static final int ARROW_X = 118;
    public static final int ARROW_Y = 1;

    public static final int ON_X = 146;
    public static final int ON_Y = 0;

    public static final int TEXT_Y = 22;
    public static final int TEXT_LINE_HEIGHT = 10;
    public static final int TEXT_MARGIN = 6;

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
    public void draw(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, JeiRecipeBackgroundDrawable.SLOT_TEXTURE,
                xOffset + OFF_X, yOffset + OFF_Y, 0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, JeiRecipeBackgroundDrawable.SLOT_TEXTURE,
                xOffset + IN1_X, yOffset + IN_Y, 0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, JeiRecipeBackgroundDrawable.SLOT_TEXTURE,
                xOffset + IN2_X, yOffset + IN_Y, 0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, JeiRecipeBackgroundDrawable.SLOT_TEXTURE,
                xOffset + IN3_X, yOffset + IN_Y, 0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, JeiRecipeBackgroundDrawable.ARROW_TEXTURE,
                xOffset + ARROW_X, yOffset + ARROW_Y, 0.0F, 0.0F,
                JeiRecipeBackgroundDrawable.ARROW_W, JeiRecipeBackgroundDrawable.ARROW_H,
                JeiRecipeBackgroundDrawable.ARROW_W, JeiRecipeBackgroundDrawable.ARROW_H);

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, JeiRecipeBackgroundDrawable.SLOT_TEXTURE,
                xOffset + ON_X, yOffset + ON_Y, 0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }
}
