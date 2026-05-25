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
        boolean turbinePort = menu.isTurbinePort();
        if (turbinePort) {
            toggleButtons[1].visible = false;
            toggleButtons[1].active = false;
        }

        btnDumpLiquid = Button.builder(Component.literal("D"), b -> sendDump())
                .bounds(leftPos + ResourcePortGuiLayout.LIQUID_DUMP_X, topPos + ResourcePortGuiLayout.LIQUID_DUMP_Y,
                        ResourcePortGuiLayout.DUMP_W, ResourcePortGuiLayout.DUMP_H)
                .build();
        btnDumpLiquid.setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.fluid_dump.tooltip")));
        addRenderableWidget(btnDumpLiquid);

        if (!menu.isTurbinePort()) {
            btnFilter = Button.builder(filterLabel(menu.getPortMode(), menu.getPortFilter()), b -> onFilterClick())
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
        PortFilter next = switch (menu.getPortFilter()) {
            case BOTH -> PortFilter.ONLY_SOLID_FUEL;
            case ONLY_SOLID_FUEL -> PortFilter.ONLY_COOLANT_LIQUID;
            case ONLY_COOLANT_LIQUID -> PortFilter.BOTH;
        };
        ClientPacketDistributor.sendToServer(new ResourcePortFilterPayload(pos, next.getId()));
    }

    private static Component filterLabel(PortMode mode, PortFilter filter) {
        return filter.getFilterButtonLabel(mode);
    }

    private void updateToggleButtonLabels() {
        PortMode mode = menu.getPortMode();
        PortFilter filter = menu.getPortFilter();
        boolean modeChanged = lastMode != mode;
        if (modeChanged) {
            lastMode = mode;
            applyModeLabel(toggleButtons[0], modeLabel(mode));
        }
        if (btnFilter != null && (lastFilter != filter || modeChanged)) {
            lastFilter = filter;
            btnFilter.setMessage(filterLabel(mode, filter));
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

    private void sendDump() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (!pos.equals(BlockPos.ZERO)) {
            ClientPacketDistributor.sendToServer(new FluidTankDumpPayload(pos));
        }
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
        }

        int amount = menu.getFluidAmount();
        int capacity = menu.getFluidCapacity();
        int fluidId = menu.getFluidId();
        if (capacity > 0 && amount > 0 && fluidId >= 0) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != null && fluid != Fluids.EMPTY) {
                int fillPx = (ResourcePortGuiLayout.BAR_FILL_H * amount) / capacity;
                if (fillPx > 0) {
                    int barLeft = ResourcePortGuiLayout.liquidBarFillLeft(leftPos);
                    int barBottom = ResourcePortGuiLayout.liquidBarFillBottom(topPos);
                    FluidRenderHelper.drawFluidInTank(guiGraphics, new FluidStack(fluid, amount),
                            barLeft, barBottom - fillPx, ResourcePortGuiLayout.BAR_FILL_W, fillPx);
                }
            }
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
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void extractTooltip(net.minecraft.client.gui.GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);
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
}
