package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Shared panel label drawing: status prefix in light yellow, value in white.
 */
public final class ReactorPanelText {

    private static final Component STATUS_LABEL =
            Component.translatable("gui.colossal_reactors.reactor_controller.status.label");

    private ReactorPanelText() {
    }

    /** Draws {@code Status: } + value (+ optional suffix) on one line; returns x after last glyph. */
    public static int drawStatusLine(
            GuiGraphicsExtractor graphics,
            Font font,
            int x,
            int y,
            Component statusValue,
            @Nullable Component trailing) {
        graphics.text(font, STATUS_LABEL, x, y, GuiTextColors.PANEL_YELLOW_LIGHT, false);
        int xAfter = x + font.width(STATUS_LABEL);
        graphics.text(font, statusValue, xAfter, y, GuiTextColors.PANEL_WHITE, false);
        xAfter += font.width(statusValue);
        if (trailing != null) {
            graphics.text(font, trailing, xAfter, y, GuiTextColors.PANEL_WHITE, false);
            xAfter += font.width(trailing);
        }
        return xAfter;
    }
}
