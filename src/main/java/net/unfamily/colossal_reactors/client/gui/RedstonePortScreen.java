package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.RedstoneMode;
import net.unfamily.colossal_reactors.menu.RedstonePortMenu;
import net.unfamily.colossal_reactors.network.RedstonePortRedstoneModePayload;

/**
 * GUI for Redstone Port. Only background (redstone_port.png), title and redstone button.
 * Button assets from iskandert_utilities (medium_buttons.png, redstone_gui.png) copied into this mod.
 */
public class RedstonePortScreen extends AbstractContainerScreen<RedstonePortMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/redstone_port.png");
    private static final Identifier MEDIUM_BUTTONS = Identifier.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/medium_buttons.png");
    private static final Identifier REDSTONE_GUI = Identifier.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/redstone_gui.png");

    /** Background = only redstone_port.png dimensions (100x40) */
    private static final int GUI_WIDTH = 100;
    private static final int GUI_HEIGHT = 40;

    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int REDSTONE_BUTTON_X = (GUI_WIDTH - REDSTONE_BUTTON_SIZE) / 2;
    /** Button below title with even spacing from title and bottom edge. */
    private static final int REDSTONE_BUTTON_Y = 18;

    private int redstoneButtonScreenX;
    private int redstoneButtonScreenY;

    public RedstonePortScreen(RedstonePortMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        redstoneButtonScreenX = leftPos + REDSTONE_BUTTON_X;
        redstoneButtonScreenY = topPos + REDSTONE_BUTTON_Y;
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
        guiGraphics.text(font, title, titleX, 6, GuiTextColors.TITLE, false);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderRedstoneButton(guiGraphics, mouseX, mouseY);
    }

    private void renderRedstoneButton(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= redstoneButtonScreenX && mouseX < redstoneButtonScreenX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneButtonScreenY && mouseY < redstoneButtonScreenY + REDSTONE_BUTTON_SIZE;

        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MEDIUM_BUTTONS, redstoneButtonScreenX, redstoneButtonScreenY,
                0.0F, (float)textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 96, 96);

        int iconX = redstoneButtonScreenX + 2;
        int iconY = redstoneButtonScreenY + 2;
        int iconSize = 12;

        int mode = menu.getRedstoneMode();
        switch (mode) {
            case 0 -> renderScaledItem(guiGraphics, new ItemStack(Items.GUNPOWDER), iconX, iconY, iconSize);
            case 1 -> renderScaledItem(guiGraphics, new ItemStack(Items.REDSTONE), iconX, iconY, iconSize);
            case 2 -> renderScaledTexture(guiGraphics, REDSTONE_GUI, iconX, iconY, iconSize);
            case 3 -> renderScaledItem(guiGraphics, new ItemStack(Items.REPEATER), iconX, iconY, iconSize);
            case 4 -> renderScaledItem(guiGraphics, new ItemStack(Items.BARRIER), iconX, iconY, iconSize);
            default -> renderScaledItem(guiGraphics, new ItemStack(Items.REDSTONE), iconX, iconY, iconSize);
        }

        if (isHovered) {
            guiGraphics.setTooltipForNextFrame(font, RedstoneMode.fromId(mode).getDisplayName(), mouseX, mouseY);
        }
    }

    private static void renderScaledItem(GuiGraphicsExtractor guiGraphics, ItemStack stack, int x, int y, int size) {
        guiGraphics.pose().pushMatrix();
        float scale = size / 16.0f;
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.item(stack, 0, 0);
        guiGraphics.pose().popMatrix();
    }

    private static void renderScaledTexture(GuiGraphicsExtractor guiGraphics, Identifier texture, int x, int y, int size) {
        guiGraphics.pose().pushMatrix();
        float scale = size / 16.0f;
        guiGraphics.pose().translate(x, y);
        guiGraphics.pose().scale(scale, scale);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, texture, 0, 0, 0.0F, 0.0F, 16, 16, 16, 16);
        guiGraphics.pose().popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.x() >= redstoneButtonScreenX && event.x() < redstoneButtonScreenX + REDSTONE_BUTTON_SIZE
                && event.y() >= redstoneButtonScreenY && event.y() < redstoneButtonScreenY + REDSTONE_BUTTON_SIZE) {
            ClientPacketDistributor.sendToServer(new RedstonePortRedstoneModePayload(menu.getSyncedBlockPos()));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }
}
