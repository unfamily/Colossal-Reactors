package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.client.gui.GasTankRenderHelper.GasRenderInfo;
import net.unfamily.colossal_reactors.menu.RadiationScrubberMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * Radiation Scrubber GUI. Texture 176x166. Slot 0 at (44,33), Slot 1 (catalyst) at (80,33). Energy bar on right. Default and custom tooltips.
 */
public class RadiationScrubberScreen extends AbstractContainerScreen<RadiationScrubberMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/radiation_scrubber.png");
    private static final Identifier ENERGY_BAR =
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/energy_bar.png");

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
        super(menu, playerInventory, title, GUI_WIDTH, GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        }).bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);

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
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, energyBarY, 8.0F, 0.0F, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 16, 32);
        int energy = menu.getEnergy();
        int maxEnergy = menu.getEnergyCapacity();
        if (energy > 0 && maxEnergy > 0) {
            int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
            int fillY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, fillY, 0.0F, (float)(ENERGY_BAR_HEIGHT - energyHeight), ENERGY_BAR_WIDTH, energyHeight, ENERGY_BAR_WIDTH, energyHeight, 16, 32);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        int titleX = (imageWidth - titleW) / 2;
        guiGraphics.text(font, title, titleX, 6, GuiTextColors.TITLE, false);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);

        int ex = leftPos + ENERGY_BAR_X;
        int ey = topPos + ENERGY_BAR_Y;
        if (mouseX >= ex && mouseX < ex + ENERGY_BAR_WIDTH && mouseY >= ey && mouseY < ey + ENERGY_BAR_HEIGHT) {
            Component line = Component.translatable("gui.colossal_reactors.radiation_scrubber.energy_tooltip", menu.getEnergy(), menu.getEnergyCapacity());
            guiGraphics.setTooltipForNextFrame(font, List.of(line.getVisualOrderText()), mouseX, mouseY);
        }
        int tx = leftPos + TANK_X;
        int ty = topPos + TANK_Y;
        if (mouseX >= tx && mouseX < tx + TANK_WIDTH && mouseY >= ty && mouseY < ty + TANK_HEIGHT) {
            int amount = menu.getChemicalTankAmount();
            int capacity = menu.getChemicalTankCapacity();
            String gasType = menu.getChemicalTypeRegistryName();
            Component gasName = gasType != null ? GasTankRenderHelper.getGasDisplayName(gasType) : null;
            List<Component> lineComponents = new ArrayList<>();
            if (gasName != null) lineComponents.add(gasName);
            if (capacity > 0) {
                lineComponents.add(Component.translatable("gui.colossal_reactors.radiation_scrubber.tank_tooltip", amount, capacity));
            } else {
                lineComponents.add(Component.translatable("gui.colossal_reactors.radiation_scrubber.tank_empty"));
            }
            List<FormattedCharSequence> lines = lineComponents.stream().map(Component::getVisualOrderText).toList();
            guiGraphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
        }
    }
}
