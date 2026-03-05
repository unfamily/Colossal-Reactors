package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.menu.ResourcePortMenu;
import net.unfamily.colossal_reactors.network.ResourcePortFilterPayload;
import net.unfamily.colossal_reactors.network.ResourcePortModePayload;

import java.util.ArrayList;
import java.util.List;

/**
 * GUI for Resource Port. Background 176x166, fluid bar 12x54, slot at (37, 33), mode button on the right.
 */
public class ResourcePortScreen extends AbstractContainerScreen<ResourcePortMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "textures/gui/resource_port.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    /** Close button (X): top right */
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    /** Fluid bar: position +1 left and +1 up from (11,15) -> (10, 14), internal fill 12x54 */
    private static final int FLUID_BAR_X = 10;
    private static final int FLUID_BAR_Y = 14;
    private static final int FLUID_FILL_WIDTH = 12;
    private static final int FLUID_FILL_HEIGHT = 54;
    private static final int FLUID_FILL_INSET = 1;

    /** Mode button (Insert/Extract/Eject): to the right of the GUI */
    private static final int MODE_BUTTON_X = 120;
    private static final int MODE_BUTTON_Y = 28;
    private static final int MODE_BUTTON_WIDTH = 48;
    private static final int MODE_BUTTON_HEIGHT = 20;

    /** Filter button (Both / Solid / Coolant): below mode button */
    private static final int FILTER_BUTTON_X = 120;
    private static final int FILTER_BUTTON_Y = 52;
    private static final int FILTER_BUTTON_WIDTH = 48;
    private static final int FILTER_BUTTON_HEIGHT = 20;

    /** Screen-space bounds of fluid bar for tooltip (left, top, width, height) */
    private static int fluidBarLeft(ResourcePortScreen screen) { return screen.leftPos + FLUID_BAR_X + FLUID_FILL_INSET; }
    private static int fluidBarTop(ResourcePortScreen screen) { return screen.topPos + FLUID_BAR_Y + FLUID_FILL_INSET; }

    private Button closeButton;
    private CycleButton<PortMode> modeButton;
    private CycleButton<PortFilter> filterButton;

    public ResourcePortScreen(ResourcePortMenu menu, Inventory playerInventory, Component title) {
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
        modeButton = CycleButton.builder(PortMode::getDisplayName)
                .withValues(PortMode.values())
                .withInitialValue(menu.getPortMode())
                .displayOnlyValue()
                .create(
                        leftPos + MODE_BUTTON_X,
                        topPos + MODE_BUTTON_Y,
                        MODE_BUTTON_WIDTH,
                        MODE_BUTTON_HEIGHT,
                        Component.empty(),
                        (button, mode) -> {
                            BlockPos pos = menu.getSyncedBlockPos();
                            if (!pos.equals(BlockPos.ZERO)) {
                                PacketDistributor.sendToServer(new ResourcePortModePayload(pos, mode.getId()));
                            }
                        }
                );
        addRenderableWidget(modeButton);
        filterButton = CycleButton.<PortFilter>builder(filter -> filter.getDisplayName(menu.getPortMode()))
                .withValues(PortFilter.values())
                .withInitialValue(menu.getPortFilter())
                .displayOnlyValue()
                .create(
                        leftPos + FILTER_BUTTON_X,
                        topPos + FILTER_BUTTON_Y,
                        FILTER_BUTTON_WIDTH,
                        FILTER_BUTTON_HEIGHT,
                        Component.empty(),
                        (button, filter) -> {
                            BlockPos pos = menu.getSyncedBlockPos();
                            if (!pos.equals(BlockPos.ZERO)) {
                                PacketDistributor.sendToServer(new ResourcePortFilterPayload(pos, filter.getId()));
                            }
                        }
                );
        addRenderableWidget(filterButton);
        updateFilterButtonVisibility();
    }

    private void updateFilterButtonVisibility() {
        if (filterButton != null) {
            filterButton.visible = menu.getPortMode() == PortMode.EXTRACT || menu.getPortMode() == PortMode.EJECT;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight, GUI_WIDTH, GUI_HEIGHT);

        int amount = menu.getFluidAmount();
        int capacity = menu.getFluidCapacity();
        int fluidId = menu.getFluidId();
        if (capacity > 0 && amount > 0 && fluidId >= 0) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != null && fluid != Fluids.EMPTY) {
                int fillPixels = (FLUID_FILL_HEIGHT * amount) / capacity;
                if (fillPixels > 0) {
                    int barLeft = fluidBarLeft(this);
                    int barBottom = fluidBarTop(this) + FLUID_FILL_HEIGHT;
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
        if (modeButton != null && modeButton.getValue() != menu.getPortMode()) {
            modeButton.setValue(menu.getPortMode());
            updateFilterButtonVisibility();
        }
        if (filterButton != null && filterButton.getValue() != menu.getPortFilter()) {
            filterButton.setValue(menu.getPortFilter());
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
        int left = fluidBarLeft(this);
        int top = fluidBarTop(this);
        if (mouseX >= left && mouseX < left + FLUID_FILL_WIDTH && mouseY >= top && mouseY < top + FLUID_FILL_HEIGHT) {
            int amount = menu.getFluidAmount();
            int capacity = menu.getFluidCapacity();
            int fluidId = menu.getFluidId();
            // Line 0: quantity / maximum
            Component amountLine = capacity > 0
                    ? Component.translatable("gui.colossal_reactors.resource_port.tank_tooltip", amount, capacity)
                    : Component.translatable("gui.colossal_reactors.resource_port.tank_empty");
            List<FormattedCharSequence> tooltipLines = new ArrayList<>();
            tooltipLines.add(amountLine.getVisualOrderText());
            // Line 1: fluid name (if any)
            if (fluidId >= 0) {
                Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    FluidType type = fluid.getFluidType();
                    tooltipLines.add(Component.translatable(type.getDescriptionId()).getVisualOrderText());
                }
            }
            guiGraphics.renderTooltip(font, tooltipLines, mouseX, mouseY);
        }
        // Mode button tooltip (single line: description only)
        int btnX = leftPos + MODE_BUTTON_X;
        int btnY = topPos + MODE_BUTTON_Y;
        if (mouseX >= btnX && mouseX < btnX + MODE_BUTTON_WIDTH && mouseY >= btnY && mouseY < btnY + MODE_BUTTON_HEIGHT) {
            PortMode mode = menu.getPortMode();
            Component line = Component.translatable(mode.getTooltipKey());
            if (!line.getString().isEmpty()) {
                guiGraphics.renderTooltip(font, List.of(line.getVisualOrderText()), mouseX, mouseY);
            }
        }
        // Filter button tooltip (only when visible, i.e. Extract/Eject mode)
        if (filterButton != null && filterButton.visible) {
            int fBtnX = leftPos + FILTER_BUTTON_X;
            int fBtnY = topPos + FILTER_BUTTON_Y;
            if (mouseX >= fBtnX && mouseX < fBtnX + FILTER_BUTTON_WIDTH && mouseY >= fBtnY && mouseY < fBtnY + FILTER_BUTTON_HEIGHT) {
                PortFilter filter = menu.getPortFilter();
                Component line = Component.translatable(filter.getTooltipKey(menu.getPortMode()));
                if (!line.getString().isEmpty()) {
                    guiGraphics.renderTooltip(font, List.of(line.getVisualOrderText()), mouseX, mouseY);
                }
            }
        }
    }
}
