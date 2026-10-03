package net.unfamily.colossal_reactors.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Shared heating-coil recipe layout (OFF + 3 inputs + arrow + ON). No recipe-viewer API dependency.
 */
public final class RecipeViewerHeatingCoilLayout {
    public static final int SLOT_SIZE = RecipeViewerLayout.SLOT_SIZE;
    public static final int ITEM_OFFSET_X = RecipeViewerLayout.ITEM_OFFSET_X;
    public static final int ITEM_OFFSET_Y = RecipeViewerLayout.ITEM_OFFSET_Y;

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

    public static final int WIDTH = 170;
    public static final int HEIGHT = 112;

    private RecipeViewerHeatingCoilLayout() {}

    public static void draw(GuiGraphics guiGraphics, int xOffset, int yOffset) {
        RenderSystem.enableBlend();
        blitSlot(guiGraphics, xOffset + OFF_X, yOffset + OFF_Y);
        blitSlot(guiGraphics, xOffset + IN1_X, yOffset + IN_Y);
        blitSlot(guiGraphics, xOffset + IN2_X, yOffset + IN_Y);
        blitSlot(guiGraphics, xOffset + IN3_X, yOffset + IN_Y);
        guiGraphics.blit(
                RecipeViewerLayout.ARROW_TEXTURE,
                xOffset + ARROW_X,
                yOffset + ARROW_Y,
                0,
                0,
                RecipeViewerLayout.ARROW_W,
                RecipeViewerLayout.ARROW_H,
                RecipeViewerLayout.ARROW_W,
                RecipeViewerLayout.ARROW_H);
        blitSlot(guiGraphics, xOffset + ON_X, yOffset + ON_Y);
        RenderSystem.disableBlend();
    }

    public static int inputSlotX(int index) {
        return switch (index) {
            case 0 -> IN1_X;
            case 1 -> IN2_X;
            default -> IN3_X;
        };
    }

    private static void blitSlot(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(
                RecipeViewerLayout.SLOT_TEXTURE, x, y, 0, 0, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE, SLOT_SIZE);
    }
}
