package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.menu.RedstonePortMenu;
import net.unfamily.colossal_reactors.network.RedstonePortRedstoneModePayload;

/**
 * GUI for Redstone Port: background, title, vanilla redstone mode button.
 */
public class RedstonePortScreen extends AbstractContainerScreen<RedstonePortMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/redstone_port.png");

    private static final int GUI_WIDTH = 100;
    private static final int GUI_HEIGHT = 40;

    private static final int REDSTONE_BUTTON_X = (GUI_WIDTH - RedstoneGuiButtons.ICON_SIZE) / 2;
    private static final int REDSTONE_BUTTON_Y = 18;
    private static final boolean ALLOW_PULSE = false;

    private ItemIconButton redstoneModeButton;

    public RedstonePortScreen(RedstonePortMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        redstoneModeButton = addRenderableWidget(RedstoneGuiButtons.button(
                leftPos + REDSTONE_BUTTON_X,
                topPos + REDSTONE_BUTTON_Y,
                b -> cycleRedstone(true),
                menu::getRedstoneMode,
                ALLOW_PULSE));
        RedstoneGuiButtons.refreshTooltip(redstoneModeButton, menu.getRedstoneMode(), ALLOW_PULSE);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        RedstoneGuiButtons.refreshTooltip(redstoneModeButton, menu.getRedstoneMode(), ALLOW_PULSE);
    }

    private void cycleRedstone(boolean next) {
        PacketDistributor.sendToServer(new RedstonePortRedstoneModePayload(menu.getSyncedBlockPos(), next));
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        int titleX = (imageWidth - titleW) / 2;
        guiGraphics.drawString(font, title, titleX, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1 && redstoneModeButton != null && redstoneModeButton.isMouseOver(mouseX, mouseY)) {
            cycleRedstone(false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
