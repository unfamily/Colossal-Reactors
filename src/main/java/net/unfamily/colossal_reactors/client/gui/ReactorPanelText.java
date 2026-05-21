package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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

    public static int drawStatusLine(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            Component statusValue,
            @Nullable Component trailing) {
        graphics.drawString(font, STATUS_LABEL, x, y, GuiTextColors.PANEL_YELLOW_LIGHT, false);
        int xAfter = x + font.width(STATUS_LABEL);
        graphics.drawString(font, statusValue, xAfter, y, GuiTextColors.PANEL_WHITE, false);
        xAfter += font.width(statusValue);
        if (trailing != null) {
            graphics.drawString(font, trailing, xAfter, y, GuiTextColors.PANEL_WHITE, false);
            xAfter += font.width(trailing);
        }
        return xAfter;
    }

    /** Metric row: yellow {@code label}, white {@code value} (e.g. "Blades:" + " 12 valid"). */
    public static int drawMetricLine(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            Component label,
            Component value) {
        graphics.drawString(font, label, x, y, GuiTextColors.PANEL_YELLOW_LIGHT, false);
        int xAfter = x + font.width(label);
        graphics.drawString(font, value, xAfter, y, GuiTextColors.PANEL_WHITE, false);
        return xAfter + font.width(value);
    }

    public static int drawMetricRow(
            GuiGraphics graphics,
            Font font,
            int x,
            int y,
            int lineHeight,
            String labelKey,
            Component value) {
        drawMetricLine(graphics, font, x, y, Component.translatable(labelKey), value);
        return y + lineHeight;
    }
}
