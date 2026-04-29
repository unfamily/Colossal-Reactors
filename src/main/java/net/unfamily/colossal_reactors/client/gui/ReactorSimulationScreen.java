package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;

/**
 * Simulation GUI opened from Reactor Builder. Same layout as reactor controller (dark panel, same labels)
 * but without Reboot button. Placeholder values until simulation logic is implemented.
 * ESC or close returns to the builder screen.
 */
public class ReactorSimulationScreen extends Screen {

    private static final Identifier BACKGROUND = Identifier.fromNamespaceAndPath(
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
    private static final int TEXT_COLOR = GuiTextColors.PANEL_WHITE;

    private final Screen parent;

    @Override
    protected void init() {
        super.init();
        int leftPos = (width - GUI_WIDTH) / 2;
        int topPos = (height - GUI_HEIGHT) / 2;
        Button closeButton = Button.builder(Component.literal("\u2715"), b -> {
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
    public boolean keyPressed(KeyEvent event) {
        if (super.keyPressed(event)) return true;
        if (minecraft != null && event.isEscape()) {
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

    /** ARGB: dark overlay only (no blur). */
    private static final int DARKEN_COLOR = 0xF0101010;

    @Override
    public void extractBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, width, height, DARKEN_COLOR);
        int leftPos = (width - GUI_WIDTH) / 2;
        int topPos = (height - GUI_HEIGHT) / 2;

        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, BACKGROUND, leftPos, topPos, 0.0F, 0.0F, GUI_WIDTH, GUI_HEIGHT, GUI_WIDTH, GUI_HEIGHT);

        int titleW = font.width(title);
        int titleX = leftPos + (GUI_WIDTH - titleW) / 2;
        guiGraphics.text(font, title, titleX, topPos + 5, GuiTextColors.TITLE, false);

        int y = topPos + PANEL_Y;
        Component statusKey = Component.translatable("gui.colossal_reactors.reactor_builder.simulation");
        Component statusLine = Component.translatable("gui.colossal_reactors.reactor_controller.status", statusKey);
        guiGraphics.text(font, statusLine, leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.rods", 0, 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.coolant_blocks", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.energy_production", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.water_consume", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.steam_production", 0),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        guiGraphics.text(font,
                Component.translatable("gui.colossal_reactors.reactor_controller.fuel_units", "0"),
                leftPos + PANEL_X, y, TEXT_COLOR, false);
        y += LINE_HEIGHT;

        if (Boolean.TRUE.equals(Config.REACTOR_UNSTABILITY.get())) {
            Component label = Component.translatable("gui.colossal_reactors.reactor_controller.stability.label");
            String stabilityStr = "100.0%";
            int stabilityColor = 0xFF00FF00;
            guiGraphics.text(font, label, leftPos + PANEL_X, y, TEXT_COLOR, false);
            guiGraphics.text(font, stabilityStr, leftPos + PANEL_X + font.width(label), y, stabilityColor, false);
        }
    }
}
