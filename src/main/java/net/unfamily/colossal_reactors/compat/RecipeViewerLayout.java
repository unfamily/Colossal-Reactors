package net.unfamily.colossal_reactors.compat;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * Shared JEI/EMI/REI slot + arrow layout. No recipe-viewer API dependency.
 */
public final class RecipeViewerLayout {
    public static final ResourceLocation SLOT_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/jei/slot.png");
    public static final ResourceLocation ARROW_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/jei/arrow.png");

    public static final int SLOT_SIZE = 18;
    public static final int SLOT_IN_X = 0;
    public static final int SLOT_IN_Y = 0;
    public static final int ARROW_X = 22;
    public static final int ARROW_Y = 1;
    public static final int ARROW_W = 22;
    public static final int ARROW_H = 15;
    public static final int SLOT_OUT_X = 46;
    public static final int SLOT_OUT_Y = 0;
    public static final int AUX_SLOT_X = 22;
    public static final int ITEM_OFFSET_X = 1;
    public static final int ITEM_OFFSET_Y = 1;
    public static final int TEXT_Y = 20;
    public static final int TEXT_LINE_HEIGHT = 10;
    public static final int TEXT_MARGIN = 6;

    private RecipeViewerLayout() {}

    public static void draw(GuiGraphics guiGraphics, int xOffset, int yOffset, boolean twoSlotsWithArrow) {
        draw(guiGraphics, xOffset, yOffset, true, twoSlotsWithArrow);
    }

    /**
     * Draws only the slot frames that have content. EMI rejects empty recipe slots; prefer omitting
     * unused frames rather than leaving blank slot widgets.
     */
    public static void draw(
            GuiGraphics guiGraphics, int xOffset, int yOffset, boolean drawInputSlot, boolean drawOutputSlot) {
        RenderSystem.enableBlend();
        if (drawInputSlot) {
            guiGraphics.blit(
                    SLOT_TEXTURE,
                    xOffset + SLOT_IN_X,
                    yOffset + SLOT_IN_Y,
                    0,
                    0,
                    SLOT_SIZE,
                    SLOT_SIZE,
                    SLOT_SIZE,
                    SLOT_SIZE);
        }
        if (drawInputSlot && drawOutputSlot) {
            guiGraphics.blit(
                    ARROW_TEXTURE, xOffset + ARROW_X, yOffset + ARROW_Y, 0, 0, ARROW_W, ARROW_H, ARROW_W, ARROW_H);
        }
        if (drawOutputSlot) {
            guiGraphics.blit(
                    SLOT_TEXTURE,
                    xOffset + SLOT_OUT_X,
                    yOffset + SLOT_OUT_Y,
                    0,
                    0,
                    SLOT_SIZE,
                    SLOT_SIZE,
                    SLOT_SIZE,
                    SLOT_SIZE);
        }
        RenderSystem.disableBlend();
    }
}
