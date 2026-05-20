package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.colossal_reactors.menu.TurbineControllerMenu;

/** Turbine controller stats panel. */
public class TurbineControllerScreen extends AbstractContainerScreen<TurbineControllerMenu> {

    public TurbineControllerScreen(TurbineControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 230;
        this.imageHeight = 240;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(ReactorControllerGui.BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight, imageWidth, imageHeight);
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int y = 20;
        g.drawString(font, Component.translatable("gui.colossal_reactors.turbine_controller.title"), leftPos + 10, topPos + y, 0xFFFFFF, false);
        y += 12;
        if (menu.isValid()) {
            g.drawString(font, Component.translatable("gui.colossal_reactors.turbine_controller.valid"), leftPos + 10, topPos + y, 0x55FF55, false);
            y += 12;
            g.drawString(font, Component.literal("RF/t: " + menu.getRfPerTick()), leftPos + 10, topPos + y, 0xFFFFFF, false);
            y += 12;
            g.drawString(font, Component.literal("Steam/t: " + menu.getSteamPerTick()), leftPos + 10, topPos + y, 0xFFFFFF, false);
            y += 12;
            g.drawString(font, Component.literal("Blades: " + menu.getBladeCount()), leftPos + 10, topPos + y, 0xFFFFFF, false);
            y += 12;
            g.drawString(font, Component.literal(String.format("Coil eff: %.2f  Blade eff: %.2f", menu.getCoilEff(), menu.getBladeEff())),
                    leftPos + 10, topPos + y, 0xFFFFFF, false);
        } else {
            g.drawString(font, Component.translatable("gui.colossal_reactors.turbine_controller.invalid"), leftPos + 10, topPos + y, 0xFF5555, false);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }
}
