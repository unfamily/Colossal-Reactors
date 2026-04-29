package net.unfamily.colossal_reactors.client.gui;

/**
 * Opaque ARGB colors for {@link net.minecraft.client.gui.GuiGraphics#text} on Minecraft 26+.
 * 24-bit values like {@code 0x404040} are interpreted as ARGB with alpha 0 and render invisible.
 */
public final class GuiTextColors {
    public static final int TITLE = 0xFF404040;
    public static final int BODY = 0xFF404040;
    /** Primary label text on dark GUI panels (legacy {@code 0xFFFFFF}). */
    public static final int PANEL_WHITE = 0xFFFFFFFF;
    public static final int ERROR = 0xFFFF0000;

    private GuiTextColors() {}
}
