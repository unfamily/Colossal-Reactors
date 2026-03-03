package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.menu.ReactorControllerMenu;

/**
 * Reactor controller GUI. Background reactor_controller.png (230x150). Dark panel (11,19)-(218,137) shows stats in white.
 */
public class ReactorControllerScreen extends AbstractContainerScreen<ReactorControllerMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/reactor_controller.png");

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 150;

    /** Dark panel: top-left (11, 19), text offset 5px right and down */
    private static final int PANEL_X = 16;
    private static final int PANEL_Y = 24;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = 0xFFFFFF;

    public ReactorControllerScreen(ReactorControllerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
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
        Component statusKey = switch (stateId) {
            case 0 -> Component.translatable("gui.colossal_reactors.reactor_controller.status.off");
            case 1 -> Component.translatable("gui.colossal_reactors.reactor_controller.status.validating");
            default -> Component.translatable("gui.colossal_reactors.reactor_controller.status.on");
        };
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.status", statusKey),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", menu.getRodCount(), menu.getRodColumns()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant", menu.getCoolantCount()),
                PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        Component fuelLabel = menu.hasFuel()
                ? Component.translatable("gui.colossal_reactors.reactor_controller.fuel.yes")
                : Component.translatable("gui.colossal_reactors.reactor_controller.fuel.na");
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel", fuelLabel),
                PANEL_X, y, TEXT_COLOR, false);
    }
}
