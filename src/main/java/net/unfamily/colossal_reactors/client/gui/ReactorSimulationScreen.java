package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;

/**
 * Simulation GUI opened from Reactor Builder. Same layout as reactor controller (dark panel, same labels)
 * but without Reboot button. Placeholder values until simulation logic is implemented.
 * ESC or close returns to the builder screen.
 */
public class ReactorSimulationScreen extends Screen {

    private static final ResourceLocation BACKGROUND = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/reactor_controller.png");

    private static final int GUI_WIDTH = 230;
    private static final int GUI_HEIGHT = 230;

    /** Close button (X): top right */
    private static final int CLOSE_BUTTON_Y = 5;
    private static final int CLOSE_BUTTON_SIZE = 12;
    private static final int CLOSE_BUTTON_X = GUI_WIDTH - CLOSE_BUTTON_SIZE - 5;

    private static final int PANEL_X = 16;
    private static final int PANEL_Y = 29;
    private static final int LINE_HEIGHT = 12;
    private static final int TEXT_COLOR = 0xFFFFFF;

    private final Screen parent;

    @Override
    protected void init() {
        super.init();
        int leftPos = (width - GUI_WIDTH) / 2;
        int topPos = (height - GUI_HEIGHT) / 2;
        Button closeButton = Button.builder(Component.literal("\u2715"), b -> {
            if (minecraft != null && minecraft.getSoundManager() != null)
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            if (minecraft != null) minecraft.setScreen(parent);
        })
                .bounds(leftPos + CLOSE_BUTTON_X, topPos + CLOSE_BUTTON_Y, CLOSE_BUTTON_SIZE, CLOSE_BUTTON_SIZE)
                .build();
        addRenderableWidget(closeButton);
    }

    public ReactorSimulationScreen(Screen parent) {
        super(Component.translatable("gui.colossal_reactors.reactor_builder.simulation"));
        this.parent = parent;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (super.keyPressed(keyCode, scanCode, modifiers)) return true;
        if (minecraft != null && (keyCode == 256 || keyCode == 1)) { // ESC
            minecraft.setScreen(parent);
            return true;
        }
        return false;
    }

    @Override
    public void onClose() {
        if (minecraft != null) minecraft.setScreen(parent);
        else super.onClose();
    }

    /** ARGB: dark overlay only (no blur). Same as inventory screens; do not call renderBackground() which adds blur/sgranatura. */
    private static final int DARKEN_COLOR = 0xF0101010;

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Do not call renderBackground(): it would add blur/sgranatura. Only draw dark overlay (like Deep Drawer valid keys mode).
        guiGraphics.fill(0, 0, width, height, DARKEN_COLOR);
        int leftPos = (width - GUI_WIDTH) / 2;
        int topPos = (height - GUI_HEIGHT) / 2;

        guiGraphics.blit(BACKGROUND, leftPos, topPos, 0, 0, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        int titleW = font.width(title);
        int titleX = leftPos + (GUI_WIDTH - titleW) / 2;
        guiGraphics.drawString(font, title, titleX, topPos + 5, 0x404040, false);

        int y = topPos + PANEL_Y;
        // Status: fixed "Simulation"
        Component statusKey = Component.translatable("gui.colossal_reactors.reactor_builder.simulation");
        Component statusLine = Component.translatable("gui.colossal_reactors.reactor_controller.status", statusKey);
        guiGraphics.drawString(font, statusLine, leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        // Rods: placeholder 0
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", 0, 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        // Coolant blocks
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_blocks", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        // Energy
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.energy_production", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        // Coolant (first), Exhaust coolant (second)
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.water_consume", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.steam_production", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        // Fuel
        guiGraphics.drawString(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_units", "0"),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        if (Boolean.TRUE.equals(Config.REACTOR_UNSTABILITY.get())) {
            Component label = Component.translatable("gui.colossal_reactors.reactor_controller.stability.label");
            String stabilityStr = "100.0%";
            int stabilityColor = 0x00FF00; // green
            guiGraphics.drawString(font, label, leftPos + PANEL_X, y, TEXT_COLOR, false);
            guiGraphics.drawString(font, stabilityStr, leftPos + PANEL_X + font.width(label), y, stabilityColor, false);
        }
        for (var child : children()) {
            if (child instanceof Renderable r) {
                r.render(guiGraphics, mouseX, mouseY, partialTick);
            }
        }
    }
}
