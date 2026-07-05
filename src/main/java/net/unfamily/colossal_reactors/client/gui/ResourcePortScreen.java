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
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.PortMedium;
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
 * Resource port GUI: mode + exclusive medium (+ fuel/waste/coolant filter on reactor ports).
 */
public class ResourcePortScreen extends AbstractContainerScreen<ResourcePortMenu> {

    private static final int COLOR_MODE = 0xFFFFFF;

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/resource_port.png");

    private final boolean mekLoaded = ModList.get().isLoaded("mekanism");

    private Button btnMode;
    private Button btnMedium;
    @Nullable
    private Button btnFilter;
    private Button btnDumpLiquid;
    private Button btnDumpGas;

    private PortMode lastMode;
    private PortFilter lastFilter;
    private PortMedium lastMedium;

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

        btnMode = Button.builder(Component.empty(), b -> onModeClick())
                .bounds(bx, topPos + ResourcePortGuiLayout.toggleY(ResourcePortGuiLayout.TOGGLE_ROW_MODE), bw, bh)
                .build();
        addRenderableWidget(btnMode);

        btnMedium = Button.builder(Component.empty(), b -> onMediumClick())
                .bounds(bx, topPos + ResourcePortGuiLayout.toggleY(ResourcePortGuiLayout.TOGGLE_ROW_MEDIUM), bw, bh)
                .build();
        addRenderableWidget(btnMedium);

        if (!menu.isTurbinePort()) {
            btnFilter = Button.builder(Component.empty(), b -> onFilterClick())
                    .bounds(leftPos + ResourcePortGuiLayout.FILTER_X,
                            topPos + ResourcePortGuiLayout.toggleY(ResourcePortGuiLayout.TOGGLE_ROW_FILTER),
                            ResourcePortGuiLayout.FILTER_BTN_W, ResourcePortGuiLayout.FILTER_BTN_H)
                    .build();
            addRenderableWidget(btnFilter);
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
                .tooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.gas_dump.tooltip")))
                .build();
        btnDumpGas.visible = mekLoaded;
        addRenderableWidget(btnDumpGas);

        lastMode = null;
        lastFilter = null;
        lastMedium = null;
        updateControlButtons();
    }

    private void onModeClick() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PacketDistributor.sendToServer(new ResourcePortModePayload(pos, nextMode(menu.getPortMode(), true).getId()));
    }

    private void onModeClickBack() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PacketDistributor.sendToServer(new ResourcePortModePayload(pos, nextMode(menu.getPortMode(), false).getId()));
    }

    private static PortMode nextMode(PortMode current, boolean forward) {
        if (forward) {
            return current == PortMode.INSERT ? PortMode.EXTRACT
                    : current == PortMode.EXTRACT ? PortMode.EJECT
                    : PortMode.INSERT;
        }
        return current == PortMode.INSERT ? PortMode.EJECT
                : current == PortMode.EXTRACT ? PortMode.INSERT
                : PortMode.EXTRACT;
    }

    private void onMediumClick() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PacketDistributor.sendToServer(new ResourcePortSettingsPayload(pos,
                ResourcePortSettingsPayload.KIND_MEDIUM, ResourcePortSettingsPayload.VALUE_CYCLE_FORWARD));
    }

    private void onMediumClickBack() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PacketDistributor.sendToServer(new ResourcePortSettingsPayload(pos,
                ResourcePortSettingsPayload.KIND_MEDIUM, ResourcePortSettingsPayload.VALUE_CYCLE_BACK));
    }

    private void onFilterClick() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PacketDistributor.sendToServer(new ResourcePortFilterPayload(pos, nextFilter(menu.getPortFilter()).getId()));
    }

    private void onFilterClickBack() {
        BlockPos pos = menu.getSyncedBlockPos();
        if (pos.equals(BlockPos.ZERO)) {
            return;
        }
        PacketDistributor.sendToServer(new ResourcePortFilterPayload(pos, nextFilter(menu.getPortFilter()).getId()));
    }

    private static PortFilter nextFilter(PortFilter current) {
        return current == PortFilter.ONLY_COOLANT_LIQUID
                ? PortFilter.ONLY_SOLID_FUEL
                : PortFilter.ONLY_COOLANT_LIQUID;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            if (btnMode != null && btnMode.isMouseOver(mouseX, mouseY)) {
                onModeClickBack();
                playClickSound();
                return true;
            }
            if (btnMedium != null && btnMedium.active && btnMedium.isMouseOver(mouseX, mouseY)) {
                onMediumClickBack();
                playClickSound();
                return true;
            }
            if (btnFilter != null && btnFilter.isMouseOver(mouseX, mouseY)) {
                onFilterClickBack();
                playClickSound();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void updateControlButtons() {
        PortMode mode = menu.getPortMode();
        PortFilter filter = menu.getPortFilter();
        PortMedium medium = menu.getPortMedium();

        if (lastMode != mode) {
            applyModeLabel(btnMode, modeLabel(mode));
            btnMode.setTooltip(Tooltip.create(Component.translatable(modeTooltipKey(mode))));
        }

        if (lastMedium != medium) {
            lastMedium = medium;
            applyMediumLabel(btnMedium, mediumLabel(medium), mediumColor(medium));
            btnMedium.setTooltip(Tooltip.create(Component.translatable(mediumTooltipKey(medium))));
        }
        btnMedium.active = mekLoaded || !menu.isTurbinePort();

        if (btnFilter != null && (lastFilter != filter || lastMode != mode)) {
            lastFilter = filter;
            btnFilter.setMessage(filterLabel(filter, mode));
            btnFilter.setTooltip(Tooltip.create(Component.translatable(filter.getTooltipKey(mode))));
        }
        lastMode = mode;
    }

    private static MutableComponent modeLabel(PortMode mode) {
        return Component.translatable("gui.colossal_reactors.resource_port.mode." + mode.name().toLowerCase());
    }

    private static MutableComponent mediumLabel(PortMedium medium) {
        return Component.translatable("gui.colossal_reactors.resource_port.toggle." + medium.name().toLowerCase());
    }

    private static int mediumColor(PortMedium medium) {
        return switch (medium) {
            case SOLID -> 0xFFFF55;
            case LIQUID -> 0x55FFFF;
            case GAS -> 0xFF55FF;
        };
    }

    private String modeTooltipKey(PortMode mode) {
        if (menu.isTurbinePort()) {
            return "gui.colossal_reactors.turbine_resource_port.mode." + mode.name().toLowerCase() + ".tooltip";
        }
        return mode.getTooltipKey();
    }

    private String mediumTooltipKey(PortMedium medium) {
        if (menu.isTurbinePort()) {
            return "gui.colossal_reactors.resource_port.medium.turbine." + medium.name().toLowerCase() + ".tooltip";
        }
        return "gui.colossal_reactors.resource_port.medium." + medium.name().toLowerCase() + ".tooltip";
    }

    private static Component filterLabel(PortFilter filter, PortMode mode) {
        return filter.getFilterButtonLabel(mode);
    }

    private static void applyModeLabel(Button button, MutableComponent text) {
        button.setMessage(text.withStyle(ChatFormatting.WHITE, ChatFormatting.BOLD));
        button.setFGColor(COLOR_MODE);
        button.active = true;
    }

    private static void applyMediumLabel(Button button, MutableComponent text, int fgColor) {
        button.setMessage(text.withStyle(ChatFormatting.BOLD));
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
        playClickSound();
        PacketDistributor.sendToServer(new FluidTankDumpPayload(pos, tankType));
    }

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
        if (!menu.showItemSlot()) {
            ResourcePortGuiLayout.fillItemSlotMask(g, leftPos, topPos);
        }
    }

    private void renderLiquidBar(GuiGraphics g, int guiX, int guiY) {
        long amount = menu.getFluidAmountLong();
        long capacity = menu.getFluidCapacityLong();
        int fluidId = menu.getFluidId();
        if (capacity <= 0 || amount <= 0 || fluidId < 0) return;
        Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
        if (fluid == null || fluid == Fluids.EMPTY) return;
        int fillPx = ResourcePortGuiLayout.barFillPixels(amount, capacity, ResourcePortGuiLayout.BAR_FILL_H);
        if (fillPx <= 0) return;
        int fillTop = ResourcePortGuiLayout.liquidBarFillBottom(guiY) - fillPx;
        int stackAmount = amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
        FluidRenderHelper.drawFluidInTank(g, new FluidStack(fluid, stackAmount),
                ResourcePortGuiLayout.liquidBarFillLeft(guiX), fillTop,
                ResourcePortGuiLayout.BAR_FILL_W, fillPx);
    }

    private void renderGasBar(GuiGraphics g, int guiX, int guiY) {
        long amount = menu.getGasAmountLong();
        long capacity = menu.getGasCapacityLong();
        if (capacity <= 0 || amount <= 0) return;
        int fillPx = ResourcePortGuiLayout.barFillPixels(amount, capacity, ResourcePortGuiLayout.BAR_FILL_H);
        if (fillPx <= 0) return;
        int outerLeft = ResourcePortGuiLayout.gasBarFillLeft(guiX);
        int outerTop = ResourcePortGuiLayout.gasBarFillTop(guiY);
        String gasName = menu.getGasRegistryName();
        int stackAmount = amount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amount;
        if (!GasTankRenderHelper.drawGasInTank(g, gasName, stackAmount, outerLeft, outerTop,
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
        updateControlButtons();
        updateDumpButtons();
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
                GuiNumberFormat.format(menu.getFluidAmountLong()),
                GuiNumberFormat.format(menu.getFluidCapacityLong())).getVisualOrderText());
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
                GuiNumberFormat.format(menu.getGasAmountLong()),
                GuiNumberFormat.format(menu.getGasCapacityLong())).getVisualOrderText());
        String gasName = menu.getGasRegistryName();
        Component name = GasTankRenderHelper.getGasDisplayName(gasName);
        if (name != null) lines.add(name.getVisualOrderText());
        g.renderTooltip(font, lines, mouseX, mouseY);
    }
}
