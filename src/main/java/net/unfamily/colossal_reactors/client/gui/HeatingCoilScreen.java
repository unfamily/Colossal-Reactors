package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.RedstoneMode;
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

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/resource_port.png");
    private static final ResourceLocation ENERGY_BAR =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/energy_bar.png");
    private static final ResourceLocation MEDIUM_BUTTONS = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/medium_buttons.png");
    private static final ResourceLocation REDSTONE_GUI = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/redstone_gui.png");

    private static final int ENERGY_BAR_WIDTH = 8;
    private static final int ENERGY_BAR_HEIGHT = 32;
    private static final int ENERGY_BAR_X = ResourcePortGuiLayout.GUI_WIDTH - ENERGY_BAR_WIDTH - 8;
    private static final int ENERGY_BAR_Y = ResourcePortGuiLayout.LIQUID_BAR_Y
            + (ResourcePortGuiLayout.BAR_FILL_H - ENERGY_BAR_HEIGHT) / 2;

    private static final int REDSTONE_BUTTON_SIZE = 16;
    private static final int REDSTONE_BUTTON_X = ResourcePortGuiLayout.CLOSE_X - REDSTONE_BUTTON_SIZE - 4;
    private static final int REDSTONE_BUTTON_Y = ResourcePortGuiLayout.ITEM_SLOT_Y
            + (18 - REDSTONE_BUTTON_SIZE) / 2;

    private static final int SLOT_MASK_SIZE = 18;

    private final boolean mekLoaded = ModList.get().isLoaded("mekanism");

    private Button closeButton;
    private Button btnDumpLiquid;
    private int redstoneButtonScreenX;
    private int redstoneButtonScreenY;

    public HeatingCoilScreen(HeatingCoilMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = ResourcePortGuiLayout.GUI_WIDTH;
        imageHeight = ResourcePortGuiLayout.GUI_HEIGHT;
    }

    private boolean showGasBar() {
        return mekLoaded && menu.showChemicalInGui();
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.getSoundManager() != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        }).bounds(leftPos + ResourcePortGuiLayout.CLOSE_X, topPos + ResourcePortGuiLayout.CLOSE_Y,
                ResourcePortGuiLayout.CLOSE_SIZE, ResourcePortGuiLayout.CLOSE_SIZE).build();
        addRenderableWidget(closeButton);

        btnDumpLiquid = Button.builder(Component.literal("D"), b -> {
            if (menu.getBlockPos() != null) {
                PacketDistributor.sendToServer(new FluidTankDumpPayload(menu.getBlockPos()));
            }
        }).bounds(leftPos + ResourcePortGuiLayout.LIQUID_DUMP_X, topPos + ResourcePortGuiLayout.LIQUID_DUMP_Y,
                ResourcePortGuiLayout.DUMP_W, ResourcePortGuiLayout.DUMP_H)
                .tooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.fluid_dump.tooltip")))
                .build();
        addRenderableWidget(btnDumpLiquid);

        redstoneButtonScreenX = leftPos + REDSTONE_BUTTON_X;
        redstoneButtonScreenY = topPos + REDSTONE_BUTTON_Y;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos;
        int y = topPos;
        g.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight,
                ResourcePortGuiLayout.GUI_WIDTH, ResourcePortGuiLayout.GUI_HEIGHT);

        if (showGasBar()) {
            // Gas tank rendering when coil has chemical + Mek (BE tank sync can be added later)
        } else {
            g.fill(ResourcePortGuiLayout.maskGasLeft(x), ResourcePortGuiLayout.maskGasTop(y),
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
                    int fillPx = (ResourcePortGuiLayout.BAR_FILL_H * amount) / capacity;
                    if (fillPx > 0) {
                        FluidRenderHelper.drawFluidInTank(g, new FluidStack(fluid, amount),
                                ResourcePortGuiLayout.liquidBarFillLeft(x),
                                ResourcePortGuiLayout.liquidBarFillBottom(y) - fillPx,
                                ResourcePortGuiLayout.BAR_FILL_W, fillPx);
                    }
                }
            }
        } else {
            g.fill(x + ResourcePortGuiLayout.LIQUID_BAR_X - ResourcePortGuiLayout.MASK_INSET,
                    y + ResourcePortGuiLayout.LIQUID_BAR_Y - ResourcePortGuiLayout.MASK_INSET,
                    x + ResourcePortGuiLayout.LIQUID_BAR_X + ResourcePortGuiLayout.BAR_FILL_W + ResourcePortGuiLayout.MASK_INSET,
                    y + ResourcePortGuiLayout.LIQUID_BAR_Y + ResourcePortGuiLayout.BAR_FILL_H + ResourcePortGuiLayout.MASK_INSET,
                    ResourcePortGuiLayout.MASK_COLOR);
        }

        if (!menu.showItemInGui()) {
            int sx = x + ResourcePortGuiLayout.ITEM_SLOT_X - ResourcePortGuiLayout.MASK_INSET;
            int sy = y + ResourcePortGuiLayout.ITEM_SLOT_Y - ResourcePortGuiLayout.MASK_INSET;
            g.fill(sx, sy, sx + SLOT_MASK_SIZE + 2 * ResourcePortGuiLayout.MASK_INSET,
                    sy + SLOT_MASK_SIZE + 2 * ResourcePortGuiLayout.MASK_INSET, ResourcePortGuiLayout.MASK_COLOR);
        }

        if (menu.showEnergyInGui()) {
            int energyBarX = x + ENERGY_BAR_X;
            int energyBarY = y + ENERGY_BAR_Y;
            g.blit(ENERGY_BAR, energyBarX, energyBarY, 8, 0, ENERGY_BAR_WIDTH, ENERGY_BAR_HEIGHT, 16, 32);
            int energy = menu.getEnergy();
            int maxEnergy = menu.getEnergyCapacity();
            if (energy > 0 && maxEnergy > 0) {
                int energyHeight = (energy * ENERGY_BAR_HEIGHT) / maxEnergy;
                int energyY = energyBarY + (ENERGY_BAR_HEIGHT - energyHeight);
                g.blit(ENERGY_BAR, energyBarX, energyY, 0, ENERGY_BAR_HEIGHT - energyHeight,
                        ENERGY_BAR_WIDTH, energyHeight, 16, 32);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int titleW = font.width(title);
        g.drawString(font, title, (imageWidth - titleW) / 2, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        if (btnDumpLiquid != null) {
            btnDumpLiquid.visible = menu.showFluidInGui();
        }
        super.render(g, mouseX, mouseY, partialTick);
        renderRedstoneButton(g, mouseX, mouseY);
        this.renderTooltip(g, mouseX, mouseY);
    }

    private void renderRedstoneButton(GuiGraphics g, int mouseX, int mouseY) {
        boolean hovered = mouseX >= redstoneButtonScreenX && mouseX < redstoneButtonScreenX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneButtonScreenY && mouseY < redstoneButtonScreenY + REDSTONE_BUTTON_SIZE;
        int textureY = hovered ? 16 : 0;
        g.blit(MEDIUM_BUTTONS, redstoneButtonScreenX, redstoneButtonScreenY,
                0, textureY, REDSTONE_BUTTON_SIZE, REDSTONE_BUTTON_SIZE, 96, 96);
        int iconX = redstoneButtonScreenX + 2;
        int iconY = redstoneButtonScreenY + 2;
        int iconSize = 12;
        int mode = menu.getRedstoneMode();
        switch (mode) {
            case 0 -> renderScaledItem(g, new ItemStack(Items.GUNPOWDER), iconX, iconY, iconSize);
            case 1 -> renderScaledItem(g, new ItemStack(Items.REDSTONE), iconX, iconY, iconSize);
            case 2 -> renderScaledTexture(g, REDSTONE_GUI, iconX, iconY, iconSize);
            case 3 -> renderScaledItem(g, new ItemStack(Items.REPEATER), iconX, iconY, iconSize);
            case 4 -> renderScaledItem(g, new ItemStack(Items.BARRIER), iconX, iconY, iconSize);
            default -> renderScaledItem(g, new ItemStack(Items.REDSTONE), iconX, iconY, iconSize);
        }
        if (hovered) {
            g.renderTooltip(font, RedstoneMode.fromId(mode).getDisplayName(), mouseX, mouseY);
        }
    }

    private static void renderScaledItem(GuiGraphics g, ItemStack stack, int x, int y, int size) {
        g.pose().pushPose();
        float scale = size / 16.0f;
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.renderItem(stack, 0, 0);
        g.pose().popPose();
    }

    private static void renderScaledTexture(GuiGraphics g, ResourceLocation texture, int x, int y, int size) {
        g.pose().pushPose();
        float scale = size / 16.0f;
        g.pose().translate(x, y, 0);
        g.pose().scale(scale, scale, 1.0f);
        g.blit(texture, 0, 0, 0, 0, 16, 16, 16, 16);
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if ((button == 0 || button == 1) && menu.getBlockPos() != null
                && mouseX >= redstoneButtonScreenX && mouseX < redstoneButtonScreenX + REDSTONE_BUTTON_SIZE
                && mouseY >= redstoneButtonScreenY && mouseY < redstoneButtonScreenY + REDSTONE_BUTTON_SIZE) {
            if (minecraft != null && minecraft.getSoundManager() != null) {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            PacketDistributor.sendToServer(new HeatingCoilRedstoneModePayload(menu.getBlockPos(), button == 0));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void tooltipLiquid(GuiGraphics g, int mouseX, int mouseY) {
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
                lines.add(Component.translatable(fluid.getFluidType().getDescriptionId()).getVisualOrderText());
            }
        }
        g.renderTooltip(font, lines, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        tooltipLiquid(g, mouseX, mouseY);
        if (menu.showEnergyInGui()) {
            int ex = leftPos + ENERGY_BAR_X;
            int ey = topPos + ENERGY_BAR_Y;
            if (mouseX >= ex && mouseX < ex + ENERGY_BAR_WIDTH && mouseY >= ey && mouseY < ey + ENERGY_BAR_HEIGHT) {
                Component line = Component.translatable("gui.colossal_reactors.heating_coil.energy_tooltip",
                        menu.getEnergy(), menu.getEnergyCapacity());
                g.renderTooltip(font, List.of(line.getVisualOrderText()), mouseX, mouseY);
            }
        }
    }
}
