package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.RedstoneMode;
import net.unfamily.colossal_reactors.client.gui.FluidRenderHelper;
import net.unfamily.colossal_reactors.menu.MelterMenu;
import net.unfamily.colossal_reactors.network.MelterRedstoneModePayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Melter GUI. Slot at (44,33), tank at (118,15), progress bar between them (left to right).
 * Empty bar always visible on top of fill. Close button X. Tooltips for tank and default slot.
 */
public class MelterScreen extends AbstractContainerScreen<MelterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/melter.png");
    private static final ResourceLocation PROGRESS_EMPTY =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/progress_empty.png");
    private static final ResourceLocation PROGRESS_FILLED =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/progress_filled.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/medium_buttons.png");
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/redstone_gui.png");
    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int REDSTONE_BUTTON_X = CLOSE_BUTTON_X - REDSTONE_BUTTON_SIZE - 4;

    /** Input slot (44, 33) — +1 from border position */
    private static final int SLOT_X = 44;
    private static final int SLOT_Y = 33;
    private static final int SLOT_SIZE = 18;
    /** Redstone button Y: vertically centered with input item slot */
    private static final int REDSTONE_BUTTON_Y = SLOT_Y + (SLOT_SIZE - REDSTONE_BUTTON_SIZE) / 2;

    /** Tank at (117, 14) — 1px left and 1px up from previous */
    private static final int FLUID_BAR_X = 117;
    private static final int FLUID_BAR_Y = 14;
    private static final int FLUID_FILL_WIDTH = 12;
    private static final int FLUID_FILL_HEIGHT = 54;
    private static final int FLUID_FILL_INSET = 1;

    /** Progress bar 24x16, between slot and tank; vertically centered with slot (slot center Y = 32 + 9 = 41) */
    private static final int PROGRESS_BAR_WIDTH = 24;
    private static final int PROGRESS_BAR_HEIGHT = 16;
    private static final int PROGRESS_BAR_X = 77;
    private static final int PROGRESS_BAR_Y = SLOT_Y + (18 - PROGRESS_BAR_HEIGHT) / 2;

    /** Exposed for JEI recipe click area (click on progress bar opens Melter recipes). */
    public static int getProgressBarX() { return PROGRESS_BAR_X; }
    public static int getProgressBarY() { return PROGRESS_BAR_Y; }
    public static int getProgressBarWidth() { return PROGRESS_BAR_WIDTH; }
    public static int getProgressBarHeight() { return PROGRESS_BAR_HEIGHT; }

    private Button closeButton;
    private int redstoneButtonScreenX;
    private int redstoneButtonScreenY;

    public MelterScreen(MelterMenu menu, Inventory playerInventory, Component title) {
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
        redstoneButtonScreenX = leftPos + REDSTONE_BUTTON_X;
        redstoneButtonScreenY = topPos + REDSTONE_BUTTON_Y;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);

        // Progress bar: left to right (like Pattern-Crafter style). Draw background first, then fill on top.
        int progress = menu.getProgress();
        int maxProgress = menu.getMaxProgress();
        int fillWidth = (maxProgress > 0 && progress > 0) ? (progress * PROGRESS_BAR_WIDTH) / maxProgress : 0;
        guiGraphics.blit(PROGRESS_EMPTY, x + PROGRESS_BAR_X, y + PROGRESS_BAR_Y,
                0, 0, PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT,
                PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
        if (fillWidth > 0) {
            guiGraphics.blit(PROGRESS_FILLED, x + PROGRESS_BAR_X, y + PROGRESS_BAR_Y,
                    0, 0, fillWidth, PROGRESS_BAR_HEIGHT,
                    PROGRESS_BAR_WIDTH, PROGRESS_BAR_HEIGHT);
        }

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
        renderRedstoneButton(guiGraphics, mouseX, mouseY);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderRedstoneButton(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        boolean isHovered = mouseX >= redstoneButtonScreenX && mouseX < redstoneButtonScreenX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneButtonScreenY && mouseY < redstoneButtonScreenY + REDSTONE_BUTTON_SIZE;
        int textureY = isHovered ? 16 : 0;
        guiGraphics.blit(MEDIUM_BUTTONS, redstoneButtonScreenX, redstoneButtonScreenY,
                0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 96, 96);
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
            guiGraphics.renderTooltip(font, RedstoneMode.fromId(mode).getDisplayName(), mouseX, mouseY);
        }
    }

    private static void renderScaledItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.renderItem(stack, 0, 0);
        guiGraphics.pose().popPose();
    }

    private static void renderScaledTexture(GuiGraphics guiGraphics, ResourceLocation texture, int x, int y, int size) {
        guiGraphics.pose().pushPose();
        float scale = size / 16.0f;
        guiGraphics.pose().translate(x, y, 0);
        guiGraphics.pose().scale(scale, scale, 1.0f);
        guiGraphics.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= redstoneButtonScreenX && mouseX < redstoneButtonScreenX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneButtonScreenY && mouseY < redstoneButtonScreenY + REDSTONE_BUTTON_SIZE) {
            if (minecraft != null && minecraft.getSoundManager() != null)
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            PacketDistributor.sendToServer(new MelterRedstoneModePayload(menu.getBlockPos()));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);

        int left = leftPos + FLUID_BAR_X + FLUID_FILL_INSET;
        int top = topPos + FLUID_BAR_Y + FLUID_FILL_INSET;
        if (mouseX >= left && mouseX < left + FLUID_FILL_WIDTH && mouseY >= top && mouseY < top + FLUID_FILL_HEIGHT) {
            int amount = menu.getFluidAmount();
            int capacity = menu.getFluidCapacity();
            int fluidId = menu.getFluidId();
            Component amountLine = capacity > 0
                    ? Component.translatable("gui.colossal_reactors.melter.tank_tooltip", amount, capacity)
                    : Component.translatable("gui.colossal_reactors.melter.tank_empty");
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
    }
}
