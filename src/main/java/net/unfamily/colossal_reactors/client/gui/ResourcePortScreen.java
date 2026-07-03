package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.menu.ResourcePortMenu;
import net.unfamily.colossal_reactors.network.FluidTankDumpPayload;
import net.unfamily.colossal_reactors.network.ResourcePortFilterPayload;
import net.unfamily.colossal_reactors.network.ResourcePortModePayload;
import net.unfamily.colossal_reactors.network.ResourcePortSettingsPayload;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Resource port GUI: four vertical buttons (mode + solid / liquid / gas), same click/packet pattern as
 * {@link ReactorBuilderScreen} ({@link ResourcePortMenu#getSyncedBlockPos()}).
 */
public class ResourcePortScreen extends AbstractContainerScreen<ResourcePortMenu> {

    private static final int COLOR_MODE = 0xFFFFFF;
    private static final int COLOR_SOLID = 0xFFFF55;
    private static final int COLOR_LIQUID = 0x55FFFF;
    private static final int COLOR_GAS = 0xFF55FF;
    private static final int TOGGLE_COUNT = 4;

    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/resource_port.png");

    private final boolean mekLoaded = ModList.get().isLoaded("mekanism");

    private final Button[] toggleButtons = new Button[TOGGLE_COUNT];
    private Button btnDumpLiquid;
    private Button btnDumpGas;
    @Nullable
    private Button btnFilter;

    private PortMode lastMode;
    private PortFilter lastFilter;
    private boolean lastSolid;
    private boolean lastLiquid;
    private boolean lastGas;

    public ResourcePortScreen(ResourcePortMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title, ResourcePortGuiLayout.GUI_WIDTH, ResourcePortGuiLayout.GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();

        addRenderableWidget(Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.closeContainer();
            }
        }).bounds(leftPos + ResourcePortGuiLayout.CLOSE_X, topPos + ResourcePortGuiLayout.CLOSE_Y,
                ResourcePortGuiLayout.CLOSE_SIZE, ResourcePortGuiLayout.CLOSE_SIZE).build());

        int bx = leftPos + ResourcePortGuiLayout.TOGGLE_X;
        int bw = ResourcePortGuiLayout.TOGGLE_BTN_W;
        int bh = ResourcePortGuiLayout.TOGGLE_BTN_H;

        boolean turbinePort = menu.isTurbinePort();
        for (int row = 0; row < TOGGLE_COUNT; row++) {
            final int rowIndex = row;
            toggleButtons[row] = Button.builder(toggleLabel(row), b -> onToggleClick(rowIndex))
                    .bounds(bx, topPos + ResourcePortGuiLayout.mediumToggleY(row, turbinePort), bw, bh)
                    .build();
            addRenderableWidget(toggleButtons[row]);
        }
        applyTurbineLayout();
        applyGasToggleVisibility();

        btnDumpLiquid = Button.builder(Component.literal("D"), b -> sendDump(FluidTankDumpPayload.TANK_FLUID))
                .bounds(leftPos + ResourcePortGuiLayout.LIQUID_DUMP_X, topPos + ResourcePortGuiLayout.LIQUID_DUMP_Y,
                        ResourcePortGuiLayout.DUMP_W, ResourcePortGuiLayout.DUMP_H)
                .build();
        btnDumpLiquid.setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.fluid_dump.tooltip")));
        addRenderableWidget(btnDumpLiquid);

        btnDumpGas = Button.builder(Component.literal("D"), b -> sendDump(FluidTankDumpPayload.TANK_GAS))
                .bounds(leftPos + ResourcePortGuiLayout.GAS_DUMP_X, topPos + ResourcePortGuiLayout.GAS_DUMP_Y,
                        ResourcePortGuiLayout.DUMP_W, ResourcePortGuiLayout.DUMP_H)
                .build();
        btnDumpGas.setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.gas_dump.tooltip")));
        btnDumpGas.visible = mekLoaded;
        addRenderableWidget(btnDumpGas);

        if (!menu.isTurbinePort()) {
            btnFilter = Button.builder(filterLabel(menu.getPortFilter(), menu.getPortMode()), b -> onFilterClick())
                    .bounds(leftPos + ResourcePortGuiLayout.FILTER_X, topPos + ResourcePortGuiLayout.FILTER_Y,
                            ResourcePortGuiLayout.FILTER_BTN_W, ResourcePortGuiLayout.FILTER_BTN_H)
                    .build();
            addRenderableWidget(btnFilter);
        }

        lastMode = null;
        lastFilter = null;
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
        switch (row) {
            case 0 -> {
                PortMode current = menu.getPortMode();
                PortMode next = current == PortMode.INSERT ? PortMode.EXTRACT
                        : current == PortMode.EXTRACT ? PortMode.EJECT
                        : PortMode.INSERT;
                ClientPacketDistributor.sendToServer(new ResourcePortModePayload(pos, next.getId()));
            }
            case 1 -> {
                if (menu.isTurbinePort()) {
                    return;
                }
                boolean next = !menu.isAllowSolid();
                ClientPacketDistributor.sendToServer(new ResourcePortSettingsPayload(pos,
                        ResourcePortSettingsPayload.KIND_SOLID, next ? 1 : 0));
            }
            case 2 -> {
                boolean next = !menu.isAllowLiquid();
                ClientPacketDistributor.sendToServer(new ResourcePortSettingsPayload(pos,
                        ResourcePortSettingsPayload.KIND_LIQUID, next ? 1 : 0));
            }
            case 3 -> {
                boolean next = !menu.isAllowGas();
                ClientPacketDistributor.sendToServer(new ResourcePortSettingsPayload(pos,
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

    private void onFilterClick() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PortFilter next = menu.getPortFilter() == PortFilter.ONLY_COOLANT_LIQUID
                ? PortFilter.ONLY_SOLID_FUEL
                : PortFilter.ONLY_COOLANT_LIQUID;
        ClientPacketDistributor.sendToServer(new ResourcePortFilterPayload(pos, next.getId()));
    }

    private static Component filterLabel(PortFilter filter, PortMode mode) {
        return filter.getFilterButtonLabel(mode);
    }

    private void applyTurbineLayout() {
        if (!menu.isTurbinePort()) {
            return;
        }
        int bx = leftPos + ResourcePortGuiLayout.TOGGLE_X;
        int bw = ResourcePortGuiLayout.TOGGLE_BTN_W;
        int bh = ResourcePortGuiLayout.TOGGLE_BTN_H;

        // Turbine: no solid items — block row 1 (Solid) entirely.
        toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_SOLID].visible = false;
        toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_SOLID].active = false;

        // Liquid and Gas shift up one row into the former Solid / Liquid slots.
        toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_LIQUID].setPosition(
                bx, topPos + ResourcePortGuiLayout.mediumToggleY(ResourcePortGuiLayout.TOGGLE_ROW_LIQUID, true));
        toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_LIQUID].setWidth(bw);
        toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_LIQUID].setHeight(bh);
        toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_GAS].setPosition(
                bx, topPos + ResourcePortGuiLayout.mediumToggleY(ResourcePortGuiLayout.TOGGLE_ROW_GAS, true));
        toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_GAS].setWidth(bw);
        toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_GAS].setHeight(bh);

        if (btnFilter != null) {
            btnFilter.visible = false;
            btnFilter.active = false;
        }
        applyGasToggleVisibility();
    }

    /** Gas medium toggle is always shown; the gas tank frame is masked when Mek is absent. */
    private void applyGasToggleVisibility() {
        Button gas = toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_GAS];
        if (gas == null) return;
        gas.visible = true;
        gas.active = true;
    }

    private void updateToggleButtonLabels() {
        applyTurbineLayout();
        applyGasToggleVisibility();
        PortMode mode = menu.getPortMode();
        PortFilter filter = menu.getPortFilter();
        boolean modeChanged = lastMode != mode;
        if (modeChanged) {
            lastMode = mode;
            applyModeLabel(toggleButtons[0], modeLabel(mode));
        }
        if (btnFilter != null && (lastFilter != filter || modeChanged)) {
            lastFilter = filter;
            btnFilter.setMessage(filterLabel(filter, mode));
        }
        if (!menu.isTurbinePort()) {
            boolean solid = menu.isAllowSolid();
            if (lastSolid != solid) {
                lastSolid = solid;
                applyMediumLabel(toggleButtons[1], mediumLabel("solid"), solid, COLOR_SOLID);
            }
        }
        boolean liquid = menu.isAllowLiquid();
        if (lastLiquid != liquid) {
            lastLiquid = liquid;
            applyMediumLabel(toggleButtons[2], mediumLabel("liquid"), liquid, COLOR_LIQUID);
        }
        boolean gas = menu.isAllowGas();
        if (lastGas != gas) {
            lastGas = gas;
            applyMediumLabel(toggleButtons[ResourcePortGuiLayout.TOGGLE_ROW_GAS], mediumLabel("gas"), gas, COLOR_GAS);
        }
    }

    private static MutableComponent modeLabel(PortMode mode) {
        return Component.translatable("gui.colossal_reactors.resource_port.mode." + mode.name().toLowerCase());
    }

    private static MutableComponent mediumLabel(String key) {
        return Component.translatable("gui.colossal_reactors.resource_port.toggle." + key);
    }

    private static void applyModeLabel(Button button, MutableComponent text) {
        if (button == null) return;
        button.setMessage(text.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        button.setFGColor(COLOR_MODE);
        button.active = true;
    }

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
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        if (tankType == FluidTankDumpPayload.TANK_GAS && menu.isGasDumpBlockedByRadioactivity()) {
            return;
        }
        ClientPacketDistributor.sendToServer(new FluidTankDumpPayload(pos, tankType));
    }

    /** Only gas dump is gated: disabled when the tank holds radioactive Mek gas. Liquid dump is unchanged. */
    private void updateDumpButtons() {
        if (btnDumpGas == null) {
            return;
        }
        boolean radioactive = menu.isGasDumpBlockedByRadioactivity();
        btnDumpGas.active = !radioactive;
        btnDumpGas.setTooltip(Tooltip.create(radioactive
                ? Component.translatable("gui.colossal_reactors.gas_dump.tooltip.radioactive")
                : Component.translatable("gui.colossal_reactors.gas_dump.tooltip")));
    }

    @Override
    public void extractBackground(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                  float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos, 0.0F, 0.0F, imageWidth, imageHeight,
                ResourcePortGuiLayout.GUI_WIDTH, ResourcePortGuiLayout.GUI_HEIGHT);

        if (!mekLoaded) {
            guiGraphics.fill(ResourcePortGuiLayout.maskGasLeft(leftPos), ResourcePortGuiLayout.maskGasTop(topPos),
                    ResourcePortGuiLayout.maskGasRight(leftPos), ResourcePortGuiLayout.maskGasBottom(topPos),
                    ResourcePortGuiLayout.MASK_COLOR);
        } else {
            renderGasBar(guiGraphics, leftPos, topPos);
        }

        long amount = menu.getFluidAmountLong();
        long capacity = menu.getFluidCapacityLong();
        int fluidId = menu.getFluidId();
        if (capacity > 0 && amount > 0 && fluidId >= 0) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != null && fluid != Fluids.EMPTY) {
                int fillPx = ResourcePortGuiLayout.barFillPixels(amount, capacity, ResourcePortGuiLayout.BAR_FILL_H);
                if (fillPx > 0) {
                    int barLeft = ResourcePortGuiLayout.liquidBarFillLeft(leftPos);
                    int barBottom = ResourcePortGuiLayout.liquidBarFillBottom(topPos);
                    FluidRenderHelper.drawFluidInTank(guiGraphics, new FluidStack(fluid, (int) Math.min(amount, Integer.MAX_VALUE)),
                            barLeft, barBottom - fillPx, ResourcePortGuiLayout.BAR_FILL_W, fillPx);
                }
            }
        }
        if (!menu.showItemSlot()) {
            ResourcePortGuiLayout.fillItemSlotMask(guiGraphics, leftPos, topPos);
        }
    }

    @Override
    protected void extractLabels(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int titleW = font.width(title);
        guiGraphics.text(font, title, (imageWidth - titleW) / 2, 6, GuiTextColors.TITLE, false);
    }

    @Override
    public void extractRenderState(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY,
                                   float partialTick) {
        updateToggleButtonLabels();
        updateDumpButtons();
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void renderGasBar(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int guiX, int guiY) {
        long amount = menu.getGasAmountLong();
        long capacity = menu.getGasCapacityLong();
        if (capacity <= 0 || amount <= 0) {
            return;
        }
        int fillPx = ResourcePortGuiLayout.barFillPixels(amount, capacity, ResourcePortGuiLayout.BAR_FILL_H);
        if (fillPx <= 0) {
            return;
        }
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
    protected void extractTooltip(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);
        if (mekLoaded) {
            tooltipGas(guiGraphics, mouseX, mouseY);
        }
        int left = ResourcePortGuiLayout.liquidBarFillLeft(leftPos);
        int top = ResourcePortGuiLayout.liquidBarFillTop(topPos);
        if (mouseX >= left && mouseX < left + ResourcePortGuiLayout.BAR_FILL_W
                && mouseY >= top && mouseY < top + ResourcePortGuiLayout.BAR_FILL_H) {
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
    }

    private void tooltipGas(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        int left = ResourcePortGuiLayout.gasBarFillLeft(leftPos);
        int top = ResourcePortGuiLayout.gasBarFillTop(topPos);
        if (mouseX < left || mouseX >= left + ResourcePortGuiLayout.BAR_FILL_W
                || mouseY < top || mouseY >= top + ResourcePortGuiLayout.BAR_FILL_H) {
            return;
        }
        List<FormattedCharSequence> lines = new ArrayList<>();
        lines.add(Component.translatable("gui.colossal_reactors.resource_port.tank_tooltip.gas",
                menu.getGasAmount(), menu.getGasCapacity()).getVisualOrderText());
        String gasName = menu.getGasRegistryName();
        Component name = GasTankRenderHelper.getGasDisplayName(gasName);
        if (name != null) {
            lines.add(name.getVisualOrderText());
        }
        guiGraphics.setTooltipForNextFrame(font, lines, mouseX, mouseY);
    }
}
