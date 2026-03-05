package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.menu.ReactorBuilderMenu;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.network.ReactorBuilderBuildPayload;
import net.unfamily.colossal_reactors.network.ReactorBuilderHeatSinkPayload;
import net.unfamily.colossal_reactors.network.ReactorBuilderOptionPayload;
import net.unfamily.colossal_reactors.network.ReactorBuilderSizePayload;
import net.unfamily.colossal_reactors.network.ReactorPreviewPayload;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.reactor.ReactorSimulation;

import java.util.ArrayList;
import java.util.List;

/**
 * Reactor Builder GUI. Background reactor_builder.png 230x230; slots offset +27px right from left edge.
 * Like Deep Drawer Extractor: Simulation is a view mode (isSimulationView) on the same screen, not a separate Screen.
 */
public class ReactorBuilderScreen extends AbstractContainerScreen<ReactorBuilderMenu> {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/reactor_builder.png");
    private static final ResourceLocation SIMULATION_BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/reactor_controller.png");

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 230;

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

    /**
     * 6 buttons: 3 cols x 2 rows (Heat Sink, Pattern, PatternMode, OpenTop, Simulation, Build/Stop); warning between the two rows.
     */
    private static final int RIGHT_EDGE_INSET = 12;
    private static final int RIGHT_BUTTON_W = 42;
    private static final int RIGHT_BLOCK_X = GUI_WIDTH - RIGHT_EDGE_INSET - (3 * RIGHT_BUTTON_W + 2 * GAP);
    private static final int RIGHT_BUTTON_H = BUTTON_H;
    private static final int RIGHT_COL0_X = RIGHT_BLOCK_X;
    private static final int RIGHT_COL1_X = RIGHT_BLOCK_X + RIGHT_BUTTON_W + GAP;
    private static final int RIGHT_COL2_X = RIGHT_BLOCK_X + 2 * (RIGHT_BUTTON_W + GAP);
    private static final int RIGHT_ROW0_Y = 32;
    /** Second row kept where the old third row was (60). */
    private static final int RIGHT_ROW1_Y = 60;
    /** Warning text: X = right block left edge; Y computed so text bottom aligns with bottom of right arrow button (ROW2_Y + BUTTON_H). */
    private static final int WARNING_RIGHT_X = RIGHT_BLOCK_X;

    /** Simulation view panel (same layout as reactor controller). */
    private static final int SIM_PANEL_X = 16;
    private static final int SIM_PANEL_Y = 29;
    private static final int SIM_LINE_HEIGHT = 12;
    private static final int SIM_TEXT_COLOR = 0xFFFFFF;

    /** Coolant/Fuel cycle buttons: same horizontal inset each side (12px), reduced gap, width split equally. */
    private static final int SIM_BUTTONS_HORIZONTAL_INSET = 12;
    private static final int SIM_BUTTONS_GAP = 2;
    private static final int COOLANT_BUTTON_W = (GUI_WIDTH - SIM_BUTTONS_HORIZONTAL_INSET * 2 - SIM_BUTTONS_GAP) / 2;
    private static final int FUEL_BUTTON_W = COOLANT_BUTTON_W;
    private static final int COOLANT_BUTTON_H = 20;
    private static final int COOLANT_BUTTON_RIGHT_INSET = SIM_BUTTONS_HORIZONTAL_INSET;
    private static final int COOLANT_BUTTON_BOTTOM_INSET = 13;

    /** Simulation view mode: same screen, different content (like Deep Drawer how to use / valid keys). */
    private boolean isSimulationView = false;
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
    /** Right block buttons: 0=Heat Sink, 1=Pattern, 2=PatternMode, 3=OpenTop, 4=Simulation, 5=Build/Stop. */
    private final Button[] rightBlockButtons = new Button[6];
    /** Shown only in simulation view: cycles coolant type (same position as Reboot in controller). */
    private Button coolantCycleButton;
    /** Shown only in simulation view: cycles fuel type (next to coolant). */
    private Button fuelCycleButton;

    public ReactorBuilderScreen(ReactorBuilderMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        imageWidth = GUI_WIDTH;
        imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        closeButton = Button.builder(Component.literal("\u2715"), b -> onCloseButtonClicked())
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
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

        // Preview below the 4 arrows, centered with arrow group
        buttonPreview = Button.builder(Component.translatable("gui.colossal_reactors.reactor_builder.preview"), b -> {
            if (!menu.getBlockPos().equals(BlockPos.ZERO))
                PacketDistributor.sendToServer(new ReactorPreviewPayload(menu.getBlockPos()));
        })
                .bounds(leftPos + PREVIEW_BUTTON_X, topPos + PREVIEW_BUTTON_Y, PREVIEW_BUTTON_W, BUTTON_H)
                .build();
        buttonPreview.setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.reactor_builder.preview.tooltip")));
        addRenderableWidget(buttonPreview);

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
        updateWidgetVisibility();
    }

    private static List<ResourceLocation> getOrderedCoolantIds() {
        List<ResourceLocation> ids = new ArrayList<>(CoolantLoader.getAll().keySet());
        ids.sort(ResourceLocation::compareTo);
        return ids;
    }

    private static List<ResourceLocation> getOrderedFuelIds() {
        List<ResourceLocation> ids = new ArrayList<>(FuelLoader.getAll().keySet());
        ids.sort(ResourceLocation::compareTo);
        return ids;
    }

    /** Index of uranium in ordered fuel list, or 0 if not found / empty. */
    private static int getUraniumIndex() {
        List<ResourceLocation> ids = getOrderedFuelIds();
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
        if (minecraft != null && minecraft.getSoundManager() != null)
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (next) {
            simulationCoolantIndex = (simulationCoolantIndex + 1) % options;
        } else {
            simulationCoolantIndex = simulationCoolantIndex <= 0 ? options - 1 : simulationCoolantIndex - 1;
        }
        updateCoolantButtonLabel();
    }

    private void updateCoolantButtonLabel() {
        if (coolantCycleButton == null) return;
        List<ResourceLocation> ids = getOrderedCoolantIds();
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
        ResourceLocation id = ids.get(idx);
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
        List<ResourceLocation> ids = getOrderedFuelIds();
        if (ids.isEmpty()) return;
        if (minecraft != null && minecraft.getSoundManager() != null)
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        if (next) {
            simulationFuelIndex = (simulationFuelIndex + 1) % ids.size();
        } else {
            simulationFuelIndex = simulationFuelIndex <= 0 ? ids.size() - 1 : simulationFuelIndex - 1;
        }
        updateFuelButtonLabel();
    }

    private void updateFuelButtonLabel() {
        if (fuelCycleButton == null) return;
        List<ResourceLocation> ids = getOrderedFuelIds();
        Component clickHint = Component.translatable("gui.colossal_reactors.reactor_builder.simulation.click_hint");
        MutableComponent fuelTitle = Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_label");
        if (ids.isEmpty()) {
            fuelCycleButton.setMessage(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_uranium"));
            fuelCycleButton.setTooltip(Tooltip.create(fuelTitle.append(Component.literal("\n")).append(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_tooltip", "—", "—")).append(Component.literal("\n")).append(clickHint)));
            return;
        }
        if (simulationFuelIndex >= ids.size()) simulationFuelIndex = 0;
        ResourceLocation id = ids.get(simulationFuelIndex);
        var ra = minecraft != null && minecraft.level != null ? minecraft.level.registryAccess() : null;
        Component label = getFuelDisplayName(id, ra);
        MutableComponent tooltip = fuelTitle.copy().append(Component.literal("\n")).append(getFuelTooltip(id, ra)).append(Component.literal("\n")).append(clickHint);
        fuelCycleButton.setMessage(label);
        fuelCycleButton.setTooltip(Tooltip.create(tooltip));
    }

    /** Tooltip line: Consumes: X, Produces: X (input item and waste item names). */
    private static Component getFuelTooltip(ResourceLocation fuelId, net.minecraft.core.RegistryAccess ra) {
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
    private static Component getFuelDisplayName(ResourceLocation fuelId, net.minecraft.core.RegistryAccess ra) {
        if (ra != null) {
            ItemStack stack = FuelLoader.getFirstInputStack(fuelId, ra);
            if (!stack.isEmpty()) return stack.getHoverName();
        }
        if (ReactorRodBlockEntity.URANIUM_FUEL_ID.equals(fuelId))
            return Component.translatable("gui.colossal_reactors.reactor_builder.simulation.fuel_uranium");
        return Component.literal(fuelId.getPath());
    }

    private void onCloseButtonClicked() {
        if (isSimulationView) {
            if (minecraft != null && minecraft.getSoundManager() != null)
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            switchToBuilderView();
        } else {
            if (minecraft != null && minecraft.getSoundManager() != null)
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (minecraft != null && minecraft.player != null) minecraft.player.closeContainer();
        }
    }

    private void switchToSimulationView() {
        isSimulationView = true;
        updateWidgetVisibility();
    }

    private void switchToBuilderView() {
        isSimulationView = false;
        updateWidgetVisibility();
    }

    private void updateWidgetVisibility() {
        boolean showBuilder = !isSimulationView;
        buttonUp.visible = showBuilder;
        buttonLeft.visible = showBuilder;
        buttonRight.visible = showBuilder;
        buttonDown.visible = showBuilder;
        buttonPreview.visible = showBuilder;
        for (Button b : rightBlockButtons) b.visible = showBuilder;
        if (coolantCycleButton != null) {
            coolantCycleButton.visible = isSimulationView;
            if (isSimulationView) updateCoolantButtonLabel();
        }
        if (fuelCycleButton != null) {
            fuelCycleButton.visible = isSimulationView;
            if (isSimulationView) updateFuelButtonLabel();
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
        BlockPos pos = menu.getBlockPos();
        if (index == 4) {
            switchToSimulationView();
            return;
        }
        if (pos.equals(BlockPos.ZERO)) return;
        if (index == 0) {
            PacketDistributor.sendToServer(new ReactorBuilderHeatSinkPayload(pos, true));  // left click = next
            return;
        }
        if (index == 1) PacketDistributor.sendToServer(new ReactorBuilderOptionPayload(pos, 1, true));  // left = next
        if (index == 2) PacketDistributor.sendToServer(new ReactorBuilderOptionPayload(pos, 2, true));
        if (index == 3) PacketDistributor.sendToServer(new ReactorBuilderOptionPayload(pos, 0, true));
        if (index == 5) PacketDistributor.sendToServer(new ReactorBuilderBuildPayload(pos));  // Build/Stop
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
        // Button 3: Open top
        rightBlockButtons[3].setTooltip(Tooltip.create(
                Component.translatable(menu.isOpenTop() ? "gui.colossal_reactors.reactor_builder.tooltip.open_top.open" : "gui.colossal_reactors.reactor_builder.tooltip.open_top.closed")
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_left"))
                        .append(Component.literal("\n"))
                        .append(Component.translatable("gui.colossal_reactors.reactor_builder.tooltip.heat_sink_right"))));
        rightBlockButtons[3].setMessage(getRightButtonLabel(3));
        // Button 4: Simulation
        rightBlockButtons[4].setMessage(getRightButtonLabel(4));
        rightBlockButtons[4].setTooltip(Tooltip.create(Component.translatable("gui.colossal_reactors.reactor_builder.simulation.tooltip")));
        // Button 5: Build/Stop
        rightBlockButtons[5].setMessage(getRightButtonLabel(5));
        rightBlockButtons[5].setTooltip(Tooltip.create(Component.translatable(
                menu.isBuilding() ? "gui.colossal_reactors.reactor_builder.stop.tooltip" : "gui.colossal_reactors.reactor_builder.build.tooltip")));
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
            if (isSimulationView && coolantCycleButton != null && coolantCycleButton.visible && isInWidget(x, y, coolantCycleButton)) {
                cycleSimulationCoolant(false);
                return true;
            }
            if (isSimulationView && fuelCycleButton != null && fuelCycleButton.visible && isInWidget(x, y, fuelCycleButton)) {
                cycleSimulationFuel(false);
                return true;
            }
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
                    PacketDistributor.sendToServer(new ReactorBuilderHeatSinkPayload(pos, false));  // right click = previous
                return true;
            }
            if (isInRightBlockButton(x, y, 1)) {
                BlockPos pos = menu.getBlockPos();
                if (!pos.equals(BlockPos.ZERO))
                    PacketDistributor.sendToServer(new ReactorBuilderOptionPayload(pos, 1, false));  // right = previous
                return true;
            }
            if (isInRightBlockButton(x, y, 2)) {
                BlockPos pos = menu.getBlockPos();
                if (!pos.equals(BlockPos.ZERO))
                    PacketDistributor.sendToServer(new ReactorBuilderOptionPayload(pos, 2, false));
                return true;
            }
            if (isInRightBlockButton(x, y, 3)) {
                BlockPos pos = menu.getBlockPos();
                if (!pos.equals(BlockPos.ZERO))
                    PacketDistributor.sendToServer(new ReactorBuilderOptionPayload(pos, 0, false));  // open top
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
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
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        if (isSimulationView) {
            guiGraphics.blit(SIMULATION_BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);
            renderSimulationPanel(guiGraphics);
        } else {
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
    }

    private void renderSimulationPanel(GuiGraphics guiGraphics) {
        int y = topPos + SIM_PANEL_Y;
        Component statusKey = Component.translatable("gui.colossal_reactors.reactor_builder.simulation");
        Component statusLine = Component.translatable("gui.colossal_reactors.reactor_controller.status", statusKey);
        guiGraphics.drawString(font, statusLine, leftPos + SIM_PANEL_X, y, SIM_TEXT_COLOR, false);
        y += SIM_LINE_HEIGHT;

        // Simulator is always "ON": run virtual tick and show computed stats
        ReactorSimulation.SimulationResult result = getSimulationResult();
        int rodCount = result.rodCount();
        int rodColumns = result.rodColumns();
        int coolantBlocks = result.coolantBlockCount();
        int energyPerTick = result.rfPerTick();
        int coolantConsumed = result.coolantConsumedPerTick();
        int steamPerTick = result.steamPerTick();
        String fuelStr = formatFuelPerTickSim(result.fuelPerTickHundredths());
        int stabilityPermille = result.stabilityPermille();

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", rodCount, rodColumns),
                leftPos + SIM_PANEL_X, y, SIM_TEXT_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_blocks", coolantBlocks),
                leftPos + SIM_PANEL_X, y, SIM_TEXT_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.energy_production", energyPerTick),
                leftPos + SIM_PANEL_X, y, SIM_TEXT_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.water_consume", coolantConsumed),
                leftPos + SIM_PANEL_X, y, SIM_TEXT_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.steam_production", steamPerTick),
                leftPos + SIM_PANEL_X, y, SIM_TEXT_COLOR, false);
        y += SIM_LINE_HEIGHT;
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_units", fuelStr),
                leftPos + SIM_PANEL_X, y, SIM_TEXT_COLOR, false);
        y += SIM_LINE_HEIGHT;
        if (Boolean.TRUE.equals(Config.REACTOR_UNSTABILITY.get())) {
            Component label = Component.translatable("gui.colossal_reactors.reactor_controller.stability.label");
            guiGraphics.drawString(font, label, leftPos + SIM_PANEL_X, y, SIM_TEXT_COLOR, false);
            String stabilityStr = String.format("%.1f%%", stabilityPermille / 10.0);
            int stabilityColor = stabilityPermille >= 1000 ? 0x00FF00 : (stabilityPermille <= 0 ? 0xFF0000 : 0xFFFF00);
            guiGraphics.drawString(font, stabilityStr, leftPos + SIM_PANEL_X + font.width(label), y, stabilityColor, false);
        }
    }

    /** Current coolant for simulation: null = None (index 0), else the selected definition. */
    private CoolantDefinition getSimulationCoolantDef() {
        if (simulationCoolantIndex <= 0) return null;
        List<ResourceLocation> ids = getOrderedCoolantIds();
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
            return new ReactorSimulation.SimulationResult(0, 0, 0, 0, 0, 0, 0, 1000);
        }
        var ra = minecraft.level.registryAccess();
        CoolantDefinition coolantDef = getSimulationCoolantDef();
        // Same order as ReactorBuildLogic: entity.sizeLeft, entity.sizeRight, entity.sizeHeight, entity.sizeDepth (sizeData 0,1,2,3)
        int sizeLeft = menu.getSizeRight();
        int sizeRight = menu.getSizeLeft();
        int sizeHeight = menu.getSizeH();
        int sizeDepth = menu.getSizeD();
        return ReactorBuilderSimulation.run(ra,
                sizeLeft, sizeRight, sizeHeight, sizeDepth,
                menu.getRodPattern(), menu.getPatternMode(), menu.getHeatSinkIndex(),
                coolantDef);
    }

    private static String formatFuelPerTickSim(int hundredths) {
        if (hundredths <= 0) return "0";
        int intPart = hundredths / 100;
        int frac = hundredths % 100;
        if (frac == 0) return String.valueOf(intPart);
        String fracStr = String.format("%02d", frac).replaceFirst("0+$", "");
        return intPart + "." + fracStr;
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (isSimulationView) {
            Component simTitle = Component.translatable("gui.colossal_reactors.reactor_builder.simulation");
            int titleX = leftPos + (imageWidth - font.width(simTitle)) / 2;
            int titleY = topPos + 6;
            guiGraphics.drawString(font, simTitle, titleX, titleY, 0x404040, false);
            return;
        }
        updateButtonTooltips();

        int titleX = (imageWidth - font.width(title)) / 2;
        guiGraphics.drawString(font, title, titleX, 6, 0x404040, false);

        // Size: (L+R=X) x (Y+1) x (Z+1) — X and Y/Z as block count (+1)
        int totalW = menu.getSizeLeft() + menu.getSizeRight();
        Component sizeLabel = Component.translatable("gui.colossal_reactors.reactor_builder.size",
                menu.getSizeLeft(), menu.getSizeRight(), totalW + 1, menu.getSizeH() + 1, menu.getSizeD() + 1);
        int sizeX = (imageWidth - font.width(sizeLabel)) / 2;
        guiGraphics.drawString(font, sizeLabel, sizeX, SIZE_LABEL_Y, 0x404040, false);

        // Warning (red): empty by default; when build found invalid blocks, show message. Bottom aligned with right arrow button.
        Component warning = menu.isInvalidBlocksDetected()
                ? Component.translatable("gui.colossal_reactors.reactor_builder.warning.invalid_blocks")
                : Component.translatable("gui.colossal_reactors.reactor_builder.warning");
        if (!warning.getString().isEmpty()) {
            int warningY = ROW2_Y + BUTTON_H - font.lineHeight;
            guiGraphics.drawString(font, warning, WARNING_RIGHT_X, warningY, 0xFF0000, false);
        }
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
        if (isSimulationView) {
            this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
            this.renderBg(guiGraphics, partialTick, mouseX, mouseY);
            this.renderLabels(guiGraphics, mouseX, mouseY);
            for (var renderable : this.renderables) {
                if (renderable instanceof net.minecraft.client.gui.components.Renderable r) {
                    r.render(guiGraphics, mouseX, mouseY, partialTick);
                }
            }
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        } else {
            super.render(guiGraphics, mouseX, mouseY, partialTick);
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (isSimulationView && (keyCode == 256 || keyCode == 1)) {
            switchToBuilderView();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
