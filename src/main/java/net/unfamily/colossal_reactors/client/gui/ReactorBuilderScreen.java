package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
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
import net.unfamily.colossal_reactors.menu.ReactorBuilderMenu;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.network.ReactorBuilderHeatSinkPayload;
import net.unfamily.colossal_reactors.network.ReactorBuilderSizePayload;
import net.unfamily.colossal_reactors.network.ReactorPreviewPayload;

import java.util.ArrayList;
import java.util.List;

/**
 * Reactor Builder GUI. Background reactor_builder.png 230x230; slots offset +27px right from left edge.
 */
public class ReactorBuilderScreen extends AbstractContainerScreen<ReactorBuilderMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/reactor_builder.png");

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 230;

    /**
     * Tank at (12, 26), same dimensions as Resource Port: 12x54 fill with 1px inset
     */
    private static final int FLUID_BAR_X = 12;
    private static final int FLUID_BAR_Y = 26;
    private static final int FLUID_FILL_WIDTH = 12;
    private static final int FLUID_FILL_HEIGHT = 54;
    private static final int FLUID_FILL_INSET = 1;

    /**
     * Size above buttons (centered). Buffer/inventory: left 35, right 35+9*18=197.
     */
    private static final int SIZE_LABEL_Y = 22;
    /**
     * Area buttons: ^ on row1; < V > on row2. Arrow block aligned with inventory left edge (35).
     */
    private static final int BUTTON_W = 14;
    private static final int BUTTON_H = 12;
    private static final int ROW1_Y = 32;
    private static final int ROW2_Y = 46;
    private static final int WARNING_Y = 62;
    private static final int GAP = 3;
    private static final int INVENTORY_LEFT_X = 35;
    private static final int ARROW_GROUP_WIDTH = 3 * BUTTON_W + 2 * GAP;  // 48
    /**
     * Arrow block left edge = inventory left edge (35).
     */
    private static final int GROUP_LEFT_X = INVENTORY_LEFT_X;
    private static final int BUTTON_UP_X = GROUP_LEFT_X + ARROW_GROUP_WIDTH / 2 - BUTTON_W / 2;
    private static final int BUTTON_LEFT_X = GROUP_LEFT_X;
    private static final int BUTTON_DOWN_X = GROUP_LEFT_X + BUTTON_W + GAP;
    private static final int BUTTON_RIGHT_X = GROUP_LEFT_X + 2 * (BUTTON_W + GAP);
    private static final int WARNING_X = GROUP_LEFT_X;

    /**
     * 6 buttons: 12px from GUI right edge; gap between arrow block and this block.
     */
    private static final int RIGHT_EDGE_INSET = 12;
    private static final int RIGHT_BUTTON_W = 42;
    private static final int RIGHT_BLOCK_X = GUI_WIDTH - RIGHT_EDGE_INSET - (2 * RIGHT_BUTTON_W + GAP);
    private static final int RIGHT_BUTTON_H = BUTTON_H;
    private static final int RIGHT_COL0_X = RIGHT_BLOCK_X;
    private static final int RIGHT_COL1_X = RIGHT_BLOCK_X + RIGHT_BUTTON_W + GAP;
    private static final int RIGHT_ROW0_Y = 32;
    private static final int RIGHT_ROW1_Y = 46;
    private static final int RIGHT_ROW2_Y = 60;

    private static final String TOOLTIP_LEFT_CLICK = "gui.colossal_reactors.reactor_builder.tooltip.left_click";
    private static final String TOOLTIP_RIGHT_CLICK = "gui.colossal_reactors.reactor_builder.tooltip.right_click";
    private static final String TOOLTIP_SHIFT_10 = "gui.colossal_reactors.reactor_builder.tooltip.shift_10";
    private static final String TOOLTIP_ALT_CTRL_5 = "gui.colossal_reactors.reactor_builder.tooltip.alt_ctrl_5";

    private static Tooltip tooltipWithValue(String valueKey, int value) {
        Component full = Component.translatable(valueKey, value)
                .append(Component.literal("\n"))
                .append(Component.translatable(TOOLTIP_LEFT_CLICK))
                .append(Component.literal("\n"))
                .append(Component.translatable(TOOLTIP_RIGHT_CLICK))
                .append(Component.literal("\n"))
                .append(Component.translatable(TOOLTIP_SHIFT_10))
                .append(Component.literal("\n"))
                .append(Component.translatable(TOOLTIP_ALT_CTRL_5));
        return Tooltip.create(full);
    }

    private Button buttonUp;
    private Button buttonLeft;
    private Button buttonRight;
    private Button buttonDown;
    /**
     * Right block buttons, index 0–5 (displayed as 1–6).
     */
    private final Button[] rightBlockButtons = new Button[6];

    public ReactorBuilderScreen(ReactorBuilderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        BlockPos pos = menu.getBlockPos();
        buttonUp = Button.builder(Component.literal("\u2191"), b -> sendSize(0, true))  // ↑
                .bounds(leftPos + BUTTON_UP_X, topPos + ROW1_Y, BUTTON_W, BUTTON_H)
                .build();
        buttonLeft = Button.builder(Component.literal("\u2190"), b -> sendSize(1, true))  // ← changes first value (L = sizeRight)
                .bounds(leftPos + BUTTON_LEFT_X, topPos + ROW2_Y, BUTTON_W, BUTTON_H)
                .build();
        buttonDown = Button.builder(Component.literal("-"), b -> sendSize(3, true))  // Z/depth
                .bounds(leftPos + BUTTON_DOWN_X, topPos + ROW2_Y, BUTTON_W, BUTTON_H)
                .build();
        buttonRight = Button.builder(Component.literal("\u2192"), b -> sendSize(2, true))  // → changes second value (R = sizeLeft)
                .bounds(leftPos + BUTTON_RIGHT_X, topPos + ROW2_Y, BUTTON_W, BUTTON_H)
                .build();
        addRenderableWidget(buttonUp);
        addRenderableWidget(buttonLeft);
        addRenderableWidget(buttonDown);
        addRenderableWidget(buttonRight);

        // Right block: 2 cols x 3 rows. 0=Heat Sink, 5=Preview, others 1–4, 6 reserved.
        int[] rowY = {RIGHT_ROW0_Y, RIGHT_ROW1_Y, RIGHT_ROW2_Y};
        int[] colX = {RIGHT_COL0_X, RIGHT_COL1_X};
        for (int i = 0; i < 6; i++) {
            int row = i / 2;
            int col = i % 2;
            Component label = (i == 0) ? getHeatSinkButtonLabel()
                    : (i == 4) ? Component.translatable("gui.colossal_reactors.reactor_builder.preview")
                    : Component.literal(String.valueOf(i + 1));
            final int buttonIndex = i;
            rightBlockButtons[i] = Button.builder(label, b -> onRightBlockClick(buttonIndex))
                    .bounds(leftPos + colX[col], topPos + rowY[row], RIGHT_BUTTON_W, RIGHT_BUTTON_H)
                    .build();
            if (i == 4) {
                rightBlockButtons[i].setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.reactor_builder.preview.tooltip")));
            }
            addRenderableWidget(rightBlockButtons[i]);
        }
    }

    private Component getHeatSinkButtonLabel() {
        if (minecraft != null && minecraft.level != null) {
            return HeatSinkLoader.getOptionDisplayName(minecraft.level.registryAccess(), menu.getHeatSinkIndex());
        }
        return Component.translatable("block.minecraft.air");
    }

    private void onRightBlockClick(int index) {
        BlockPos pos = menu.getBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        if (index == 0) {
            PacketDistributor.sendToServer(new ReactorBuilderHeatSinkPayload(pos, false));  // left click = previous
            return;
        }
        if (index == 4) {
            PacketDistributor.sendToServer(new ReactorPreviewPayload(pos));
        }
    }

    private void updateButtonTooltips() {
        buttonUp.setTooltip(tooltipWithValue("gui.colossal_reactors.reactor_builder.tooltip.up_value", menu.getSizeH()));
        buttonLeft.setTooltip(tooltipWithValue("gui.colossal_reactors.reactor_builder.tooltip.left_value", menu.getSizeLeft()));
        buttonRight.setTooltip(tooltipWithValue("gui.colossal_reactors.reactor_builder.tooltip.right_value", menu.getSizeRight()));
        buttonDown.setTooltip(tooltipWithValue("gui.colossal_reactors.reactor_builder.tooltip.behind_value", menu.getSizeD()));
        // Heat sink button: current value + left=previous, right=next
        Component heatSinkName = getHeatSinkButtonLabel();
        Component heatSinkTooltip = Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_value", heatSinkName)
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_left"))
                .append(Component.literal("\n"))
                .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_right"));
        rightBlockButtons[0].setTooltip(Tooltip.create(heatSinkTooltip));
        rightBlockButtons[0].setMessage(getHeatSinkButtonLabel());
    }

    private void sendSize(int direction, boolean increment) {
        BlockPos pos = menu.getBlockPos();
        if (pos.equals(BlockPos.ZERO)) return;
        int amount = Screen.hasShiftDown() ? 10 : (Screen.hasControlDown() || Screen.hasAltDown()) ? 5 : 1;
        PacketDistributor.sendToServer(new ReactorBuilderSizePayload(pos, direction, increment, amount));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = (int) mouseX;
        int y = (int) mouseY;
        if (button == 1) {
            if (isInBounds(x, y, leftPos + BUTTON_UP_X, topPos + ROW1_Y)) {
                sendSize(0, false);
                return true;
            }
            if (isInBounds(x, y, leftPos + BUTTON_LEFT_X, topPos + ROW2_Y)) {
                sendSize(1, false);
                return true;
            }
            if (isInBounds(x, y, leftPos + BUTTON_DOWN_X, topPos + ROW2_Y)) {
                sendSize(3, false);
                return true;
            }
            if (isInBounds(x, y, leftPos + BUTTON_RIGHT_X, topPos + ROW2_Y)) {
                sendSize(2, false);
                return true;
            }
            if (isInRightBlockButton(x, y, 0)) {
                BlockPos pos = menu.getBlockPos();
                if (!pos.equals(BlockPos.ZERO))
                    PacketDistributor.sendToServer(new ReactorBuilderHeatSinkPayload(pos, true));  // right click = next
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean isInBounds(int x, int y, int left, int top) {
        return x >= left && x < left + BUTTON_W && y >= top && y < top + BUTTON_H;
    }

    private boolean isInRightBlockButton(int x, int y, int index) {
        int row = index / 2;
        int col = index % 2;
        int left = leftPos + (col == 0 ? RIGHT_COL0_X : RIGHT_COL1_X);
        int top = topPos + (row == 0 ? RIGHT_ROW0_Y : row == 1 ? RIGHT_ROW1_Y : RIGHT_ROW2_Y);
        return x >= left && x < left + RIGHT_BUTTON_W && y >= top && y < top + RIGHT_BUTTON_H;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
        int amount = menu.getFluidAmount();
        int capacity = menu.getFluidCapacity();
        int fluidId = menu.getFluidId();
        if (capacity > 0 && amount > 0 && fluidId >= 0) {
            Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
            if (fluid != null && fluid != Fluids.EMPTY) {
                int fillPixels = (FLUID_FILL_HEIGHT * amount) / capacity;
                if (fillPixels > 0) {
                    int barLeft = leftPos + FLUID_BAR_X + FLUID_FILL_INSET;
                    int barBottom = topPos + FLUID_BAR_Y + FLUID_FILL_INSET + FLUID_FILL_HEIGHT;
                    int fillTop = barBottom - fillPixels;
                    FluidRenderHelper.drawFluidInTank(guiGraphics, new FluidStack(fluid, amount), barLeft, fillTop, FLUID_FILL_WIDTH, fillPixels);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        updateButtonTooltips();

        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, 6, 0x404040, false);

        // Size: (L+R=total) x Y x Z — centered, above buttons
        int totalW = menu.getSizeLeft() + menu.getSizeRight();
        Component sizeLabel = Component.translatable("gui.colossal_reactors.reactor_builder.size",
                menu.getSizeLeft(), menu.getSizeRight(), totalW, menu.getSizeH(), menu.getSizeD());
        int sizeX = (imageWidth - font.width(sizeLabel)) / 2;
        guiGraphics.drawString(font, sizeLabel, sizeX, SIZE_LABEL_Y, 0x404040, false);

        // Warning (red) below the arrow block, left-aligned with it
        Component warning = Component.translatable("gui.colossal_reactors.reactor_builder.warning");
        guiGraphics.drawString(font, warning, WARNING_X, WARNING_Y, 0xFF0000, false);
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
                    ? Component.translatable("gui.colossal_reactors.resource_port.tank_tooltip", amount, capacity)
                    : Component.translatable("gui.colossal_reactors.resource_port.tank_empty");
            List<FormattedCharSequence> tooltipLines = new ArrayList<>();
            tooltipLines.add(amountLine.getVisualOrderText());
            if (fluidId >= 0) {
                Fluid fluid = BuiltInRegistries.FLUID.byId(fluidId);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    FluidType type = fluid.getFluidType();
                    tooltipLines.add(Component.translatable(type.getDescriptionId()).getVisualOrderText());
                }
            }
            guiGraphics.renderTooltip(font, tooltipLines, mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        this.renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
