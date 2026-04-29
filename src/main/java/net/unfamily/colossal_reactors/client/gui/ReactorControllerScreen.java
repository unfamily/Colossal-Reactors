package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.menu.ReactorControllerMenu;
import net.unfamily.colossal_reactors.network.ReactorControllerRefreshPayload;

/**
 * Reactor controller GUI. Background reactor_controller.png (230x230). Dark panel shows status, rods, coolant blocks, energy, coolant, exhaust coolant, fuel. Reboot button on the right, near bottom.
 */
public class ReactorControllerScreen extends AbstractContainerScreen<ReactorControllerMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/reactor_controller.png");

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 230;

    /** Dark panel: top-left (11, 19), text offset 5px right and down; labels 5px lower */
    private static final int PANEL_X = 16;
    private static final int PANEL_Y = 29;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = GuiTextColors.PANEL_WHITE;

    /** Close button (X): top right, same as iskandert_utilities */
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    /** Reboot button: bottom right, slightly above edge */
    private static final int REFRESH_BUTTON_WIDTH = 50;
    private static final int REFRESH_BUTTON_HEIGHT = 20;
    private static final int REFRESH_BUTTON_RIGHT_INSET = 12;
    private static final int REFRESH_BUTTON_BOTTOM_INSET = 13;

    private Button closeButton;
    private Button refreshButton;

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
    }

    private void sendRefresh() {
        ClientPacketDistributor.sendToServer(new ReactorControllerRefreshPayload(menu.getControllerBlockPos()));
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        int titleX = (imageWidth - titleW) / 2;
        guiGraphics.text(font, title, titleX, 5, GuiTextColors.TITLE, false);

        int y = PANEL_Y;
        int stateId = menu.getControllerStateId();
        // When ON but redstone port blocks, show OFF
        boolean effectivelyOff = (stateId == 2 && menu.hasRedstonePort() && !menu.isRedstoneGateSatisfied());
        Component statusKey = switch (stateId) {
            case 0 -> Component.translatable("gui.colossal_reactors.reactor_controller.status.off");
            case 1 -> Component.translatable("gui.colossal_reactors.reactor_controller.status.validating");
            default -> effectivelyOff
                    ? Component.translatable("gui.colossal_reactors.reactor_controller.status.off")
                    : Component.translatable("gui.colossal_reactors.reactor_controller.status.on");
        };
        Component statusLine = Component.translatable("gui.colossal_reactors.reactor_controller.status", statusKey);
        if (!menu.hasRedstonePort()) {
            statusLine = statusLine.copy().append(Component.literal(" ")).append(Component.translatable("gui.colossal_reactors.reactor_controller.status.requires_redstone_port"));
        }
        guiGraphics.text(font, statusLine, PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", menu.getRodCount(), menu.getRodColumns()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_blocks", menu.getCoolantCount()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        // Production/consumption only when reactor is actually running (ON and redstone satisfied)
        boolean reactorRunning = (stateId == 2 && !effectivelyOff);
        int energyPerTick = reactorRunning ? menu.getEnergyPerTick() : 0;
        int waterPerTick = reactorRunning ? menu.getWaterPerTick() : 0;
        int steamPerTick = reactorRunning ? menu.getSteamPerTick() : 0;
        int fuelHundredths = reactorRunning ? menu.getFuelPerTickHundredths() : 0;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.energy_production", energyPerTick),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.water_consume", waterPerTick),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.steam_production", steamPerTick),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        String fuelStr = formatFuelPerTick(fuelHundredths);
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_units", fuelStr),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        if (menu.isUnstabilityEnabled()) {
            int permille = reactorRunning ? menu.getStabilityPermille() : 1000;
            String stabilityStr = String.format("%.1f%%", permille / 10.0);
            Component label = Component.translatable("gui.colossal_reactors.reactor_controller.stability.label");
            guiGraphics.text(font, label, PANEL_X, y, TEXT_COLOR, false);
            int stabilityColor = stabilityColorFromPermille(permille);
            guiGraphics.text(font, stabilityStr, PANEL_X + font.width(label), y, stabilityColor, false);
        }
    }

    /** Green at 100% stability, red at 0%; interpolates in between. */
    private static int stabilityColorFromPermille(int permille) {
        float t = Math.max(0, Math.min(1000, permille)) / 1000f;
        int r = (int) (255 * (1f - t));
        int g = (int) (255 * t);
        return 0xFF000000 | (r << 16) | (g << 8);
    }

    private static String formatFuelPerTick(int hundredths) {
        if (hundredths <= 0) return "0";
        int intPart = hundredths / 100;
        int frac = hundredths % 100;
        if (frac == 0) {
            return String.valueOf(intPart);
        }
        String fracStr = String.format("%02d", frac).replaceFirst("0+$", "");
        return intPart + "." + fracStr;
    }
}
