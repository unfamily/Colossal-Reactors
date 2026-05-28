package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.menu.RadiationScrubberMenu;

import java.util.List;

/**
 * Radiation Scrubber GUI. Texture 176x176. Slot 0 at (44,38), Slot 1 (catalyst) at (80,38). Energy bar on right. Default and custom tooltips.
 */
public class RadiationScrubberScreen extends AbstractContainerScreen<RadiationScrubberMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/radiation_scrubber.png");
    private static final ResourceLocation BORON_DUST_GHOST =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/item/boron_dust.png");
    private static final ResourceLocation PRODUCTION_MODULE_GHOST =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/item/production_module.png");
    private static final ResourceLocation ENERGY_BAR =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/energy_bar.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 176;
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    /** Gas tank on radiation_scrubber.png (inclusive 118,20 – 128,73); full rect, no code inset for frame. */
    private static final int TANK_LEFT = 118;
    private static final int TANK_TOP = 20;
    private static final int TANK_RIGHT = 128;
    private static final int TANK_BOTTOM = 73;
    private static final int TANK_WIDTH = TANK_RIGHT - TANK_LEFT + 1;
    private static final int TANK_HEIGHT = TANK_BOTTOM - TANK_TOP + 1;
    /** Gas fill: +1 px wide vs texture frame (same left edge as tank). */
    private static final int GAS_FILL_WIDTH = TANK_WIDTH + 1;

    /** Energy bar: same position as HeatingCoilScreen (8x32, right side, vertically centered with tank area) */
    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    private static final int ENERGY_BAR_X = GUI_WIDTH - ENERGY_BAR_WIDTH - 8;
    private static final int ENERGY_BAR_Y = TANK_TOP + (TANK_HEIGHT + 2 - ENERGY_BAR_HEIGHT) / 2;

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

        renderSlotGhosts(guiGraphics);

        int tankAmount = menu.getChemicalTankAmount();
        int tankCapacity = menu.getChemicalTankCapacity();
        int gasLeft = x + TANK_LEFT;
        int barTop = y + TANK_TOP;
        if (tankCapacity > 0 && tankAmount > 0) {
            int fillHeight = (tankAmount * TANK_HEIGHT) / tankCapacity;
            String chemName = menu.getChemicalTypeRegistryName();
            if (!GasTankRenderHelper.drawGasInTank(guiGraphics, chemName, tankAmount, gasLeft, barTop,
                    GAS_FILL_WIDTH, TANK_HEIGHT, fillHeight)) {
                int fillTop = barTop + TANK_HEIGHT - fillHeight;
                guiGraphics.fill(gasLeft, fillTop, gasLeft + GAS_FILL_WIDTH, barTop + TANK_HEIGHT, 0xFF_80_FF_80);
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

        renderSlotTooltips(guiGraphics, mouseX, mouseY);

        int ex = leftPos + ENERGY_BAR_X;
        int ey = topPos + ENERGY_BAR_Y;
        if (mouseX >= ex && mouseX < ex + ENERGY_BAR_WIDTH && mouseY >= ey && mouseY < ey + ENERGY_BAR_HEIGHT) {
            Component line = Component.translatable("gui.colossal_reactors.radiation_scrubber.energy_tooltip", menu.getEnergy(), menu.getEnergyCapacity());
            guiGraphics.renderTooltip(font, List.of(line.getVisualOrderText()), mouseX, mouseY);
        }
        int tx = leftPos + TANK_LEFT;
        int ty = topPos + TANK_TOP;
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

    private void renderSlotGhosts(GuiGraphics guiGraphics) {
        // Slot 0: show Boron Dust as common catalyst example (rendered as texture to allow transparency).
        if (menu.slots.size() >= 2) {
            var s0 = menu.getSlot(0);
            if (s0 != null && s0.getItem().isEmpty()) {
                renderGhostTexture(guiGraphics, BORON_DUST_GHOST, s0.x, s0.y);
            }
            var s1 = menu.getSlot(1);
            if (s1 != null && s1.getItem().isEmpty()) {
                renderGhostTexture(guiGraphics, PRODUCTION_MODULE_GHOST, s1.x, s1.y);
            }
        }
    }

    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack stack, int sx, int sy) {
        if (stack.isEmpty()) return;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + sx, topPos + sy, 0);
        guiGraphics.renderItem(stack, 0, 0);
        // Semi-transparent white overlay (no black background).
        guiGraphics.fill(0, 0, 16, 16, 0x80FFFFFF);
        guiGraphics.pose().popPose();
    }

    private void renderGhostTexture(GuiGraphics guiGraphics, ResourceLocation texture, int sx, int sy) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(leftPos + sx, topPos + sy, 0);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.fill(0, 0, 16, 16, 0x80FFFFFF);
        guiGraphics.pose().popPose();
    }

    private void renderSlotTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (menu.slots.size() < 2) return;
        var s0 = menu.getSlot(0);
        if (s0 != null && isMouseOverSlotArea(mouseX, mouseY, s0.x, s0.y)) {
            guiGraphics.renderTooltip(font, List.of(
                    Component.translatable("gui.colossal_reactors.radiation_scrubber.slot.catalyst").getVisualOrderText()
            ), mouseX, mouseY);
            return;
        }
        var s1 = menu.getSlot(1);
        if (s1 != null && isMouseOverSlotArea(mouseX, mouseY, s1.x, s1.y)) {
            guiGraphics.renderTooltip(font, List.of(
                    Component.translatable("gui.colossal_reactors.radiation_scrubber.slot.modules").getVisualOrderText(),
                    Component.translatable("gui.colossal_reactors.radiation_scrubber.slot.modules.accepts").getVisualOrderText()
            ), mouseX, mouseY);
        }
    }

    private boolean isMouseOverSlotArea(int mouseX, int mouseY, int sx, int sy) {
        int x0 = leftPos + sx;
        int y0 = topPos + sy;
        return mouseX >= x0 && mouseX < x0 + 16 && mouseY >= y0 && mouseY < y0 + 16;
    }
}
