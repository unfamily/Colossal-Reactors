package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.menu.RedstonePortMenu;
import net.unfamily.colossal_reactors.network.RedstonePortRedstoneModePayload;

/**
 * GUI for Redstone Port: background, title, vanilla redstone mode button.
 */
public class RedstonePortScreen extends AbstractContainerScreen<RedstonePortMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/redstone_port.png");

    private static final int GUI_WIDTH = 100;
    private static final int GUI_HEIGHT = 40;

    private static final int REDSTONE_BUTTON_X = (GUI_WIDTH - RedstoneGuiButtons.ICON_SIZE) / 2;
    private static final int REDSTONE_BUTTON_Y = 18;
    private static final boolean ALLOW_PULSE = false;

    private ItemIconButton redstoneModeButton;

    public RedstonePortScreen(RedstonePortMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
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
        ClientPacketDistributor.sendToServer(new RedstonePortRedstoneModePayload(menu.getSyncedBlockPos(), next));
    }

    private void playClick() {
        if (minecraft != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        int titleX = (imageWidth - titleW) / 2;
        guiGraphics.text(font, title, titleX, 6, GuiTextColors.TITLE, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 1 && redstoneModeButton != null
                && redstoneModeButton.isMouseOver(event.x(), event.y())) {
            playClick();
            cycleRedstone(false);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
