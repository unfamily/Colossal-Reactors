package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.menu.ReactorControllerMenu;
import net.unfamily.colossal_reactors.network.ReactorControllerRefreshPayload;

/**
 * Reactor controller GUI. Background reactor_controller.png (230x230). Dark panel shows status, rods, coolant blocks, energy, coolant, exhaust coolant, fuel. Reboot button on the right, near bottom.
 */
public class ReactorControllerScreen extends AbstractContainerScreen<ReactorControllerMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/reactor_controller.png");

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 230;

    /** Dark panel: top-left (11, 19), text offset 5px right and down; labels 5px lower */
    private static final int PANEL_X = 16;
    private static final int PANEL_Y = 29;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = 0xFFFFFF;

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
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.getSoundManager() != null)
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
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
        PacketDistributor.sendToServer(new ReactorControllerRefreshPayload(menu.getControllerBlockPos()));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        int titleX = (imageWidth - titleW) / 2;
        guiGraphics.drawString(font, title, titleX, 5, 0x404040, false);

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
        guiGraphics.drawString(font, statusLine, PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", menu.getRodCount(), menu.getRodColumns()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_blocks", menu.getCoolantCount()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.energy_production", menu.getEnergyPerTick()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.water_consume", menu.getWaterPerTick()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.steam_production", menu.getSteamPerTick()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        String fuelStr = formatFuelPerTick(menu.getFuelPerTickHundredths());
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_units", fuelStr),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        if (menu.isUnstabilityEnabled()) {
            int permille = menu.getStabilityPermille();
            String stabilityStr = String.format("%.1f%%", permille / 10.0);
            Component label = Component.translatable("gui.colossal_reactors.reactor_controller.stability.label");
            guiGraphics.drawString(font, label, PANEL_X, y, TEXT_COLOR, false);
            int stabilityColor = stabilityColorFromPermille(permille);
            guiGraphics.drawString(font, stabilityStr, PANEL_X + font.width(label), y, stabilityColor, false);
        }
    }

    /** Green at 100% stability, red at 0%; interpolates in between. */
    private static int stabilityColorFromPermille(int permille) {
        float t = Math.max(0, Math.min(1000, permille)) / 1000f;
        int r = (int) (255 * (1f - t));
        int g = (int) (255 * t);
        return (r << 16) | (g << 8) | 0;
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
