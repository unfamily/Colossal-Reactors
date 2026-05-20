package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

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
    private static final int TEXT_COLOR = 0xFFFFFF;
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
            if (minecraft != null && minecraft.getSoundManager() != null)
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (minecraft != null) minecraft.setScreen(parent);
        })
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
        panelScrollbar.createButtons(leftPos, topPos, this::addRenderableWidget, () -> {});
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (minecraft != null && (keyCode == 256 || keyCode == 1)) {
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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, DARKEN_COLOR);
        int leftPos = (width - GUI_WIDTH) / 2;
        int topPos = (height - GUI_HEIGHT) / 2;

        guiGraphics.blit(ReactorControllerGui.BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        int titleW = font.width(title);
        int titleX = leftPos + (GUI_WIDTH - titleW) / 2;
        guiGraphics.drawString(font, title, titleX, topPos + 5, 0x404040, false);

        guiGraphics.enableScissor(leftPos + CONTENT_LEFT, topPos + PANEL_Y, leftPos + GuiPanelScrollbar.TEXT_RIGHT, topPos + GuiPanelScrollbar.TEXT_BOTTOM);
        int contentHeight = drawPanelContent(guiGraphics, leftPos, topPos, panelScrollbar.getScrollOffset());
        guiGraphics.disableScissor();
        panelScrollbar.setContentHeight(contentHeight);

        for (var child : children()) {
            if (child instanceof Renderable r) {
                r.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
        panelScrollbar.render(guiGraphics, leftPos, topPos);
    }

    private int drawPanelContent(GuiGraphics guiGraphics, int leftPos, int topPos, int scrollOffset) {
        int y = topPos + PANEL_Y - scrollOffset;
        Component statusKey = Component.translatable("gui.colossal_reactors.reactor_builder.simulation");
        ReactorPanelText.drawStatusLine(guiGraphics, font, leftPos + PANEL_X, y, statusKey, null);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", 0, 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_blocks", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.energy_production", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.water_consume", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.steam_production", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_units", "0"),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        return y - (topPos + PANEL_Y - scrollOffset);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int leftPos = (width - GUI_WIDTH) / 2;
            int topPos = (height - GUI_HEIGHT) / 2;
            if (panelScrollbar.mouseClicked(mouseX, mouseY, leftPos, topPos)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            panelScrollbar.mouseReleased();
        }
        return super.mouseReleased(mouseX, mouseY, button);
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
