package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.colossal_reactors.menu.ReactorControllerMenu;
import net.unfamily.colossal_reactors.network.ReactorControllerRefreshPayload;

/**
 * Reactor controller GUI. Background reactor_controller.png (230x240). Dark panel shows status, rods, coolant blocks,
 * energy, coolant, exhaust coolant, fuel. Reboot button on the right, near bottom. Scrollbar when content exceeds the panel.
 */
public class ReactorControllerScreen extends AbstractContainerScreen<ReactorControllerMenu> {

    private static final int GUI_WIDTH = ReactorControllerGui.WIDTH;
    private static final int GUI_HEIGHT = ReactorControllerGui.HEIGHT;

    private static final int PANEL_X = 16;
    private static final int PANEL_Y = GuiPanelScrollbar.TEXT_TOP;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = GuiTextColors.PANEL_WHITE;
    private static final int CONTENT_LEFT = PANEL_X;

    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private static final int REFRESH_BUTTON_WIDTH = 50;
    private static final int REFRESH_BUTTON_HEIGHT = 20;
    private static final int REFRESH_BUTTON_RIGHT_INSET = 12;
    private static final int REFRESH_BUTTON_BOTTOM_INSET = 13;

    private Button closeButton;
    private Button refreshButton;
    private final GuiPanelScrollbar panelScrollbar = new GuiPanelScrollbar();

    public ReactorControllerScreen(ReactorControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        })
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
        int refreshX = leftPos + imageWidth - REFRESH_BUTTON_WIDTH - REFRESH_BUTTON_RIGHT_INSET;
        int refreshY = topPos + imageHeight - REFRESH_BUTTON_HEIGHT - REFRESH_BUTTON_BOTTOM_INSET;
        refreshButton = Button.builder(Component.translatable("gui.colossal_reactors.reactor_controller.reboot"), b -> sendRefresh())
                .bounds(refreshX, refreshY, REFRESH_BUTTON_WIDTH, REFRESH_BUTTON_HEIGHT)
                .tooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.reactor_controller.reboot.tooltip")))
                .build();
        addRenderableWidget(refreshButton);
        panelScrollbar.createButtons(leftPos, topPos, this::addRenderableWidget, () -> {});
    }

    private void sendRefresh() {
        ClientPacketDistributor.sendToServer(new ReactorControllerRefreshPayload(menu.getControllerBlockPos()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ReactorControllerGui.BACKGROUND, leftPos, topPos, 0.0F, 0.0F, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
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

        // Scissor coords are GUI-local; pose is already translated by extractContents.
        guiGraphics.enableScissor(CONTENT_LEFT, PANEL_Y, GuiPanelScrollbar.TEXT_RIGHT, GuiPanelScrollbar.TEXT_BOTTOM);
        int contentHeight = drawPanelContent(guiGraphics, panelScrollbar.getScrollOffset());
        guiGraphics.disableScissor();
        panelScrollbar.setContentHeight(contentHeight);
    }

    private int drawPanelContent(GuiGraphicsExtractor guiGraphics, int scrollOffset) {
        int y = PANEL_Y - scrollOffset;
        int stateId = menu.getControllerStateId();
        boolean effectivelyOff = (stateId == 2 && menu.hasRedstonePort() && !menu.isRedstoneGateSatisfied());
        Component statusKey = switch (stateId) {
            case 0 -> Component.translatable("gui.colossal_reactors.reactor_controller.status.off");
            case 1 -> Component.translatable("gui.colossal_reactors.reactor_controller.status.validating");
            default -> effectivelyOff
                    ? Component.translatable("gui.colossal_reactors.reactor_controller.status.off")
                    : Component.translatable("gui.colossal_reactors.reactor_controller.status.on");
        };
        Component trailing = !menu.hasRedstonePort()
                ? Component.literal(" ").append(Component.translatable("gui.colossal_reactors.reactor_controller.status.requires_redstone_port"))
                : null;
        ReactorPanelText.drawStatusLine(guiGraphics, font, PANEL_X, y, statusKey, trailing);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", menu.getRodCount(), menu.getRodColumns()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_blocks",
                        GuiNumberFormat.format(menu.getCoolantCount())),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        boolean reactorRunning = (stateId == 2 && !effectivelyOff);
        long energyPerTick = reactorRunning ? menu.getEnergyPerTickLong() : 0L;
        int waterPerTick = reactorRunning ? menu.getWaterPerTick() : 0;
        int steamPerTick = reactorRunning ? menu.getSteamPerTick() : 0;
        int fuelHundredths = reactorRunning ? menu.getFuelPerTickHundredths() : 0;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.energy_production",
                        GuiNumberFormat.format(energyPerTick)),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.water_consume",
                        GuiNumberFormat.format(waterPerTick)),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.steam_production",
                        GuiNumberFormat.format(steamPerTick)),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        String fuelStr = formatFuelPerTick(fuelHundredths);
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_units", fuelStr),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        int cap = menu.getFuelCapacityUnits();
        int stored = menu.getFuelStoredUnits();
        String fillStr = cap > 0 ? String.format("%d", (int) Math.round((stored * 100.0) / cap)) : "0";
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_capacity",
                        GuiNumberFormat.format(cap), fillStr),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        int wasteCap = menu.getWasteCapacityUnits();
        int wasteStored = menu.getWasteStoredUnits();
        String wasteFillStr = wasteCap > 0 ? String.format("%d", (int) Math.round((wasteStored * 100.0) / wasteCap)) : "0";
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.waste_capacity",
                        GuiNumberFormat.format(wasteStored), wasteFillStr),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        int coolantCap = menu.getCoolantCapacityMb();
        int coolantStored = menu.getCoolantStoredMb();
        String coolantFillStr = coolantCap > 0 ? String.format("%d", (int) Math.round((coolantStored * 100.0) / coolantCap)) : "0";
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_capacity",
                        GuiNumberFormat.format(coolantCap), coolantFillStr),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        if (menu.isUnstabilityEnabled()) {
            int permille = reactorRunning ? menu.getStabilityPermille() : 1000;
            String stabilityStr = String.format("%.1f%%", permille / 10.0);
            Component label = Component.translatable("gui.colossal_reactors.reactor_controller.stability.label");
            guiGraphics.text(font, label, PANEL_X, y, TEXT_COLOR, false);
            int stabilityColor = stabilityColorFromPermille(permille);
            guiGraphics.text(font, stabilityStr, PANEL_X + font.width(label), y, stabilityColor, false);
            y += LINE_HEIGHT;
        }

        return y - (PANEL_Y - scrollOffset);
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
        if (panelScrollbar.isInPanelArea(mouseX, mouseY, leftPos, topPos, CONTENT_LEFT)
                && panelScrollbar.mouseScrolled(scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static int stabilityColorFromPermille(int permille) {
        float t = Math.max(0, Math.min(1000, permille)) / 1000f;
        int r = (int) (255 * (1f - t));
        int g = (int) (255 * t);
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    private static String formatFuelPerTick(int hundredths) {
        if (hundredths <= 0) return "0";
        int intPart = hundredths / 100;
        if (intPart >= 1000) {
            return GuiNumberFormat.format(hundredths / 100.0);
        }
        int frac = hundredths % 100;
        if (frac == 0) {
            return String.valueOf(intPart);
        }
        String fracStr = String.format("%02d", frac).replaceFirst("0+$", "");
        return intPart + "." + fracStr;
    }
}
