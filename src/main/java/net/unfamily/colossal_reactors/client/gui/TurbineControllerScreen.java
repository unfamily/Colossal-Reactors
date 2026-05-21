package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.colossal_reactors.block.TurbineVisualState;
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
    private static final int PANEL_TEXT_WIDTH = GuiPanelScrollbar.TEXT_RIGHT - PANEL_X;

    private static final int CLOSE_BUTTON_X = ReactorControllerGui.closeButtonX(GUI_WIDTH);

    private Button closeButton;
    private final GuiPanelScrollbar panelScrollbar = new GuiPanelScrollbar();

    public TurbineControllerScreen(TurbineControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.getSoundManager() != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        })
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + ReactorControllerGui.HEADER_BUTTON_Y,
                        ReactorControllerGui.HEADER_BUTTON_SIZE, ReactorControllerGui.HEADER_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
        panelScrollbar.createButtons(leftPos, topPos, this::addRenderableWidget, () -> {});
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(ReactorControllerGui.BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        panelScrollbar.render(guiGraphics, leftPos, topPos);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        int titleX = (imageWidth - titleW) / 2;
        guiGraphics.drawString(font, title, titleX, ReactorControllerGui.titleLabelY(font), 0x404040, false);

        guiGraphics.enableScissor(leftPos + PANEL_X, topPos + PANEL_Y, leftPos + GuiPanelScrollbar.TEXT_RIGHT, topPos + GuiPanelScrollbar.TEXT_BOTTOM);
        int contentHeight = drawPanelContent(guiGraphics, panelScrollbar.getScrollOffset());
        guiGraphics.disableScissor();
        panelScrollbar.setContentHeight(contentHeight);
    }

    private int drawPanelContent(GuiGraphics guiGraphics, int scrollOffset) {
        int y = PANEL_Y - scrollOffset;
        int contentStart = y;

        if (!menu.isValid()) {
            ReactorPanelText.drawStatusLine(guiGraphics, font, PANEL_X, y,
                    Component.translatable("gui.colossal_reactors.turbine_controller.invalid"), null);
            y += LINE_HEIGHT;

            Component failure = TurbineValidation.failureMessage(menu.getFailureOrdinal());
            for (var line : font.split(failure, PANEL_TEXT_WIDTH)) {
                guiGraphics.drawString(font, line, PANEL_X, y, 0xFF5555, false);
                y += LINE_HEIGHT;
            }
            return y - contentStart;
        }

        int visualId = menu.getVisualStateId();
        boolean effectivelyOff = visualId == TurbineVisualState.ON.ordinal()
                && menu.hasRedstonePort()
                && !menu.isRedstoneGateSatisfied();
        Component statusKey = switch (visualId) {
            case 0 -> Component.translatable("gui.colossal_reactors.reactor_controller.status.off");
            case 1 -> Component.translatable("gui.colossal_reactors.reactor_controller.status.validating");
            default -> effectivelyOff
                    ? Component.translatable("gui.colossal_reactors.reactor_controller.status.off")
                    : Component.translatable("gui.colossal_reactors.reactor_controller.status.on");
        };
        Component trailing = !menu.hasRedstonePort()
                ? Component.literal(" ").append(Component.translatable(
                "gui.colossal_reactors.turbine_controller.status.requires_redstone_port"))
                : null;
        ReactorPanelText.drawStatusLine(guiGraphics, font, PANEL_X, y, statusKey, trailing);
        y += LINE_HEIGHT;

        boolean turbineRunning = visualId == TurbineVisualState.ON.ordinal() && !effectivelyOff;
        long rfPerTick = turbineRunning ? menu.getRfPerTick() : 0L;
        int steamPerTick = turbineRunning ? menu.getSteamPerTick() : 0;
        double bladeEff = turbineRunning ? menu.getBladeEff() : 0.0;

        y = ReactorPanelText.drawMetricRow(guiGraphics, font, PANEL_X, y, LINE_HEIGHT,
                "gui.colossal_reactors.turbine_controller.blades_valid.label",
                Component.translatable("gui.colossal_reactors.turbine_controller.blades_valid.value",
                        GuiNumberFormat.format(menu.getBladeCount())));
        y = ReactorPanelText.drawMetricRow(guiGraphics, font, PANEL_X, y, LINE_HEIGHT,
                "gui.colossal_reactors.turbine_controller.energy_runtime.label",
                Component.translatable("gui.colossal_reactors.turbine_controller.energy_runtime.value",
                        GuiNumberFormat.format(rfPerTick)));
        y = ReactorPanelText.drawMetricRow(guiGraphics, font, PANEL_X, y, LINE_HEIGHT,
                "gui.colossal_reactors.turbine_controller.steam_runtime.label",
                Component.translatable("gui.colossal_reactors.turbine_controller.steam_runtime.value",
                        GuiNumberFormat.format(steamPerTick)));
        y = ReactorPanelText.drawMetricRow(guiGraphics, font, PANEL_X, y, LINE_HEIGHT,
                "gui.colossal_reactors.turbine_controller.efficiency.label",
                Component.translatable("gui.colossal_reactors.turbine_controller.efficiency.value",
                        String.format("%.2f", bladeEff)));

        return y - contentStart;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && panelScrollbar.mouseClicked(mouseX, mouseY, leftPos, topPos)) {
            return true;
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
