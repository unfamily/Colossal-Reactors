package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.client.gui.GasTankRenderHelper.GasRenderInfo;
import net.unfamily.colossal_reactors.menu.RadiationScrubberMenu;

import java.util.List;

/**
 * Radiation Scrubber GUI. Texture 176x166. Slot 0 at (44,33), Slot 1 (catalyst) at (80,33). Energy bar on right. Default and custom tooltips.
 */
public class RadiationScrubberScreen extends AbstractContainerScreen<RadiationScrubberMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/radiation_scrubber.png");
    private static final ResourceLocation ENERGY_BAR =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/energy_bar.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    /** Tank at same position as melter: (117, 14), 12x54. Inner area +2 px right and +2 px bottom for gas rendering. */
    private static final int TANK_X = 117;
    private static final int TANK_Y = 14;
    private static final int TANK_WIDTH = 12;
    private static final int TANK_HEIGHT = 54;
    private static final int TANK_INSET = 1;
    /** Gas draw area: inner width + 2, inner height + 2 (like Mekanism gauge). */
    private static final int TANK_DRAW_WIDTH = (TANK_WIDTH - 2 * TANK_INSET) + 2;
    private static final int TANK_DRAW_HEIGHT = (TANK_HEIGHT - 2 * TANK_INSET) + 2;
    /** Vertical offset for gas rendering (pixels down). */
    private static final int TANK_DRAW_Y_OFFSET = 2;

    /** Energy bar: same position as HeatingCoilScreen (8x32, right side, vertically centered with tank area) */
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    private static final int ENERGY_BAR_X = GUI_WIDTH - ENERGY_BAR_WIDTH - 8;
    private static final int ENERGY_BAR_Y = 14 + (54 + 2 - ENERGY_BAR_HEIGHT) / 2;

    public RadiationScrubberScreen(RadiationScrubberMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.getSoundManager() != null)
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        }).bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build());
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);

        int tankAmount = menu.getChemicalTankAmount();
        int tankCapacity = menu.getChemicalTankCapacity();
        int barLeft = x + TANK_X + TANK_INSET;
        if (tankCapacity > 0 && tankAmount > 0) {
            int fillHeight = (tankAmount * TANK_DRAW_HEIGHT) / tankCapacity;
            int fillTop = y + TANK_Y + TANK_HEIGHT - TANK_INSET - fillHeight;
            GasRenderInfo gasInfo = GasTankRenderHelper.getGasRenderInfoFromRegistryName(menu.getChemicalTypeRegistryName());
            int drawY = fillTop + TANK_DRAW_Y_OFFSET;
            if (gasInfo != null && !gasInfo.isEmpty()) {
                GasTankRenderHelper.drawGasInTank(guiGraphics, gasInfo, barLeft, drawY, TANK_DRAW_WIDTH, fillHeight);
            } else {
                guiGraphics.fill(barLeft, drawY, barLeft + TANK_DRAW_WIDTH, drawY + fillHeight, 0xFF_80_FF_80);
            }
        }
        int energyBarX = x + ENERGY_BAR_X;
        int energyBarY = y + ENERGY_BAR_Y;
        guiGraphics.blit(ENERGY_BAR, energyBarX, energyBarY, 8, 0, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 16, 32);
        int energy = menu.getEnergy();
        int maxEnergy = menu.getEnergyCapacity();
        if (energy > 0 && maxEnergy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int fillY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);
            guiGraphics.blit(ENERGY_BAR, energyBarX, fillY, 0, ENERGY_BAR_HEIGHT - energyHeight, ENERGY_BAR_WIDTH, energyHeight, 16, 32);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        int titleX = (imageWidth - titleW) / 2;
        guiGraphics.drawString(font, title, titleX, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int ex = leftPos + ENERGY_BAR_X;
        int ey = topPos + ENERGY_BAR_Y;
        if (mouseX >= ex && mouseX < ex + ENERGY_BAR_WIDTH && mouseY >= ey && mouseY < ey + ENERGY_BAR_HEIGHT) {
            Component line = Component.translatable("gui.colossal_reactors.radiation_scrubber.energy_tooltip", menu.getEnergy(), menu.getEnergyCapacity());
            guiGraphics.renderTooltip(font, List.of(line.getVisualOrderText()), mouseX, mouseY);
        }
        int tx = leftPos + TANK_X;
        int ty = topPos + TANK_Y;
        if (mouseX >= tx && mouseX < tx + TANK_WIDTH && mouseY >= ty && mouseY < ty + TANK_HEIGHT) {
            int amount = menu.getChemicalTankAmount();
            int capacity = menu.getChemicalTankCapacity();
            String gasType = menu.getChemicalTypeRegistryName();
            Component gasName = gasType != null ? GasTankRenderHelper.getGasDisplayName(gasType) : null;
            List<Component> lines = new java.util.ArrayList<>();
            if (gasName != null) lines.add(gasName);
            if (capacity > 0) {
                lines.add(Component.translatable("gui.colossal_reactors.radiation_scrubber.tank_tooltip", amount, capacity));
            } else {
                lines.add(Component.translatable("gui.colossal_reactors.radiation_scrubber.tank_empty"));
            }
            guiGraphics.renderTooltip(font, lines.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
        }
    }
}
