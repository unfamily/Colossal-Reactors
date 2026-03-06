package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.menu.HeatingCoilMenu;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for Heating Coil (port-style). Uses resource_port.png; fluid bar censored with background if coil has no fluid;
 * energy bar on the right when coil uses energy (texture from energy_bar.png, same as iskandert_utilities style).
 */
public class HeatingCoilScreen extends AbstractContainerScreen<HeatingCoilMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/resource_port.png");
    private static final ResourceLocation ENERGY_BAR =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/energy_bar.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int FLUID_BAR_X = 10;
    private static final int FLUID_BAR_Y = 14;
    private static final int FLUID_FILL_WIDTH = 12;
    private static final int FLUID_FILL_HEIGHT = 54;
    private static final int FLUID_FILL_INSET = 1;

    /** Tank total height (fill + border inset on both sides); energy bar is centered within this */
    private static final int TANK_TOTAL_HEIGHT = FLUID_FILL_HEIGHT + 2 * FLUID_FILL_INSET;

    /** Energy bar: 8x32, right side of GUI; vertically centered with tank height */
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    private static final int ENERGY_BAR_X = GUI_WIDTH - ENERGY_BAR_WIDTH - 8;
    private static final int ENERGY_BAR_Y = FLUID_BAR_Y + (TANK_TOTAL_HEIGHT - ENERGY_BAR_HEIGHT) / 2;

    /** Background color to censor tank/slot when not used (#c6c6c6) */
    private static final int CENSOR_COLOR = 0xFFc6c6c6;

    /** Slot position and size (standard 18x18 including border); censor uses +1px each side */
    private static final int SLOT_X = 37;
    private static final int SLOT_Y = 33;
    private static final int SLOT_SIZE = 18;

    /** Close button (X): top right, same as other port screens */
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private Button closeButton;

    public HeatingCoilScreen(HeatingCoilMenu menu, Inventory playerInventory, Component title) {
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
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);

        if (menu.showFluidInGui()) {
            int amount = menu.getFluidAmount();
            int capacity = menu.getFluidCapacity();
            int fluidId = menu.getFluidId();
            if (capacity > 0 && amount > 0 && fluidId >= 0) {
                Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    int fillPixels = (FLUID_FILL_HEIGHT * amount) / capacity;
                    if (fillPixels > 0) {
                        int barLeft = x + FLUID_BAR_X + FLUID_FILL_INSET;
                        int barBottom = y + FLUID_BAR_Y + FLUID_FILL_INSET + FLUID_FILL_HEIGHT;
                        int fillTop = barBottom - fillPixels;
                        FluidRenderHelper.drawFluidInTank(guiGraphics, new FluidStack(fluid, amount), barLeft, fillTop, FLUID_FILL_WIDTH, fillPixels);
                    }
                }
            }
        } else {
            // Censor tank area +1px each side to hide borders
            int cx = x + FLUID_BAR_X - 1;
            int cy = y + FLUID_BAR_Y - 1;
            int cw = FLUID_FILL_WIDTH + 2 * FLUID_FILL_INSET + 2;
            int ch = FLUID_FILL_HEIGHT + 2 * FLUID_FILL_INSET + 2;
            guiGraphics.fill(cx, cy, cx + cw, cy + ch, CENSOR_COLOR);
        }

        if (!menu.showItemInGui()) {
            // Censor slot area +1px each side to hide borders
            int sx = x + SLOT_X - 1;
            int sy = y + SLOT_Y - 1;
            guiGraphics.fill(sx, sy, sx + SLOT_SIZE + 2, sy + SLOT_SIZE + 2, CENSOR_COLOR);
        }

        if (menu.showEnergyInGui()) {
            int energyBarX = x + ENERGY_BAR_X;
            int energyBarY = y + ENERGY_BAR_Y;
            guiGraphics.blit(ENERGY_BAR, energyBarX, energyBarY, 8, 0, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 16, 32);
            int energy = menu.getEnergy();
            int maxEnergy = menu.getEnergyCapacity();
            if (energy > 0 && maxEnergy > 0) {
                int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
                int energyY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);
                guiGraphics.blit(ENERGY_BAR, energyBarX, energyY, 0, ENERGY_BAR_HEIGHT - energyHeight, ENERGY_BAR_WIDTH, energyHeight, 16, 32);
            }
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
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        int left = leftPos + FLUID_BAR_X + FLUID_FILL_INSET;
        int top = topPos + FLUID_BAR_Y + FLUID_FILL_INSET;
        if (menu.showFluidInGui() && mouseX >= left && mouseX < left + FLUID_FILL_WIDTH && mouseY >= top && mouseY < top + FLUID_FILL_HEIGHT) {
            int amount = menu.getFluidAmount();
            int capacity = menu.getFluidCapacity();
            int fluidId = menu.getFluidId();
            Component amountLine = capacity > 0
                    ? Component.translatable("gui.colossal_reactors.resource_port.tank_tooltip", amount, capacity)
                    : Component.translatable("gui.colossal_reactors.resource_port.tank_empty");
            List<FormattedCharSequence> lines = new ArrayList<>();
            lines.add(amountLine.getVisualOrderText());
            if (fluidId >= 0) {
                Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    FluidType type = fluid.getFluidType();
                    lines.add(Component.translatable(type.getDescriptionId()).getVisualOrderText());
                }
            }
            guiGraphics.renderTooltip(font, lines, mouseX, mouseY);
        }
        if (menu.showEnergyInGui()) {
            int ex = leftPos + ENERGY_BAR_X;
            int ey = topPos + ENERGY_BAR_Y;
            if (mouseX >= ex && mouseX < ex + ENERGY_BAR_WIDTH && mouseY >= ey && mouseY < ey + ENERGY_BAR_HEIGHT) {
                Component line = Component.translatable("gui.colossal_reactors.heating_coil.energy_tooltip", menu.getEnergy(), menu.getEnergyCapacity());
                guiGraphics.renderTooltip(font, List.of(line.getVisualOrderText()), mouseX, mouseY);
            }
        }
    }
}
