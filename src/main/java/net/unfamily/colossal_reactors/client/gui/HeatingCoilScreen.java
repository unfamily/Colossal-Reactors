package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.RedstoneMode;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;
import net.unfamily.colossal_reactors.menu.HeatingCoilMenu;
import net.unfamily.colossal_reactors.network.FluidTankDumpPayload;
import net.unfamily.colossal_reactors.network.HeatingCoilRedstoneModePayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Heating coil GUI: same layout as resource port (gas/liquid bars, item slot) without mode toggles.
 * Gas area masked when Mek is absent or coil has no chemical consume option.
 */
public class HeatingCoilScreen extends AbstractContainerScreen<HeatingCoilMenu> {

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/resource_port.png");
    private static final Identifier ENERGY_BAR =
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/energy_bar.png");
    private static final Identifier MEDIUM_BUTTONS = Identifier.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/medium_buttons.png");
    private static final Identifier REDSTONE_GUI = Identifier.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/redstone_gui.png");

    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    private static final int ENERGY_BAR_X = ResourcePortGuiLayout.GUI_WIDTH - ENERGY_BAR_WIDTH - 8;
    private static final int ENERGY_BAR_Y = ResourcePortGuiLayout.LIQUID_BAR_Y
            + (ResourcePortGuiLayout.BAR_FILL_H - ENERGY_BAR_HEIGHT) / 2;

    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int REDSTONE_BUTTON_X = ResourcePortGuiLayout.CLOSE_X - REDSTONE_BUTTON_SIZE - 4;
    private static final int REDSTONE_BUTTON_Y = ResourcePortGuiLayout.ITEM_SLOT_Y
            + (ResourcePortGuiLayout.ITEM_SLOT_SIZE - REDSTONE_BUTTON_SIZE) / 2;

    private final boolean mekLoaded = MekChemicalHelper.isGasSupportEnabled();

    private Button closeButton;
    private Button btnDumpLiquid;
    private Button btnDumpGas;
    private int redstoneButtonScreenX;
    private int redstoneButtonScreenY;

    public HeatingCoilScreen(HeatingCoilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, ResourcePortGuiLayout.GUI_WIDTH, ResourcePortGuiLayout.GUI_HEIGHT);
    }

    private boolean showGasUi() {
        return mekLoaded && menu.showChemicalInGui();
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        }).bounds(leftPos + ResourcePortGuiLayout.CLOSE_X, topPos + ResourcePortGuiLayout.CLOSE_Y,
                ResourcePortGuiLayout.CLOSE_SIZE, ResourcePortGuiLayout.CLOSE_SIZE).build();
        addRenderableWidget(closeButton);

        btnDumpLiquid = Button.builder(Component.literal("D"), b -> {
            BlockPos pos = menu.getBlockPos();
            if (pos != null) {
                ClientPacketDistributor.sendToServer(new FluidTankDumpPayload(pos));
            }
        }).bounds(leftPos + ResourcePortGuiLayout.LIQUID_DUMP_X, topPos + ResourcePortGuiLayout.LIQUID_DUMP_Y,
                ResourcePortGuiLayout.DUMP_W, ResourcePortGuiLayout.DUMP_H)
                .build();
        btnDumpLiquid.setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.fluid_dump.tooltip")));
        addRenderableWidget(btnDumpLiquid);

        btnDumpGas = Button.builder(Component.literal("D"), b -> sendDump(FluidTankDumpPayload.TANK_GAS))
                .bounds(leftPos + ResourcePortGuiLayout.GAS_DUMP_X, topPos + ResourcePortGuiLayout.GAS_DUMP_Y,
                        ResourcePortGuiLayout.DUMP_W, ResourcePortGuiLayout.DUMP_H)
                .build();
        btnDumpGas.setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.gas_dump.tooltip")));
        addRenderableWidget(btnDumpGas);

        redstoneButtonScreenX = leftPos + REDSTONE_BUTTON_X;
        redstoneButtonScreenY = topPos + REDSTONE_BUTTON_Y;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        int x = leftPos;
        int y = topPos;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0.0F, 0.0F, imageWidth, imageHeight,
                ResourcePortGuiLayout.GUI_WIDTH, ResourcePortGuiLayout.GUI_HEIGHT);

        if (showGasUi()) {
            renderGasBar(guiGraphics, x, y);
        } else {
            guiGraphics.fill(ResourcePortGuiLayout.maskGasLeft(x), ResourcePortGuiLayout.maskGasTop(y),
                    ResourcePortGuiLayout.maskGasRight(x), ResourcePortGuiLayout.maskGasBottom(y),
                    ResourcePortGuiLayout.MASK_COLOR);
        }

        if (menu.showFluidInGui()) {
            int amount = menu.getFluidAmount();
            int capacity = menu.getFluidCapacity();
            int fluidId = menu.getFluidId();
            if (capacity > 0 && amount > 0 && fluidId >= 0) {
                Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    int fillPx = ResourcePortGuiLayout.barFillPixels(amount, capacity, ResourcePortGuiLayout.BAR_FILL_H);
                    if (fillPx > 0) {
                        FluidRenderHelper.drawFluidInTank(guiGraphics, new FluidStack(fluid, amount),
                                ResourcePortGuiLayout.liquidBarFillLeft(x),
                                ResourcePortGuiLayout.liquidBarFillBottom(y) - fillPx,
                                ResourcePortGuiLayout.BAR_FILL_W, fillPx);
                    }
                }
            }
        } else {
            guiGraphics.fill(x + ResourcePortGuiLayout.LIQUID_BAR_X - ResourcePortGuiLayout.MASK_INSET,
                    y + ResourcePortGuiLayout.LIQUID_BAR_Y - ResourcePortGuiLayout.MASK_INSET,
                    x + ResourcePortGuiLayout.LIQUID_BAR_X + ResourcePortGuiLayout.BAR_FILL_W + ResourcePortGuiLayout.MASK_INSET,
                    y + ResourcePortGuiLayout.LIQUID_BAR_Y + ResourcePortGuiLayout.BAR_FILL_H + ResourcePortGuiLayout.MASK_INSET,
                    ResourcePortGuiLayout.MASK_COLOR);
        }

        if (!menu.showItemInGui()) {
            int sx = x + ResourcePortGuiLayout.ITEM_SLOT_X - ResourcePortGuiLayout.MASK_INSET;
            int sy = y + ResourcePortGuiLayout.ITEM_SLOT_Y - ResourcePortGuiLayout.MASK_INSET;
            guiGraphics.fill(sx, sy, sx + ResourcePortGuiLayout.ITEM_SLOT_SIZE + 2 * ResourcePortGuiLayout.MASK_INSET,
                    sy + ResourcePortGuiLayout.ITEM_SLOT_SIZE + 2 * ResourcePortGuiLayout.MASK_INSET,
                    ResourcePortGuiLayout.MASK_COLOR);
        }

        if (menu.showEnergyInGui()) {
            int energyBarX = x + ENERGY_BAR_X;
            int energyBarY = y + ENERGY_BAR_Y;
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, energyBarY, 8.0F, 0.0F,
                    ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 16, 32);
            int energy = menu.getEnergy();
            int maxEnergy = menu.getEnergyCapacity();
            if (energy > 0 && maxEnergy > 0) {
                int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
                int energyY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ENERGY_BAR, energyBarX, energyY, 0.0F,
                        (float) (ENERGY_BAR_HEIGHT - energyHeight), ENERGY_BAR_WIDTH, energyHeight,
                        ENERGY_BAR_WIDTH, energyHeight, 16, 32);
            }
        }
    }

    private void renderGasBar(GuiGraphicsExtractor guiGraphics, int guiX, int guiY) {
        long amount = menu.getGasAmountLong();
        long capacity = menu.getGasCapacityLong();
        if (capacity <= 0) return;
        int fillPx = ResourcePortGuiLayout.barFillPixels(amount, capacity, ResourcePortGuiLayout.BAR_FILL_H);
        if (fillPx <= 0) return;
        int outerLeft = ResourcePortGuiLayout.gasBarFillLeft(guiX);
        int fillTop = ResourcePortGuiLayout.gasBarFillBottom(guiY) - fillPx;
        GasTankRenderHelper.GasRenderInfo info =
                GasTankRenderHelper.getGasRenderInfoFromRegistryName(menu.getGasRegistryName());
        if (info != null && !info.isEmpty()) {
            GasTankRenderHelper.drawGasInTank(guiGraphics, info, outerLeft, fillTop,
                    ResourcePortGuiLayout.BAR_FILL_W, fillPx);
        } else {
            guiGraphics.fill(outerLeft, fillTop, outerLeft + ResourcePortGuiLayout.BAR_FILL_W,
                    ResourcePortGuiLayout.gasBarFillBottom(guiY), 0xFF88CCFF);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        guiGraphics.text(font, title, (imageWidth - titleW) / 2, 6, GuiTextColors.TITLE, false);
    }

    private void sendDump(byte tankType) {
        BlockPos pos = menu.getBlockPos();
        if (pos == null) {
            return;
        }
        if (tankType == FluidTankDumpPayload.TANK_GAS && menu.isGasDumpBlockedByRadioactivity()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new FluidTankDumpPayload(pos, tankType));
    }

    private void updateDumpButtons() {
        if (btnDumpGas != null) {
            boolean radioactive = menu.isGasDumpBlockedByRadioactivity();
            btnDumpGas.active = !radioactive;
            btnDumpGas.setTooltip(Tooltip.create(radioactive
                    ? Component.translatable("gui.colossal_reactors.gas_dump.tooltip.radioactive")
                    : Component.translatable("gui.colossal_reactors.gas_dump.tooltip")));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (btnDumpLiquid != null) {
            btnDumpLiquid.visible = menu.showFluidInGui();
        }
        if (btnDumpGas != null) {
            btnDumpGas.visible = showGasUi();
        }
        updateDumpButtons();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        renderRedstoneButton(guiGraphics, mouseX, mouseY);
    }

    private void renderRedstoneButton(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        boolean hovered = mouseX >= redstoneButtonScreenX && mouseX < redstoneButtonScreenX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneButtonScreenY && mouseY < redstoneButtonScreenY + REDSTONE_BUTTON_SIZE;
        int textureY = hovered ? 16 : 0;
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, MEDIUM_BUTTONS, redstoneButtonScreenX, redstoneButtonScreenY,
                0.0F, (float) textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE,
                REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 96, 96);
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
        if (hovered) {
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
        if ((event.button() == 0 || event.button() == 1) && menu.getBlockPos() != null
                && event.x() >= redstoneButtonScreenX && event.x() < redstoneButtonScreenX + REDSTONE_BUTTON_SIZE
                && event.y() >= redstoneButtonScreenY && event.y() < redstoneButtonScreenY + REDSTONE_BUTTON_SIZE) {
            if (minecraft != null && minecraft.getSoundManager() != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            ClientPacketDistributor.sendToServer(new HeatingCoilRedstoneModePayload(menu.getBlockPos(), event.button() == 0));
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);
        tooltipLiquid(guiGraphics, mouseX, mouseY);
        if (showGasUi()) tooltipGas(guiGraphics, mouseX, mouseY);
        if (menu.showEnergyInGui()) {
            int ex = leftPos + ENERGY_BAR_X;
            int ey = topPos + ENERGY_BAR_Y;
            if (mouseX >= ex && mouseX < ex + ENERGY_BAR_WIDTH && mouseY >= ey && mouseY < ey + ENERGY_BAR_HEIGHT) {
                Component line = Component.translatable("gui.colossal_reactors.heating_coil.energy_tooltip",
                        menu.getEnergy(), menu.getEnergyCapacity());
                guiGraphics.setTooltipForNextFrame(font, List.of(line.getVisualOrderText()), mouseX, mouseY);
            }
        }
    }

    private void tooltipLiquid(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (!menu.showFluidInGui()) return;
        int left = ResourcePortGuiLayout.liquidBarFillLeft(leftPos);
        int top = ResourcePortGuiLayout.liquidBarFillTop(topPos);
        if (mouseX < left || mouseX >= left + ResourcePortGuiLayout.BAR_FILL_W
                || mouseY < top || mouseY >= top + ResourcePortGuiLayout.BAR_FILL_H) return;
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.colossal_reactors.resource_port.tank_tooltip.liquid",
                menu.getFluidAmount(), menu.getFluidCapacity()).getVisualOrderText());
        int fluidId = menu.getFluidId();
        if (fluidId >= 0) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != null && fluid != Fluids.EMPTY) {
                FluidType type = fluid.getFluidType();
                lines.add(Component.translatable(type.getDescriptionId()).getVisualOrderText());
            }
        }
        guiGraphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
    }

    private void tooltipGas(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int left = ResourcePortGuiLayout.gasBarFillLeft(leftPos);
        int top = ResourcePortGuiLayout.gasBarFillTop(topPos);
        if (mouseX < left || mouseX >= left + ResourcePortGuiLayout.BAR_FILL_W
                || mouseY < top || mouseY >= top + ResourcePortGuiLayout.BAR_FILL_H) return;
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.colossal_reactors.resource_port.tank_tooltip.gas",
                GuiNumberFormat.format(menu.getGasAmountLong()),
                GuiNumberFormat.format(menu.getGasCapacityLong())).getVisualOrderText());
        String gasName = menu.getGasRegistryName();
        Component name = GasTankRenderHelper.getGasDisplayName(gasName);
        if (name != null) lines.add(name.getVisualOrderText());
        guiGraphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
    }
}
