package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.colossal_reactors.menu.TurbineControllerMenu;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;

/**
 * Turbine controller GUI. Same layout as builder simulation / reactor controller ({@code reactor_controller.png}).
 */
public class TurbineControllerScreen extends AbstractContainerScreen<TurbineControllerMenu> {

    private static final int GUI_WIDTH = ReactorControllerGui.WIDTH;
    private static final int GUI_HEIGHT = ReactorControllerGui.HEIGHT;

    private static final int PANEL_X = 16;
    private static final int PANEL_Y = GuiPanelScrollbar.TEXT_TOP;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = GuiTextColors.PANEL_WHITE;
    private static final int PANEL_TEXT_WIDTH = GuiPanelScrollbar.TEXT_RIGHT - PANEL_X;

    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private Button closeButton;
    private final GuiPanelScrollbar panelScrollbar = new GuiPanelScrollbar();

    public TurbineControllerScreen(TurbineControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        })
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
        panelScrollbar.createButtons(leftPos, topPos, this::addRenderableWidget, () -> {});
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ReactorControllerGui.BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        panelScrollbar.render(guiGraphics, leftPos, topPos);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        int titleX = (imageWidth - titleW) / 2;
        guiGraphics.text(font, title, titleX, 5, GuiTextColors.TITLE, false);

        guiGraphics.enableScissor(PANEL_X, PANEL_Y, GuiPanelScrollbar.TEXT_RIGHT, GuiPanelScrollbar.TEXT_BOTTOM);
        int contentHeight = drawPanelContent(guiGraphics, panelScrollbar.getScrollOffset());
        guiGraphics.disableScissor();
        panelScrollbar.setContentHeight(contentHeight);
    }

    private int drawPanelContent(GuiGraphicsExtractor guiGraphics, int scrollOffset) {
        int y = PANEL_Y - scrollOffset;
        int contentStart = y;

        if (menu.isValid()) {
            ReactorPanelText.drawStatusLine(guiGraphics, font, PANEL_X, y,
                    Component.translatable("gui.colossal_reactors.turbine_controller.valid"), null);
            y += LINE_HEIGHT;

            guiGraphics.text(font,
                    Component.translatable("gui.colossal_reactors.turbine.stats.blades", menu.getBladeCount()),
                    PANEL_X, y, TEXT_COLOR, false);
            y += LINE_HEIGHT;

            guiGraphics.text(font,
                    Component.translatable("gui.colossal_reactors.turbine_controller.energy_production",
                            GuiNumberFormat.format(menu.getRfPerTick())),
                    PANEL_X, y, TEXT_COLOR, false);
            y += LINE_HEIGHT;

            guiGraphics.text(font,
                    Component.translatable("gui.colossal_reactors.turbine_controller.steam_production",
                            GuiNumberFormat.format(menu.getSteamPerTick())),
                    PANEL_X, y, TEXT_COLOR, false);
            y += LINE_HEIGHT;

            guiGraphics.text(font,
                    Component.translatable("jei.colossal_reactors.elec_coil.eff_coe",
                            String.format("%.2f", menu.getCoilEff())),
                    PANEL_X, y, TEXT_COLOR, false);
            y += LINE_HEIGHT;

            guiGraphics.text(font,
                    Component.translatable("jei.colossal_reactors.elec_coil.eff_max",
                            String.format("%.2f", menu.getBladeEff())),
                    PANEL_X, y, TEXT_COLOR, false);
            y += LINE_HEIGHT;
        } else {
            ReactorPanelText.drawStatusLine(guiGraphics, font, PANEL_X, y,
                    Component.translatable("gui.colossal_reactors.turbine_controller.invalid"), null);
            y += LINE_HEIGHT;

            Component failure = TurbineValidation.failureMessage(menu.getFailureOrdinal());
            for (var line : font.split(failure, PANEL_TEXT_WIDTH)) {
                guiGraphics.text(font, line, PANEL_X, y, 0xFFFF5555, false);
                y += LINE_HEIGHT;
            }
        }

        return y - contentStart;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean isHolding) {
        if (event.button() == 0 && panelScrollbar.mouseClicked(event.x(), event.y(), leftPos, topPos)) {
            return true;
        }
        return super.mouseClicked(event, isHolding);
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
    public void onClose() {
        panelScrollbar.disposeButtons(this::removeWidget);
        super.onClose();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (panelScrollbar.isInPanelArea(mouseX, mouseY, leftPos, topPos, PANEL_X)
                && panelScrollbar.mouseScrolled(scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
