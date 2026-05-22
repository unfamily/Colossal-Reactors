package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.menu.ResourcePortMenu;
import net.unfamily.colossal_reactors.network.FluidTankDumpPayload;
import net.unfamily.colossal_reactors.network.ResourcePortModePayload;
import net.unfamily.colossal_reactors.network.ResourcePortSettingsPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Resource port GUI: four vertical buttons (mode + solid / liquid / gas), same click/packet pattern as
 * {@link ReactorBuilderScreen} (synced block pos from {@link ResourcePortMenu#getSyncedBlockPos()}).
 */
public class ResourcePortScreen extends AbstractContainerScreen<ResourcePortMenu> {

    private static final int COLOR_MODE = 0xFFFFFF;
    private static final int COLOR_SOLID = 0xFFFF55;
    private static final int COLOR_LIQUID = 0x55FFFF;
    private static final int COLOR_GAS = 0xFF55FF;
    private static final int TOGGLE_COUNT = 4;

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/resource_port.png");

    private final boolean mekLoaded = ModList.get().isLoaded("mekanism");

    private final Button[] toggleButtons = new Button[TOGGLE_COUNT];
    private Button btnDumpLiquid;
    private Button btnDumpGas;

    private PortMode lastMode;
    private boolean lastSolid;
    private boolean lastLiquid;
    private boolean lastGas;

    public ResourcePortScreen(ResourcePortMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = ResourcePortGuiLayout.GUI_WIDTH;
        imageHeight = ResourcePortGuiLayout.GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("\u2715"), b -> closeScreen())
                .bounds(leftPos + ResourcePortGuiLayout.CLOSE_X, topPos + ResourcePortGuiLayout.CLOSE_Y,
                        ResourcePortGuiLayout.CLOSE_SIZE, ResourcePortGuiLayout.CLOSE_SIZE)
                .build());

        int bx = leftPos + ResourcePortGuiLayout.TOGGLE_X;
        int bw = ResourcePortGuiLayout.TOGGLE_BTN_W;
        int bh = ResourcePortGuiLayout.TOGGLE_BTN_H;

        for (int row = 0; row < TOGGLE_COUNT; row++) {
            final int rowIndex = row;
            toggleButtons[row] = Button.builder(toggleLabel(row), b -> onToggleClick(rowIndex))
                    .bounds(bx, topPos + ResourcePortGuiLayout.toggleY(row), bw, bh)
                    .build();
            addRenderableWidget(toggleButtons[row]);
        }
        if (!mekLoaded) {
            toggleButtons[3].visible = false;
            toggleButtons[3].active = false;
        }

        btnDumpLiquid = Button.builder(Component.literal("D"), b -> sendDump(FluidTankDumpPayload.TANK_FLUID))
                .bounds(leftPos + ResourcePortGuiLayout.LIQUID_DUMP_X, topPos + ResourcePortGuiLayout.LIQUID_DUMP_Y,
                        ResourcePortGuiLayout.DUMP_W, ResourcePortGuiLayout.DUMP_H)
                .tooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.fluid_dump.tooltip")))
                .build();
        addRenderableWidget(btnDumpLiquid);

        btnDumpGas = Button.builder(Component.literal("D"), b -> sendDump(FluidTankDumpPayload.TANK_GAS))
                .bounds(leftPos + ResourcePortGuiLayout.GAS_DUMP_X, topPos + ResourcePortGuiLayout.GAS_DUMP_Y,
                        ResourcePortGuiLayout.DUMP_W, ResourcePortGuiLayout.DUMP_H)
                .tooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.fluid_dump.tooltip")))
                .build();
        btnDumpGas.visible = mekLoaded;
        addRenderableWidget(btnDumpGas);

        lastMode = null;
        lastSolid = !menu.isAllowSolid();
        lastLiquid = !menu.isAllowLiquid();
        lastGas = !menu.isAllowGas();
        updateToggleButtonLabels();
    }

    private void onToggleClick(int row) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        playClickSound();
        switch (row) {
            case 0 -> {
                PortMode current = menu.getPortMode();
                PortMode next = current == PortMode.INSERT ? PortMode.EXTRACT
                        : current == PortMode.EXTRACT ? PortMode.EJECT
                        : PortMode.INSERT;
                PacketDistributor.sendToServer(new ResourcePortModePayload(pos, next.getId()));
            }
            case 1 -> {
                boolean next = !menu.isAllowSolid();
                PacketDistributor.sendToServer(new ResourcePortSettingsPayload(pos,
                        ResourcePortSettingsPayload.KIND_SOLID, next ? 1 : 0));
            }
            case 2 -> {
                boolean next = !menu.isAllowLiquid();
                PacketDistributor.sendToServer(new ResourcePortSettingsPayload(pos,
                        ResourcePortSettingsPayload.KIND_LIQUID, next ? 1 : 0));
            }
            case 3 -> {
                boolean next = !menu.isAllowGas();
                PacketDistributor.sendToServer(new ResourcePortSettingsPayload(pos,
                        ResourcePortSettingsPayload.KIND_GAS, next ? 1 : 0));
            }
            default -> { }
        }
    }

    private MutableComponent toggleLabel(int row) {
        return switch (row) {
            case 0 -> modeLabel(menu.getPortMode());
            case 1 -> mediumLabel("solid");
            case 2 -> mediumLabel("liquid");
            default -> mediumLabel("gas");
        };
    }

    private void updateToggleButtonLabels() {
        PortMode mode = menu.getPortMode();
        if (lastMode != mode) {
            lastMode = mode;
            applyModeLabel(toggleButtons[0], modeLabel(mode));
        }
        boolean solid = menu.isAllowSolid();
        if (lastSolid != solid) {
            lastSolid = solid;
            applyMediumLabel(toggleButtons[1], mediumLabel("solid"), solid, COLOR_SOLID);
        }
        boolean liquid = menu.isAllowLiquid();
        if (lastLiquid != liquid) {
            lastLiquid = liquid;
            applyMediumLabel(toggleButtons[2], mediumLabel("liquid"), liquid, COLOR_LIQUID);
        }
        boolean gas = menu.isAllowGas();
        if (lastGas != gas && toggleButtons[3].visible) {
            lastGas = gas;
            applyMediumLabel(toggleButtons[3], mediumLabel("gas"), gas, COLOR_GAS);
        }
    }

    private static MutableComponent modeLabel(PortMode mode) {
        return Component.translatable("gui.colossal_reactors.resource_port.mode." + mode.name().toLowerCase());
    }

    private static MutableComponent mediumLabel(String key) {
        return Component.translatable("gui.colossal_reactors.resource_port.toggle." + key);
    }

    /** Mode row: bold white, never underlined. */
    private static void applyModeLabel(Button button, MutableComponent text) {
        if (button == null) return;
        button.setMessage(text.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        button.setFGColor(COLOR_MODE);
        button.active = true;
    }

    /** Medium rows: tinted label; underlined when active. */
    private static void applyMediumLabel(Button button, MutableComponent text, boolean active, int fgColor) {
        if (button == null) return;
        MutableComponent styled = text.copy();
        if (active) {
            styled = styled.withStyle(ChatFormatting.UNDERLINE);
        }
        button.setMessage(styled);
        button.setFGColor(fgColor);
        button.active = true;
    }

    private void sendDump(byte tankType) {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            playClickSound();
            PacketDistributor.sendToServer(new FluidTankDumpPayload(pos, tankType));
        }
    }

    private void playClickSound() {
        if (minecraft != null && minecraft.getSoundManager() != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }

    private void closeScreen() {
        playClickSound();
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        g.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight,
                ResourcePortGuiLayout.GUI_WIDTH, ResourcePortGuiLayout.GUI_HEIGHT);

        if (!mekLoaded) {
            g.fill(ResourcePortGuiLayout.maskGasLeft(leftPos), ResourcePortGuiLayout.maskGasTop(topPos),
                    ResourcePortGuiLayout.maskGasRight(leftPos), ResourcePortGuiLayout.maskGasBottom(topPos),
                    ResourcePortGuiLayout.MASK_COLOR);
        } else {
            renderGasBar(g, leftPos, topPos);
        }
        renderLiquidBar(g, leftPos, topPos);
    }

    private void renderLiquidBar(GuiGraphics g, int guiX, int guiY) {
        int amount = menu.getFluidAmount();
        int capacity = menu.getFluidCapacity();
        int fluidId = menu.getFluidId();
        if (capacity <= 0 || amount <= 0 || fluidId < 0) return;
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) return;
        int fillPx = (ResourcePortGuiLayout.BAR_FILL_H * amount) / capacity;
        if (fillPx <= 0) return;
        int fillTop = ResourcePortGuiLayout.liquidBarFillBottom(guiY) - fillPx;
        FluidRenderHelper.drawFluidInTank(g, new FluidStack(fluid, amount),
                ResourcePortGuiLayout.liquidBarFillLeft(guiX), fillTop,
                ResourcePortGuiLayout.BAR_FILL_W, fillPx);
    }

    private void renderGasBar(GuiGraphics g, int guiX, int guiY) {
        int amount = menu.getGasAmount();
        int capacity = menu.getGasCapacity();
        if (capacity <= 0 || amount <= 0) return;
        int fillPx = (ResourcePortGuiLayout.BAR_FILL_H * amount) / capacity;
        if (fillPx <= 0) return;
        int outerLeft = ResourcePortGuiLayout.gasBarFillLeft(guiX);
        int outerTop = ResourcePortGuiLayout.gasBarFillTop(guiY);
        String gasName = menu.getGasRegistryName();
        if (!GasTankRenderHelper.drawGasInTank(g, gasName, amount, outerLeft, outerTop,
                ResourcePortGuiLayout.BAR_FILL_W, ResourcePortGuiLayout.BAR_FILL_H, fillPx)) {
            int fillTop = ResourcePortGuiLayout.gasBarFillBottom(guiY) - fillPx;
            g.fill(outerLeft, fillTop, outerLeft + ResourcePortGuiLayout.BAR_FILL_W,
                    ResourcePortGuiLayout.gasBarFillBottom(guiY), 0xFF88CCFF);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        int titleW = font.width(title);
        g.drawString(font, title, (imageWidth - titleW) / 2, 6, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        updateToggleButtonLabels();
        super.render(g, mouseX, mouseY, partialTick);
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics g, int mouseX, int mouseY) {
        super.renderTooltip(g, mouseX, mouseY);
        tooltipLiquid(g, mouseX, mouseY);
        if (mekLoaded) tooltipGas(g, mouseX, mouseY);
    }

    private void tooltipLiquid(GuiGraphics g, int mouseX, int mouseY) {
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

    private void tooltipGas(GuiGraphics g, int mouseX, int mouseY) {
        int left = ResourcePortGuiLayout.gasBarFillLeft(leftPos);
        int top = ResourcePortGuiLayout.gasBarFillTop(topPos);
        if (mouseX < left || mouseX >= left + ResourcePortGuiLayout.BAR_FILL_W
                || mouseY < top || mouseY >= top + ResourcePortGuiLayout.BAR_FILL_H) return;
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.colossal_reactors.resource_port.tank_tooltip.gas",
                menu.getGasAmount(), menu.getGasCapacity()).getVisualOrderText());
        String gasName = menu.getGasRegistryName();
        Component name = GasTankRenderHelper.getGasDisplayName(gasName);
        if (name != null) lines.add(name.getVisualOrderText());
        g.renderTooltip(font, lines, mouseX, mouseY);
    }
}
