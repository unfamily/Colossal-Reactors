package net.unfamily.colossal_reactors.client.gui;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.lwjgl.glfw.GLFW;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.menu.ReactorBuilderMenu;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.network.ReactorBuilderBuildPayload;
import net.unfamily.colossal_reactors.network.ReactorBuilderMarkInputPayload;
import net.unfamily.colossal_reactors.network.ReactorBuilderHeatSinkPayload;
import net.unfamily.colossal_reactors.network.ReactorBuilderOptionPayload;
import net.unfamily.colossal_reactors.network.ReactorBuilderSizePayload;
import net.unfamily.colossal_reactors.network.FluidTankDumpPayload;
import net.unfamily.colossal_reactors.network.ReactorPreviewPayload;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.reactor.ReactorBuildMaterialCounter;
import net.unfamily.colossal_reactors.reactor.ReactorSimulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Reactor Builder GUI. Background reactor_builder.png 230x240; simulation uses reactor_controller.png 230x240.
 * Like Deep Drawer Extractor: Simulation is a view mode (isSimulationView) on the same screen, not a separate Screen.
 */
public class ReactorBuilderScreen extends AbstractContainerScreen<ReactorBuilderMenu> {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/reactor_builder.png");
    private static final int GUI_WIDTH = ReactorControllerGui.WIDTH;
    /** Builder inventory GUI ({@code reactor_builder.png}). */
    private static final int BUILDER_GUI_HEIGHT = 240;
    /** Reactor panel in simulation view ({@code reactor_controller.png}). */
    private static final int REACTOR_GUI_HEIGHT = ReactorControllerGui.HEIGHT;

    /** Close button (X): top right */
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    /**
     * Tank at (12, 26), same dimensions as Resource Port: 12x54 fill with 1px inset
     */
    private static final int FLUID_BAR_X = 12;
    private static final int FLUID_BAR_Y = 26;
    private static final int FLUID_FILL_WIDTH = 12;
    private static final int FLUID_FILL_HEIGHT = 54;
    private static final int FLUID_FILL_INSET = 1;

    /** Dump (D): same size as build arrows; centered under the fluid tank. */
    private static final int DUMP_BUTTON_W = 14;
    private static final int DUMP_BUTTON_H = 12;
    private static final int DUMP_BUTTON_GAP_BELOW = 3;
    private static final int TANK_OUTER_W = FLUID_FILL_WIDTH + 2 * FLUID_FILL_INSET;
    private static final int TANK_OUTER_H = FLUID_FILL_HEIGHT + 2 * FLUID_FILL_INSET;
    private static final int FLUID_DUMP_X = FLUID_BAR_X + (TANK_OUTER_W - DUMP_BUTTON_W) / 2;
    private static final int FLUID_DUMP_Y = FLUID_BAR_Y + TANK_OUTER_H + DUMP_BUTTON_GAP_BELOW;

    /**
     * Size above buttons (centered). Buffer/inventory: left 35, right 35+9*18=197.
     */
    private static final int SIZE_LABEL_Y = 18;
    /**
     * Area buttons: ^ on row1; < V > on row2. Arrow block aligned with inventory left edge (35).
     */
    private static final int BUTTON_W = 14;
    private static final int BUTTON_H = 12;
    private static final int ROW1_Y = 32;
    private static final int ROW2_Y = 46;
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
    /** Preview button below the 4 arrows, centered with arrow group. */
    private static final int PREVIEW_BUTTON_Y = ROW2_Y + BUTTON_H + GAP;
    private static final int PREVIEW_BUTTON_W = ARROW_GROUP_WIDTH;
    private static final int PREVIEW_BUTTON_X = GROUP_LEFT_X;
    /** Mark Input: same width as Preview, directly below it. */
    private static final int MARK_INPUT_BUTTON_Y = PREVIEW_BUTTON_Y + BUTTON_H + GAP;

    /**
     * 6 buttons: 3 cols x 2 rows (Heat Sink, Pattern, PatternMode, OpenTop, Simulation, Build/Stop).
     * Invalid-blocks warning at {@link #PREVIEW_BUTTON_Y}; second right row aligns with {@link #MARK_INPUT_BUTTON_Y}.
     */
    private static final int RIGHT_EDGE_INSET = 12;
    private static final int RIGHT_BUTTON_W = 42;
    private static final int RIGHT_BLOCK_X = GUI_WIDTH - RIGHT_EDGE_INSET - (3 * RIGHT_BUTTON_W + 2 * GAP);
    private static final int RIGHT_BUTTON_H = BUTTON_H;
    private static final int RIGHT_COL0_X = RIGHT_BLOCK_X;
    private static final int RIGHT_COL1_X = RIGHT_BLOCK_X + RIGHT_BUTTON_W + GAP;
    private static final int RIGHT_COL2_X = RIGHT_BLOCK_X + 2 * (RIGHT_BUTTON_W + GAP);
    // Align right block row 0 with the up arrow (ROW1_Y). Row 1 aligns with Mark Input (see MARK_INPUT_BUTTON_Y).
    private static final int RIGHT_ROW0_Y = ROW1_Y;
    /** Y for invalid-blocks message (same band as former second button row). */
    private static final int RIGHT_ROW_WARNING_MESSAGE_Y = PREVIEW_BUTTON_Y;
    /** Second row of right buttons: aligned with Mark Input (left column). */
    private static final int RIGHT_ROW1_Y = MARK_INPUT_BUTTON_Y;
    /** Warning text: X = right block left edge; Y computed so text bottom aligns with bottom of right arrow button (ROW2_Y + BUTTON_H). */
    private static final int WARNING_RIGHT_X = RIGHT_BLOCK_X;

    /** Simulation view panel (same layout as reactor controller). */
    private static final int SIM_PANEL_X = 16;
    private static final int SIM_PANEL_Y = GuiPanelScrollbar.TEXT_TOP;
    private static final int SIM_LINE_HEIGHT = 12;
    /** Stats lines on simulation panel (same as reactor controller GUI). */
    private static final int SIM_STATS_COLOR = GuiTextColors.PANEL_WHITE;

    /** Coolant/Fuel cycle buttons: same horizontal inset each side (12px), reduced gap, width split equally. */
    private static final int SIM_BUTTONS_HORIZONTAL_INSET = 12;
    private static final int SIM_BUTTONS_GAP = 2;
    private static final int COOLANT_BUTTON_W = (GUI_WIDTH - SIM_BUTTONS_HORIZONTAL_INSET * 2 - SIM_BUTTONS_GAP) / 2;
    private static final int FUEL_BUTTON_W = COOLANT_BUTTON_W;
    private static final int COOLANT_BUTTON_H = 20;
    private static final int COOLANT_BUTTON_RIGHT_INSET = SIM_BUTTONS_HORIZONTAL_INSET;
    private static final int COOLANT_BUTTON_BOTTOM_INSET = 13;

    private enum ViewMode { BUILDER, SIMULATION }

    /** Builder / simulation view on the same screen (like Deep Drawer sub-views). */
    private ViewMode viewMode = ViewMode.BUILDER;
    /** Index into ordered coolant list for simulation view (which coolant type is shown). */
    private int simulationCoolantIndex = 0;
    /** Index into ordered fuel list for simulation view (which fuel type is shown). Default uranium if present. */
    private int simulationFuelIndex = 0;

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

    private Button closeButton;
    private Button buttonUp;
    private Button buttonLeft;
    private Button buttonRight;
    private Button buttonDown;
    private Button buttonPreview;
    private Button buttonMarkInput;
    private Button buttonDumpFluid;
    /** Right block buttons: 0=Heat Sink, 1=Pattern, 2=PatternMode, 3=OpenTop, 4=Simulation, 5=Build/Stop. */
    private final Button[] rightBlockButtons = new Button[6];
    /** Shown only in simulation view: cycles coolant type (same position as Reboot in controller). */
    private Button coolantCycleButton;
    /** Shown only in simulation view: cycles fuel type (next to coolant). */
    private Button fuelCycleButton;
    private final GuiPanelScrollbar simulationScrollbar = new GuiPanelScrollbar();

    public ReactorBuilderScreen(ReactorBuilderMenu menu, Inventory playerInventory, Component title) {
        // Window tall enough for simulation ({@code reactor_controller.png}); builder background uses {@link #BUILDER_GUI_HEIGHT}.
        super(menu, playerInventory, title, GUI_WIDTH, REACTOR_GUI_HEIGHT);
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("\u2715"), b -> onCloseButtonClicked())
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
        buttonDumpFluid = Button.builder(Component.literal("D"), b -> onFluidDumpPressed())
                .bounds(leftPos + FLUID_DUMP_X, topPos + FLUID_DUMP_Y, DUMP_BUTTON_W, DUMP_BUTTON_H)
                .build();
        buttonDumpFluid.setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.fluid_dump.tooltip")));
        addRenderableWidget(buttonDumpFluid);
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

        // Preview below the 4 arrows, centered with arrow group
        buttonPreview = Button.builder(Component.translatable("gui.colossal_reactors.reactor_builder.preview"), b -> {
            if (menu.getBlockEntity() != null)
                ClientPacketDistributor.sendToServer(new ReactorPreviewPayload(menu.getBlockPos()));
        })
                .bounds(leftPos + PREVIEW_BUTTON_X, topPos + PREVIEW_BUTTON_Y, PREVIEW_BUTTON_W, BUTTON_H)
                .build();
        buttonPreview.setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.reactor_builder.preview.tooltip")));
        addRenderableWidget(buttonPreview);

        buttonMarkInput = Button.builder(Component.translatable("gui.colossal_reactors.reactor_builder.mark_input"), b -> onMarkInputPressed())
                .bounds(leftPos + PREVIEW_BUTTON_X, topPos + MARK_INPUT_BUTTON_Y, PREVIEW_BUTTON_W, BUTTON_H)
                .build();
        buttonMarkInput.setTooltip(Tooltip.create(
                Component.translatable("gui.colossal_reactors.reactor_builder.mark_input.tooltip.line1")
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.mark_input.tooltip.line2"))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.mark_input.tooltip.line3"))));
        addRenderableWidget(buttonMarkInput);

        // Right block: 3 cols x 2 rows. 0=Heat Sink, 1=Pattern, 2=PatternMode, 3=OpenTop, 4=Simulation, 5=Build/Stop.
        int[] rowY = {RIGHT_ROW0_Y, RIGHT_ROW1_Y};
        int[] colX = {RIGHT_COL0_X, RIGHT_COL1_X, RIGHT_COL2_X};
        for (int i = 0; i < 6; i++) {
            int row = i / 3;
            int col = i % 3;
            Component label = getRightButtonLabel(i);
            final int buttonIndex = i;
            rightBlockButtons[i] = Button.builder(label, b -> onRightBlockClick(buttonIndex))
                    .bounds(leftPos + colX[col], topPos + rowY[row], RIGHT_BUTTON_W, RIGHT_BUTTON_H)
                    .build();
            addRenderableWidget(rightBlockButtons[i]);
        }
        int coolantY = topPos + imageHeight - COOLANT_BUTTON_H - COOLANT_BUTTON_BOTTOM_INSET;
        int fuelX = leftPos + SIM_BUTTONS_HORIZONTAL_INSET;
        int coolantX = fuelX + FUEL_BUTTON_W + SIM_BUTTONS_GAP;
        coolantCycleButton = Button.builder(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.coolant_none"), b -> cycleSimulationCoolant(true))
                .bounds(coolantX, coolantY, COOLANT_BUTTON_W, COOLANT_BUTTON_H)
                .build();
        addRenderableWidget(coolantCycleButton);
        simulationFuelIndex = getUraniumIndex();
        fuelCycleButton = Button.builder(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_uranium"), b -> cycleSimulationFuel(true))
                .bounds(fuelX, coolantY, FUEL_BUTTON_W, COOLANT_BUTTON_H)
                .build();
        addRenderableWidget(fuelCycleButton);
        simulationScrollbar.createButtons(leftPos, topPos, this::addRenderableWidget, () -> {});
        updateWidgetVisibility();
    }

    @Override
    public void containerTick() {
        super.containerTick();
        // Match StructureSaverMachineScreen / DeepDrawers: refresh labels in tick, not every extractLabels frame.
        if (viewMode == ViewMode.BUILDER) {
            updateButtonTooltips();
        }
    }

    private static List<Identifier> getOrderedCoolantIds() {
        List<Identifier> ids = new ArrayList<>(CoolantLoader.getAll().keySet());
        ids.sort(Identifier::compareTo);
        return ids;
    }

    private static List<Identifier> getOrderedFuelIds() {
        List<Identifier> ids = new ArrayList<>(FuelLoader.getAll().keySet());
        ids.sort(Identifier::compareTo);
        return ids;
    }

    /** Index of uranium in ordered fuel list, or 0 if not found / empty. */
    private static int getUraniumIndex() {
        List<Identifier> ids = getOrderedFuelIds();
        if (ids.isEmpty()) return 0;
        for (int i = 0; i < ids.size(); i++) {
            if (ReactorRodBlockEntity.URANIUM_FUEL_ID.equals(ids.get(i))) return i;
        }
        return 0;
    }

    /** Number of options: 0 = None, then one per coolant. */
    private static int getCoolantOptionCount() {
        return 1 + getOrderedCoolantIds().size();
    }

    private void cycleSimulationCoolant(boolean next) {
        int options = getCoolantOptionCount();
        if (options <= 1) return;
        if (next) {
            simulationCoolantIndex = (simulationCoolantIndex + 1) % options;
        } else {
            simulationCoolantIndex = simulationCoolantIndex <= 0 ? options - 1 : simulationCoolantIndex - 1;
        }
        updateCoolantButtonLabel();
    }

    private void updateCoolantButtonLabel() {
        if (coolantCycleButton == null) return;
        List<Identifier> ids = getOrderedCoolantIds();
        Component clickHint = Component.translatable("gui.colossal_reactors.reactor_builder.simulation.click_hint");
        // Index 0 = None (no coolant)
        MutableComponent coolantTitle = Component.translatable("gui.colossal_reactors.reactor_builder.simulation.coolant_label");
        if (simulationCoolantIndex == 0) {
            coolantCycleButton.setMessage(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.coolant_none"));
            coolantCycleButton.setTooltip(Tooltip.create(coolantTitle.append(Component.literal("\n")).append(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.coolant_tooltip", "—", "—")).append(Component.literal("\n")).append(clickHint)));
            return;
        }
        int idx = simulationCoolantIndex - 1;
        if (idx >= ids.size()) {
            coolantCycleButton.setMessage(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.coolant_none"));
            coolantCycleButton.setTooltip(Tooltip.create(coolantTitle.append(Component.literal("\n")).append(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.coolant_tooltip", "—", "—")).append(Component.literal("\n")).append(clickHint)));
            return;
        }
        Identifier id = ids.get(idx);
        CoolantDefinition def = CoolantLoader.get(id);
        if (def == null) {
            coolantCycleButton.setMessage(Component.literal(id.toString()));
            coolantCycleButton.setTooltip(Tooltip.create(coolantTitle.append(Component.literal("\n")).append(clickHint)));
            return;
        }
        var level = minecraft != null ? minecraft.level : null;
        var ra = level != null ? level.registryAccess() : null;
        Component label = getCoolantDisplayName(def, ra);
        MutableComponent tooltip = coolantTitle.copy().append(Component.literal("\n")).append(getCoolantTooltip(def, ra)).append(Component.literal("\n")).append(clickHint);
        coolantCycleButton.setMessage(label);
        coolantCycleButton.setTooltip(Tooltip.create(tooltip));
    }

    /** Display name: first input fluid (from tag = first in tag, from id = that fluid's name). */
    private static Component getCoolantDisplayName(CoolantDefinition def, net.minecraft.core.RegistryAccess ra) {
        if (ra == null) return Component.literal(def.coolantId().toString());
        Fluid input = CoolantLoader.getFirstFluidFromDefinition(def, ra);
        if (input == null || input == Fluids.EMPTY) return Component.literal(def.coolantId().toString());
        return Component.translatable(input.getFluidType().getDescriptionId());
    }

    private static Component getCoolantTooltip(CoolantDefinition def, net.minecraft.core.RegistryAccess ra) {
        Component consume = Component.literal("—");
        Component produce = Component.literal("—");
        if (ra != null) {
            Fluid in = CoolantLoader.getFirstFluidFromDefinition(def, ra);
            if (in != null && in != Fluids.EMPTY) consume = Component.translatable(in.getFluidType().getDescriptionId());
            Fluid out = CoolantLoader.getFirstFluidFromTag(def.output(), ra);
            if (out != null && out != Fluids.EMPTY) produce = Component.translatable(out.getFluidType().getDescriptionId());
        }
        return Component.translatable("gui.colossal_reactors.reactor_builder.simulation.coolant_tooltip", consume, produce);
    }

    private void cycleSimulationFuel(boolean next) {
        List<Identifier> ids = getOrderedFuelIds();
        if (ids.isEmpty()) return;
        if (next) {
            simulationFuelIndex = (simulationFuelIndex + 1) % ids.size();
        } else {
            simulationFuelIndex = simulationFuelIndex <= 0 ? ids.size() - 1 : simulationFuelIndex - 1;
        }
        updateFuelButtonLabel();
    }

    private void updateFuelButtonLabel() {
        if (fuelCycleButton == null) return;
        List<Identifier> ids = getOrderedFuelIds();
        Component clickHint = Component.translatable("gui.colossal_reactors.reactor_builder.simulation.click_hint");
        MutableComponent fuelTitle = Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_label");
        if (ids.isEmpty()) {
            fuelCycleButton.setMessage(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_uranium"));
            fuelCycleButton.setTooltip(Tooltip.create(fuelTitle.append(Component.literal("\n")).append(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_tooltip", "—", "—")).append(Component.literal("\n")).append(clickHint)));
            return;
        }
        if (simulationFuelIndex >= ids.size()) simulationFuelIndex = 0;
        Identifier id = ids.get(simulationFuelIndex);
        var ra = minecraft != null && minecraft.level != null ? minecraft.level.registryAccess() : null;
        Component label = getFuelDisplayName(id, ra);
        MutableComponent tooltip = fuelTitle.copy().append(Component.literal("\n")).append(getFuelTooltip(id, ra)).append(Component.literal("\n")).append(clickHint);
        fuelCycleButton.setMessage(label);
        fuelCycleButton.setTooltip(Tooltip.create(tooltip));
    }

    /** Tooltip line: Consumes: X, Produces: X (input item and waste item names). */
    private static Component getFuelTooltip(Identifier fuelId, net.minecraft.core.RegistryAccess ra) {
        Component consume = Component.literal("—");
        Component produce = Component.literal("—");
        if (ra != null) {
            ItemStack in = FuelLoader.getFirstInputStack(fuelId, ra);
            if (!in.isEmpty()) consume = in.getHoverName();
            ItemStack out = FuelLoader.getFirstOutputStack(fuelId, ra);
            if (!out.isEmpty()) produce = out.getHoverName();
        }
        return Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_tooltip", consume, produce);
    }

    /** Display name: first input item from tag or fixed id (like coolant). */
    private static Component getFuelDisplayName(Identifier fuelId, net.minecraft.core.RegistryAccess ra) {
        if (ra != null) {
            ItemStack stack = FuelLoader.getFirstInputStack(fuelId, ra);
            if (!stack.isEmpty()) return stack.getHoverName();
        }
        if (ReactorRodBlockEntity.URANIUM_FUEL_ID.equals(fuelId))
            return Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_uranium");
        return Component.literal(fuelId.getPath());
    }

    private void onCloseButtonClicked() {
        if (viewMode != ViewMode.BUILDER) {
            switchToBuilderView();
        } else {
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        }
    }

    private void switchToSimulationView() {
        viewMode = ViewMode.SIMULATION;
        simulationScrollbar.resetScroll();
        menu.setHideAllSlotsForSimulationView(true);
        simulationScrollbar.ensureButtons(leftPos, topPos, this::addRenderableWidget, () -> {});
        updateWidgetVisibility();
    }

    private void switchToBuilderView() {
        viewMode = ViewMode.BUILDER;
        simulationScrollbar.disposeButtons(this::removeWidget);
        menu.setHideAllSlotsForSimulationView(false);
        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        boolean showBuilder = viewMode == ViewMode.BUILDER;
        buttonUp.visible = showBuilder;
        buttonLeft.visible = showBuilder;
        buttonRight.visible = showBuilder;
        buttonDown.visible = showBuilder;
        buttonPreview.visible = showBuilder;
        buttonMarkInput.visible = showBuilder;
        buttonDumpFluid.visible = showBuilder;
        for (Button b : rightBlockButtons) b.visible = showBuilder;
        if (coolantCycleButton != null) {
            coolantCycleButton.visible = viewMode == ViewMode.SIMULATION;
            if (viewMode == ViewMode.SIMULATION) updateCoolantButtonLabel();
        }
        if (fuelCycleButton != null) {
            fuelCycleButton.visible = viewMode == ViewMode.SIMULATION;
            if (viewMode == ViewMode.SIMULATION) updateFuelButtonLabel();
        }
    }

    private Component getRightButtonLabel(int index) {
        return switch (index) {
            case 0 -> getHeatSinkButtonLabel();
            case 1 -> Component.translatable("gui.colossal_reactors.reactor_builder.pattern." + menu.getRodPattern());
            case 2 -> Component.translatable("gui.colossal_reactors.reactor_builder.pattern_mode." + menu.getPatternMode());
            case 3 -> menu.isOpenTop()
                    ? Component.translatable("gui.colossal_reactors.reactor_builder.open_top.open")
                    : Component.translatable("gui.colossal_reactors.reactor_builder.open_top.closed");
            case 4 -> Component.translatable("gui.colossal_reactors.reactor_builder.simulation");
            case 5 -> menu.isBuilding()
                    ? Component.translatable("gui.colossal_reactors.reactor_builder.stop")
                    : Component.translatable("gui.colossal_reactors.reactor_builder.build");
            default -> Component.literal("?");
        };
    }

    private Component getHeatSinkButtonLabel() {
        if (minecraft != null && minecraft.level != null) {
            return HeatSinkLoader.getOptionDisplayName(minecraft.level.registryAccess(), menu.getHeatSinkIndex());
        }
        return Component.translatable("block.minecraft.air");
    }

    private void onRightBlockClick(int index) {
        if (index == 4) {
            switchToSimulationView();
            return;
        }
        if (menu.getBlockEntity() == null) return;
        BlockPos pos = menu.getBlockPos();
        if (index == 0) {
            ClientPacketDistributor.sendToServer(new ReactorBuilderHeatSinkPayload(pos, true));  // left click = next
            return;
        }
        if (index == 1) ClientPacketDistributor.sendToServer(new ReactorBuilderOptionPayload(pos, 1, true));  // left = next
        if (index == 2) ClientPacketDistributor.sendToServer(new ReactorBuilderOptionPayload(pos, 2, true));
        if (index == 3) ClientPacketDistributor.sendToServer(new ReactorBuilderOptionPayload(pos, 0, true));  // toggle; next ignored on server
        if (index == 5) ClientPacketDistributor.sendToServer(new ReactorBuilderBuildPayload(pos));  // Build/Stop
    }

    private void onFluidDumpPressed() {
        if (menu.getBlockEntity() == null) return;
        ClientPacketDistributor.sendToServer(new FluidTankDumpPayload(menu.getBlockPos()));
    }

    private void onMarkInputPressed() {
        if (menu.getBlockEntity() == null) return;
        int mode = ReactorBuilderMarkInputPayload.MODE_NORMAL;
        if (minecraft != null && minecraft.getWindow() != null) {
            Window w = minecraft.getWindow();
            if (InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
                mode = ReactorBuilderMarkInputPayload.MODE_SHIFT;
            } else if (InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_CONTROL)
                    || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_ALT)) {
                mode = ReactorBuilderMarkInputPayload.MODE_CTRL;
            }
        }
        ClientPacketDistributor.sendToServer(new ReactorBuilderMarkInputPayload(menu.getBlockEntity().getBlockPos(), mode));
    }

    private void updateButtonTooltips() {
        buttonUp.setTooltip(tooltipWithValue("gui.colossal_reactors.reactor_builder.tooltip.up_value", menu.getSizeH() + 1));
        buttonLeft.setTooltip(tooltipWithValue("gui.colossal_reactors.reactor_builder.tooltip.left_value", menu.getSizeLeft()));
        buttonRight.setTooltip(tooltipWithValue("gui.colossal_reactors.reactor_builder.tooltip.right_value", menu.getSizeRight()));
        buttonDown.setTooltip(tooltipWithValue("gui.colossal_reactors.reactor_builder.tooltip.behind_value", menu.getSizeD() + 1));
        // Heat sink button
        Component heatSinkName = getHeatSinkButtonLabel();
        rightBlockButtons[0].setTooltip(Tooltip.create(
                Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_value", heatSinkName)
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_left"))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_right"))));
        rightBlockButtons[0].setMessage(getHeatSinkButtonLabel());
        // Button 1: Pattern (left=next, right=previous, same as heat sink)
        rightBlockButtons[1].setTooltip(Tooltip.create(
                Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.pattern." + menu.getRodPattern())
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_left"))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_right"))));
        rightBlockButtons[1].setMessage(getRightButtonLabel(1));
        // Button 2: Pattern mode
        rightBlockButtons[2].setTooltip(Tooltip.create(
                Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.pattern_mode." + menu.getPatternMode())
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_left"))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_right"))));
        rightBlockButtons[2].setMessage(getRightButtonLabel(2));
        // Button 3: Open top (click cycles open/closed)
        rightBlockButtons[3].setTooltip(Tooltip.create(
                Component.translatable(menu.isOpenTop() ? "gui.colossal_reactors.reactor_builder.tooltip.open_top.open" : "gui.colossal_reactors.reactor_builder.tooltip.open_top.closed")));
        rightBlockButtons[3].setMessage(getRightButtonLabel(3));
        // Button 4: Simulation
        rightBlockButtons[4].setMessage(getRightButtonLabel(4));
        rightBlockButtons[4].setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.tooltip")));
        // Button 5: Build/Stop
        rightBlockButtons[5].setMessage(getRightButtonLabel(5));
        rightBlockButtons[5].setTooltip(Tooltip.create(Component.translatable(
                menu.isBuilding() ? "gui.colossal_reactors.reactor_builder.stop.tooltip" : "gui.colossal_reactors.reactor_builder.build.tooltip")));
    }

    private int modifierStepAmount() {
        if (minecraft == null || minecraft.getWindow() == null) return 1;
        Window w = minecraft.getWindow();
        if (InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_SHIFT)) {
            return 10;
        }
        if (InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_CONTROL)
                || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(w, GLFW.GLFW_KEY_RIGHT_ALT)) {
            return 5;
        }
        return 1;
    }

    private void sendSize(int direction, boolean increment) {
        if (menu.getBlockEntity() == null) return;
        BlockPos pos = menu.getBlockPos();
        int amount = modifierStepAmount();
        ClientPacketDistributor.sendToServer(new ReactorBuilderSizePayload(pos, direction, increment, amount));
    }

    /** Click feedback for interactions that do not go through {@link Button} (e.g. right-click on arrows). */
    private void playGuiClickSound() {
        if (minecraft != null && minecraft.getSoundManager() != null) {
            AbstractWidget.playButtonClickSound(minecraft.getSoundManager());
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (viewMode == ViewMode.SIMULATION && event.button() == 0 && simulationScrollbar.mouseClicked(event.x(), event.y(), leftPos, topPos)) {
            return true;
        }
        int x = (int) event.x();
        int y = (int) event.y();
        if (event.button() == 1) {
            if (viewMode == ViewMode.SIMULATION && coolantCycleButton != null && coolantCycleButton.visible && isInWidget(x, y, coolantCycleButton)) {
                playGuiClickSound();
                cycleSimulationCoolant(false);
                return true;
            }
            if (viewMode == ViewMode.SIMULATION && fuelCycleButton != null && fuelCycleButton.visible && isInWidget(x, y, fuelCycleButton)) {
                playGuiClickSound();
                cycleSimulationFuel(false);
                return true;
            }
            if (isInBounds(x, y, leftPos + BUTTON_UP_X, topPos + ROW1_Y)) {
                playGuiClickSound();
                sendSize(0, false);
                return true;
            }
            if (isInBounds(x, y, leftPos + BUTTON_LEFT_X, topPos + ROW2_Y)) {
                playGuiClickSound();
                sendSize(1, false);
                return true;
            }
            if (isInBounds(x, y, leftPos + BUTTON_DOWN_X, topPos + ROW2_Y)) {
                playGuiClickSound();
                sendSize(3, false);
                return true;
            }
            if (isInBounds(x, y, leftPos + BUTTON_RIGHT_X, topPos + ROW2_Y)) {
                playGuiClickSound();
                sendSize(2, false);
                return true;
            }
            if (isInRightBlockButton(x, y, 0)) {
                playGuiClickSound();
                if (menu.getBlockEntity() != null)
                    ClientPacketDistributor.sendToServer(new ReactorBuilderHeatSinkPayload(menu.getBlockPos(), false));  // right click = previous
                return true;
            }
            if (isInRightBlockButton(x, y, 1)) {
                playGuiClickSound();
                if (menu.getBlockEntity() != null)
                    ClientPacketDistributor.sendToServer(new ReactorBuilderOptionPayload(menu.getBlockPos(), 1, false));  // right = previous
                return true;
            }
            if (isInRightBlockButton(x, y, 2)) {
                playGuiClickSound();
                if (menu.getBlockEntity() != null)
                    ClientPacketDistributor.sendToServer(new ReactorBuilderOptionPayload(menu.getBlockPos(), 2, false));
                return true;
            }
            if (isInRightBlockButton(x, y, 3)) {
                playGuiClickSound();
                if (menu.getBlockEntity() != null)
                    ClientPacketDistributor.sendToServer(new ReactorBuilderOptionPayload(menu.getBlockPos(), 0, true));  // toggle open top
                return true;
            }
        }
        // Vanilla widgets: ContainerEventHandler picks the first child where isMouseOver (forward list order).
        // Widgets added later are drawn on top but lose hits when rects overlap; temporarily reverse so top-most wins.
        @SuppressWarnings("unchecked")
        List<GuiEventListener> listeners = (List<GuiEventListener>) (List<?>) children();
        Collections.reverse(listeners);
        try {
            return super.mouseClicked(event, doubleClick);
        } finally {
            Collections.reverse(listeners);
        }
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0) {
            simulationScrollbar.mouseReleased();
        }
        return super.mouseReleased(event);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        simulationScrollbar.mouseMoved(mouseY);
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (viewMode == ViewMode.SIMULATION
                && simulationScrollbar.isInPanelArea(mouseX, mouseY, leftPos, topPos, SIM_PANEL_X)
                && simulationScrollbar.mouseScrolled(scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private static boolean isInBounds(int x, int y, int left, int top) {
        return x >= left && x < left + BUTTON_W && y >= top && y < top + BUTTON_H;
    }

    private static boolean isInWidget(int x, int y, Button widget) {
        return x >= widget.getX() && x < widget.getX() + widget.getWidth() && y >= widget.getY() && y < widget.getY() + widget.getHeight();
    }

    private boolean isInRightBlockButton(int x, int y, int index) {
        int row = index / 3;
        int col = index % 3;
        int left = leftPos + (col == 0 ? RIGHT_COL0_X : col == 1 ? RIGHT_COL1_X : RIGHT_COL2_X);
        int top = topPos + (row == 0 ? RIGHT_ROW0_Y : RIGHT_ROW1_Y);
        return x >= left && x < left + RIGHT_BUTTON_W && y >= top && y < top + RIGHT_BUTTON_H;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(guiGraphics, mouseX, mouseY, partialTick);
        if (viewMode != ViewMode.BUILDER) {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, ReactorControllerGui.BACKGROUND, leftPos, topPos, 0.0F, 0.0F, GUI_WIDTH, REACTOR_GUI_HEIGHT, GUI_WIDTH, REACTOR_GUI_HEIGHT);
        } else {
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, GUI_WIDTH, BUILDER_GUI_HEIGHT, GUI_WIDTH, BUILDER_GUI_HEIGHT);
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
    }

    /** GUI-local coords inside {@code extractContents} translate. Order: status+stats, blank, required-for-build. */
    private int renderSimulationPanel(GuiGraphicsExtractor guiGraphics) {
        int y = SIM_PANEL_Y - simulationScrollbar.getScrollOffset();
        int contentStart = y;
        y = renderSimulationStatusAndStats(guiGraphics, y);
        y += SIM_LINE_HEIGHT;
        y = renderSimulationBuildRequirements(guiGraphics, y);
        return y - contentStart;
    }

    private int renderSimulationStatusAndStats(GuiGraphicsExtractor guiGraphics, int y) {
        ReactorPanelText.drawStatusLine(guiGraphics, font, SIM_PANEL_X, y,
                Component.translatable("gui.colossal_reactors.reactor_builder.simulation"), null);
        y += SIM_LINE_HEIGHT;

        ReactorSimulation.SimulationResult result = getSimulationResult();
        int rodCount = result.rodCount();
        String fuelStr = formatFuelPerTickSim(result.fuelPerTickHundredths());

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", rodCount, result.rodColumns()),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_blocks",
                        GuiNumberFormat.format(result.coolantBlockCount())),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.energy_production",
                        GuiNumberFormat.format(result.rfPerTick())),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.water_consume",
                        GuiNumberFormat.format(result.coolantConsumedPerTick())),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.steam_production",
                        GuiNumberFormat.format(result.steamPerTick())),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_units", fuelStr),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_capacity",
                        GuiNumberFormat.format((long) rodCount * (long) Config.ROD_MAX_FUEL_UNITS.get())),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_builder.simulation.coolant_capacity",
                        GuiNumberFormat.format((long) rodCount
                                * (long) net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity.getCoolantCapacityMb())),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;

        if (Config.REACTOR_UNSTABILITY.get() && rodCount > 0) {
            Component stabilityLabel = Component.translatable("gui.colossal_reactors.reactor_controller.stability.label");
            guiGraphics.text(font, stabilityLabel, SIM_PANEL_X, y, SIM_STATS_COLOR, false);
            boolean ok = result.stabilityCoolingSufficient();
            Component state = Component.translatable(
                    ok ? "gui.colossal_reactors.reactor_builder.simulation.stability_stable"
                            : "gui.colossal_reactors.reactor_builder.simulation.stability_unstable");
            int stateColor = ok ? 0xFF00CC00 : 0xFFCC3333;
            guiGraphics.text(font, state, SIM_PANEL_X + font.width(stabilityLabel), y, stateColor, false);
            y += SIM_LINE_HEIGHT;
        }
        return y;
    }

    private int renderSimulationBuildRequirements(GuiGraphicsExtractor guiGraphics, int y) {
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_builder.simulation.required_for_build"),
                SIM_PANEL_X, y, GuiTextColors.PANEL_YELLOW_LIGHT, false);
        y += SIM_LINE_HEIGHT;
        return renderMaterialCounts(guiGraphics, y);
    }

    private int renderMaterialCounts(GuiGraphicsExtractor guiGraphics, int y) {
        ReactorBuildMaterialCounter.BuildMaterialCounts counts = getMaterialCounts();
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_builder.calculate.frame_casings", counts.frameCasings()),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_builder.calculate.face_casings", counts.faceCasings()),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_builder.calculate.rods", counts.rods()),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_builder.calculate.rod_controllers", counts.rodControllers()),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_builder.calculate.heat_sinks", counts.heatSinkCells()),
                SIM_PANEL_X, y, SIM_STATS_COLOR, false);
        if (HeatSinkLoader.requiresLiquidPlacement(menu.getHeatSinkIndex()) && counts.heatSinkCells() > 0) {
            y += SIM_LINE_HEIGHT;
            guiGraphics.text(font,
                    Component.translatable("gui.colossal_reactors.reactor_builder.calculate.fluid_mb", counts.estimatedFluidMb()),
                    SIM_PANEL_X, y, SIM_STATS_COLOR, false);
            y += SIM_LINE_HEIGHT;
        }
        return y;
    }

    private ReactorBuildMaterialCounter.BuildMaterialCounts getMaterialCounts() {
        int sizeLeft = menu.getSizeRight();
        int sizeRight = menu.getSizeLeft();
        return ReactorBuildMaterialCounter.estimate(
                sizeLeft, sizeRight, menu.getSizeH(), menu.getSizeD(),
                menu.getRodPattern(), menu.getPatternMode(), menu.getHeatSinkIndex(), menu.isOpenTop());
    }

    /** Current coolant for simulation: null = None (index 0), else the selected definition. */
    private CoolantDefinition getSimulationCoolantDef() {
        if (simulationCoolantIndex <= 0) return null;
        List<Identifier> ids = getOrderedCoolantIds();
        int idx = simulationCoolantIndex - 1;
        if (idx >= ids.size()) return null;
        return CoolantLoader.get(ids.get(idx));
    }

    /**
     * Same simulation as the real reactor: same formula for RF and fuel consumption (fuel units/tick, same as uranium #c:ingots/uranium).
     * Parameters must match {@link ReactorBuildLogic}: sizeLeft/sizeRight/sizeHeight/sizeDepth from entity (menu sizeData 0,1,2,3).
     * Menu display swaps L/R: getSizeRight() = sizeData(0) = entity.sizeLeft, getSizeLeft() = sizeData(1) = entity.sizeRight.
     */
    private ReactorSimulation.SimulationResult getSimulationResult() {
        if (minecraft == null || minecraft.level == null) {
            return new ReactorSimulation.SimulationResult(0, 0, 0, 0, 0, 0, 0, true);
        }
        var ra = minecraft.level.registryAccess();
        CoolantDefinition coolantDef = getSimulationCoolantDef();
        List<Identifier> fuelIds = getOrderedFuelIds();
        Identifier simulationFuelId = fuelIds.isEmpty() ? null : fuelIds.get(Math.min(simulationFuelIndex, fuelIds.size() - 1));
        // Same order as ReactorBuildLogic: entity.sizeLeft, entity.sizeRight, entity.sizeHeight, entity.sizeDepth (sizeData 0,1,2,3)
        int sizeLeft = menu.getSizeRight();
        int sizeRight = menu.getSizeLeft();
        int sizeHeight = menu.getSizeH();
        int sizeDepth = menu.getSizeD();
        return ReactorBuilderSimulation.run(ra,
                sizeLeft, sizeRight, sizeHeight, sizeDepth,
                menu.getRodPattern(), menu.getPatternMode(), menu.getHeatSinkIndex(),
                simulationFuelId, coolantDef);
    }

    private static String formatFuelPerTickSim(int hundredths) {
        if (hundredths <= 0) return "0";
        int intPart = hundredths / 100;
        if (intPart >= 1000) {
            return GuiNumberFormat.format(hundredths / 100.0);
        }
        int frac = hundredths % 100;
        if (frac == 0) return String.valueOf(intPart);
        String fracStr = String.format("%02d", frac).replaceFirst("0+$", "");
        return intPart + "." + fracStr;
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        if (viewMode == ViewMode.SIMULATION) {
            // Scissor coords are GUI-local; pose is already translated by extractContents.
            guiGraphics.enableScissor(SIM_PANEL_X, SIM_PANEL_Y, GuiPanelScrollbar.TEXT_RIGHT, GuiPanelScrollbar.TEXT_BOTTOM);
            int contentHeight = renderSimulationPanel(guiGraphics);
            guiGraphics.disableScissor();
            simulationScrollbar.setContentHeight(contentHeight);
            Component simTitle = Component.translatable("gui.colossal_reactors.reactor_builder.simulation");
            int titleX = (imageWidth - font.width(simTitle)) / 2;
            guiGraphics.text(font, simTitle, titleX, 6, GuiTextColors.TITLE, false);
            return;
        }
        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.text(font, title, titleX, 6, GuiTextColors.TITLE, false);

        // Size: (L+R=X) x (Y+1) x (Z+1) — X and Y/Z as block count (+1)
        int totalW = menu.getSizeLeft() + menu.getSizeRight();
        Component sizeLabel = Component.translatable("gui.colossal_reactors.reactor_builder.size",
                menu.getSizeLeft(), menu.getSizeRight(), totalW + 1, menu.getSizeH() + 1, menu.getSizeD() + 1);
        int sizeX = (imageWidth - font.width(sizeLabel)) / 2;
        guiGraphics.text(font, sizeLabel, sizeX, SIZE_LABEL_Y, GuiTextColors.TITLE, false);

        // Build progress (aligned with bottom of row-2 arrows). Warning on separate line at former row-1 Y.
        int buildingTextY = ROW2_Y + BUTTON_H - font.lineHeight;
        if (menu.isBuildProgressVisible()) {
            int percent = menu.getBuildProgressPercent();
            float t = Math.max(0, Math.min(100, percent)) / 100f;
            int r = (int) (255 * (1f - t));
            int g = (int) (255 * t);
            int progressColor = 0xFF000000 | (r << 16) | (g << 8);
            Component prefix = Component.translatable("gui.colossal_reactors.reactor_builder.building_progress_label");
            guiGraphics.text(font, prefix, WARNING_RIGHT_X, buildingTextY, GuiTextColors.TITLE, false);
            int pctX = WARNING_RIGHT_X + font.width(prefix);
            guiGraphics.text(font, Component.literal(percent + "%"), pctX, buildingTextY, progressColor, false);
        }
        if (menu.isInvalidBlocksDetected()) {
            guiGraphics.text(font,
                    Component.translatable("gui.colossal_reactors.reactor_builder.warning.invalid_blocks"),
                    WARNING_RIGHT_X, RIGHT_ROW_WARNING_MESSAGE_Y, GuiTextColors.ERROR, false);
        }
    }

    @Override
    public void extractContents(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.extractContents(guiGraphics, mouseX, mouseY, partialTick);
        if (viewMode == ViewMode.BUILDER) {
            renderMarkInputGhosts(guiGraphics);
        }
    }

    /** Ghost item + dark overlay in empty buffer slots with a mark-input filter (same as Pattern Crafter). */
    private void renderMarkInputGhosts(GuiGraphicsExtractor guiGraphics) {
        if (menu.getBlockEntity() == null) return;
        for (int i = 0; i < ReactorBuilderMenu.BUFFER_SLOTS; i++) {
            if (!menu.hasMarkInputFilter(i)) continue;
            Slot guiSlot = menu.getSlot(i);
            if (!guiSlot.getItem().isEmpty()) continue;
            ItemStack ghost = menu.getMarkInputFilter(i);
            if (ghost.isEmpty()) continue;
            renderMarkInputGhostItem(guiGraphics, ghost, guiSlot.x, guiSlot.y);
        }
    }

    private void renderMarkInputGhostItem(GuiGraphicsExtractor guiGraphics, ItemStack stack, int sx, int sy) {
        guiGraphics.pose().pushMatrix();
        guiGraphics.pose().translate(leftPos + sx, topPos + sy);
        guiGraphics.item(stack, 0, 0);
        guiGraphics.fill(0, 0, 16, 16, 0x80000000);
        guiGraphics.pose().popMatrix();
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
        super.extractTooltip(guiGraphics, mouseX, mouseY);
        if (viewMode != ViewMode.BUILDER) return;
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
            guiGraphics.setTooltipForNextFrame(font, tooltipLines, mouseX, mouseY);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (viewMode != ViewMode.BUILDER) {
            extractContents(guiGraphics, mouseX, mouseY, partialTick);
            simulationScrollbar.render(guiGraphics, leftPos, topPos);
            extractCarriedItem(guiGraphics, mouseX, mouseY);
            extractSnapbackItem(guiGraphics);
            extractTooltip(guiGraphics, mouseX, mouseY);
        } else {
            super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (viewMode != ViewMode.BUILDER && event.isEscape()) {
            switchToBuilderView();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        simulationScrollbar.disposeButtons(this::removeWidget);
        menu.setHideAllSlotsForSimulationView(false);
        super.onClose();
    }
}
