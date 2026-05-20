package net.unfamily.colossal_reactors.client.gui;

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

    private ReactorControllerGui() {
    }
}
