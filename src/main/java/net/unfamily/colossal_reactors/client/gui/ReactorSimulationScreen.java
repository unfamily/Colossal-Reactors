package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;

/**
 * Simulation GUI opened from Reactor Builder. Same layout as reactor controller (dark panel, same labels)
 * but without Reboot button. ESC or close returns to the builder screen.
 */
public class ReactorSimulationScreen extends Screen {

    private static final int GUI_WIDTH = ReactorControllerGui.WIDTH;
    private static final int GUI_HEIGHT = ReactorControllerGui.HEIGHT;

    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private static final int PANEL_X = 16;
    private static final int PANEL_Y = GuiPanelScrollbar.TEXT_TOP;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = GuiTextColors.PANEL_WHITE;
    private static final int CONTENT_LEFT = PANEL_X;

    private static final int DARKEN_COLOR = 0xF0101010;

    private final Screen parent;
    private final GuiPanelScrollbar panelScrollbar = new GuiPanelScrollbar();

    public ReactorSimulationScreen(Screen parent) {
        super(Component.translatable("gui.colossal_reactors.reactor_builder.simulation"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        int leftPos = (width - GUI_WIDTH) / 2;
        int topPos = (height - GUI_HEIGHT) / 2;
        Button closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null) minecraft.setScreen(parent);
        })
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
        panelScrollbar.createButtons(leftPos, topPos, this::addRenderableWidget, () -> {});
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;
        if (minecraft != null && event.isEscape()) {
            minecraft.setScreen(parent);
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        panelScrollbar.disposeButtons(this::removeWidget);
        if (minecraft != null) minecraft.setScreen(parent);
        else super.onClose();
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, DARKEN_COLOR);
        int leftPos = (width - GUI_WIDTH) / 2;
        int topPos = (height - GUI_HEIGHT) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ReactorControllerGui.BACKGROUND, leftPos, topPos, 0.0F, 0.0F, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        int titleW = font.width(title);
        int titleX = leftPos + (GUI_WIDTH - titleW) / 2;
        guiGraphics.text(font, title, titleX, topPos + 5, GuiTextColors.TITLE, false);

        // Screen-space coords (no pose translate in extractBackground).
        guiGraphics.enableScissor(leftPos + CONTENT_LEFT, topPos + PANEL_Y,
                leftPos + GuiPanelScrollbar.TEXT_RIGHT, topPos + GuiPanelScrollbar.TEXT_BOTTOM);
        int contentHeight = drawPanelContent(guiGraphics, leftPos, topPos, panelScrollbar.getScrollOffset());
        guiGraphics.disableScissor();
        panelScrollbar.setContentHeight(contentHeight);
        panelScrollbar.render(guiGraphics, leftPos, topPos);
    }

    private int drawPanelContent(GuiGraphicsExtractor guiGraphics, int leftPos, int topPos, int scrollOffset) {
        int y = topPos + PANEL_Y - scrollOffset;
        int contentStart = y;
        Component statusKey = Component.translatable("gui.colossal_reactors.reactor_builder.simulation");
        ReactorPanelText.drawStatusLine(guiGraphics, font, leftPos + PANEL_X, y, statusKey, null);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", 0, 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_blocks", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.energy_production", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.water_consume", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.steam_production", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_units", "0"),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        return y - contentStart;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            int leftPos = (width - GUI_WIDTH) / 2;
            int topPos = (height - GUI_HEIGHT) / 2;
            if (panelScrollbar.mouseClicked(event.x(), event.y(), leftPos, topPos)) {
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            panelScrollbar.mouseReleased();
        }
        return super.mouseReleased(event);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        panelScrollbar.mouseMoved(mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int leftPos = (width - GUI_WIDTH) / 2;
        int topPos = (height - GUI_HEIGHT) / 2;
        if (panelScrollbar.isInPanelArea(mouseX, mouseY, leftPos, topPos, CONTENT_LEFT)
                && panelScrollbar.mouseScrolled(scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
