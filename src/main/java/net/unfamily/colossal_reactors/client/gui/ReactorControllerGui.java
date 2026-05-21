package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * Layout constants for {@code reactor_controller.png} and scroll thumb {@code scroller.png}.
 * Pixel sizes must match the PNG assets on disk.
 */
public final class ReactorControllerGui {

    public static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/reactor_controller.png");

    /** {@code reactor_controller.png} width in pixels. */
    public static final int WIDTH = 230;
    /** {@code reactor_controller.png} height in pixels. */
    public static final int HEIGHT = 240;

    /** {@code scroller.png} thumb width in pixels. */
    public static final int SCROLLER_WIDTH = 12;
    /** {@code scroller.png} thumb height in pixels. */
    public static final int SCROLLER_HEIGHT = 15;

    /** Header close button top edge (local GUI Y). */
    public static final int HEADER_BUTTON_Y = 5;
    /** Header close button width and height. */
    public static final int HEADER_BUTTON_SIZE = 12;
    /** Inset from the right edge of the GUI to the close button. */
    public static final int HEADER_RIGHT_INSET = 5;

    public static int closeButtonX(int guiWidth) {
        return guiWidth - HEADER_BUTTON_SIZE - HEADER_RIGHT_INSET;
    }

    /** Title label Y in {@code renderLabels} local coordinates, vertically centered with the close button. */
    public static int titleLabelY(Font font) {
        return HEADER_BUTTON_Y + (HEADER_BUTTON_SIZE - font.lineHeight) / 2;
    }

    private ReactorControllerGui() {
    }
}
