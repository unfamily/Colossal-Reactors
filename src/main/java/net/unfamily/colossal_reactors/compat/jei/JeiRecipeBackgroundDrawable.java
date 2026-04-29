package net.unfamily.colossal_reactors.compat.jei;

import mezz.jei.api.gui.drawable.IDrawable;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * Draws slot and arrow textures for JEI recipe layout.
 * Slot drawn at full 18x18 so the texture border (top/bottom/sides) is visible.
 */
public class JeiRecipeBackgroundDrawable implements IDrawable {

    public static final Identifier SLOT_TEXTURE = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/jei/slot.png");
    public static final Identifier ARROW_TEXTURE = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/jei/arrow.png");

    /** Full slot size 18x18 (slot.png size) so border is not cropped */
    public static final int SLOT_SIZE = 18;
    public static final int SLOT_IN_X = 0;
    public static final int SLOT_IN_Y = 0;
    /** Arrow at actual texture size (arrow.png is 22x15) */
    public static final int ARROW_X = 22;
    public static final int ARROW_Y = 1;
    public static final int ARROW_W = 22;
    public static final int ARROW_H = 15;
    public static final int SLOT_OUT_X = 46;
    public static final int SLOT_OUT_Y = 0;

    /** Offset for item inside slot: +1 right, +1 down (so item is drawn inset from slot border) */
    public static final int ITEM_OFFSET_X = 1;
    public static final int ITEM_OFFSET_Y = 1;

    /** Y offset for first line of text below slots (below 18px slot) */
    public static final int TEXT_Y = 20;
    /** Vertical step between text lines */
    public static final int TEXT_LINE_HEIGHT = 10;
    /** Margin from left edge for text */
    public static final int TEXT_MARGIN = 6;

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
    public void draw(GuiGraphicsExtractor guiGraphics, int xOffset, int yOffset) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, xOffset + SLOT_IN_X, yOffset + SLOT_IN_Y,
                0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        if (twoSlotsWithArrow) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ARROW_TEXTURE, xOffset + ARROW_X, yOffset + ARROW_Y,
                    0.0F, 0.0F, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SLOT_TEXTURE, xOffset + SLOT_OUT_X, yOffset + SLOT_OUT_Y,
                    0.0F, 0.0F, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
        }
    }
}
