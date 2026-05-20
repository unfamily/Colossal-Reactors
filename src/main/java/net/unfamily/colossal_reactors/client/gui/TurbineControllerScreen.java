package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.colossal_reactors.menu.TurbineControllerMenu;

/** Turbine controller stats panel. */
public class TurbineControllerScreen extends AbstractContainerScreen<TurbineControllerMenu> {

    private static final int GUI_WIDTH = ReactorControllerGui.WIDTH;
    private static final int GUI_HEIGHT = ReactorControllerGui.HEIGHT;

    public TurbineControllerScreen(TurbineControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ReactorControllerGui.BACKGROUND, leftPos, topPos, 0.0F, 0.0F,
                GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int y = 20;
        guiGraphics.text(font, Component.translatable("gui.colossal_reactors.turbine_controller.title"),
                10, y, GuiTextColors.TITLE, false);
        y += 12;
        if (menu.isValid()) {
            guiGraphics.text(font, Component.translatable("gui.colossal_reactors.turbine_controller.valid"),
                    10, y, 0xFF55FF55, false);
            y += 12;
            guiGraphics.text(font, Component.translatable("gui.colossal_reactors.turbine_controller.energy_production",
                            menu.getRfPerTick()),
                    10, y, GuiTextColors.PANEL_WHITE, false);
            y += 12;
            guiGraphics.text(font, Component.translatable("gui.colossal_reactors.turbine_controller.steam_production",
                            menu.getSteamPerTick()),
                    10, y, GuiTextColors.PANEL_WHITE, false);
            y += 12;
            guiGraphics.text(font, Component.translatable("gui.colossal_reactors.turbine_controller.rods",
                            menu.getBladeCount(), menu.getBladeCount()),
                    10, y, GuiTextColors.PANEL_WHITE, false);
            y += 12;
            guiGraphics.text(font, Component.translatable("jei.colossal_reactors.elec_coil.eff_coe",
                            String.format("%.2f", menu.getCoilEff())),
                    10, y, GuiTextColors.PANEL_WHITE, false);
            y += 12;
            guiGraphics.text(font, Component.translatable("jei.colossal_reactors.elec_coil.eff_max",
                            String.format("%.2f", menu.getBladeEff())),
                    10, y, GuiTextColors.PANEL_WHITE, false);
        } else {
            guiGraphics.text(font, Component.translatable("gui.colossal_reactors.turbine_controller.invalid"),
                    10, y, 0xFFFF5555, false);
        }
    }
}
