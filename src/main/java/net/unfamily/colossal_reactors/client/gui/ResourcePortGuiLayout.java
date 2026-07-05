package net.unfamily.colossal_reactors.client.gui;

/**
 * Shared layout constants for resource port and heating coil GUIs ({@code resource_port.png}, 176×176).
 * Tank fill areas on {@code resource_port.png}: gas (11,21)–(22,74), liquid (37,21)–(48,74).
 */
public final class ResourcePortGuiLayout {

    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 176;

    public static final int GAS_LEFT = 11;
    public static final int GAS_TOP = 21;
    public static final int GAS_RIGHT = 22;
    public static final int GAS_BOTTOM = 74;
    public static final int GAS_WIDTH = GAS_RIGHT - GAS_LEFT + 1;
    public static final int GAS_HEIGHT = GAS_BOTTOM - GAS_TOP + 1;

    public static final int GAS_BAR_X = GAS_LEFT;
    public static final int GAS_BAR_Y = GAS_TOP;
    public static final int LIQUID_BAR_X = 37;
    public static final int LIQUID_BAR_Y = 21;
    public static final int BAR_FILL_W = GAS_WIDTH;
    public static final int BAR_FILL_H = GAS_HEIGHT;

    public static final int ITEM_SLOT_X = 63;
    public static final int ITEM_SLOT_Y = 39;
    public static final int ITEM_SLOT_SIZE = 18;
    public static final int TOGGLE_SLOT_GAP = 10;

    public static final int DUMP_W = 14;
    public static final int DUMP_H = 12;
    public static final int DUMP_GAP_BELOW = 3;

    public static final int GAS_DUMP_X = GAS_BAR_X + (BAR_FILL_W - DUMP_W) / 2;
    public static final int GAS_DUMP_Y = GAS_BAR_Y + BAR_FILL_H + DUMP_GAP_BELOW;
    public static final int LIQUID_DUMP_X = LIQUID_BAR_X + (BAR_FILL_W - DUMP_W) / 2;
    public static final int LIQUID_DUMP_Y = LIQUID_BAR_Y + BAR_FILL_H + DUMP_GAP_BELOW;

    public static final int CLOSE_SIZE = 12;
    public static final int CLOSE_X = GUI_WIDTH - CLOSE_SIZE - 5;
    public static final int CLOSE_Y = 5;

    /** Stacked control column: mode, medium, filter (reactor). */
    public static final int TOGGLE_BTN_W = 58;
    public static final int TOGGLE_BTN_H = 16;
    public static final int TOGGLE_GAP = 3;
    public static final int TOGGLE_X = ITEM_SLOT_X + ITEM_SLOT_SIZE + TOGGLE_SLOT_GAP;
    public static final int TOGGLE_ROW0_Y = 17;

    public static final int TOGGLE_ROW_MODE = 0;
    public static final int TOGGLE_ROW_MEDIUM = 1;
    public static final int TOGGLE_ROW_FILTER = 2;

    public static final int FILTER_BTN_W = TOGGLE_BTN_W;
    public static final int FILTER_BTN_H = TOGGLE_BTN_H;
    public static final int FILTER_X = TOGGLE_X;
    public static final int FILTER_Y = toggleY(TOGGLE_ROW_FILTER);

    public static final int MASK_COLOR = 0xFFC6C6C6;
    public static final int MASK_INSET = 1;

    public static final int LIQUID_FRAME_RIGHT = LIQUID_BAR_X + BAR_FILL_W + MASK_INSET;

    public static int barFillPixels(long amount, long capacity, int barHeight) {
        if (capacity <= 0 || amount <= 0) {
            return 0;
        }
        return (int) Math.min(barHeight, amount * barHeight / capacity);
    }

    private ResourcePortGuiLayout() {}

    public static int toggleY(int row) {
        return TOGGLE_ROW0_Y + row * (TOGGLE_BTN_H + TOGGLE_GAP);
    }

    public static int gasBarFillLeft(int guiX) {
        return guiX + GAS_BAR_X;
    }

    public static int gasBarFillTop(int guiY) {
        return guiY + GAS_BAR_Y;
    }

    public static int gasBarFillBottom(int guiY) {
        return guiY + GAS_BAR_Y + BAR_FILL_H;
    }

    public static int liquidBarFillLeft(int guiX) {
        return guiX + LIQUID_BAR_X;
    }

    public static int liquidBarFillTop(int guiY) {
        return guiY + LIQUID_BAR_Y;
    }

    public static int liquidBarFillBottom(int guiY) {
        return guiY + LIQUID_BAR_Y + BAR_FILL_H;
    }

    public static int maskGasLeft(int guiX) {
        return guiX + GAS_BAR_X - MASK_INSET;
    }

    public static int maskGasTop(int guiY) {
        return guiY + GAS_BAR_Y - MASK_INSET;
    }

    public static int maskGasRight(int guiX) {
        return guiX + GAS_BAR_X + BAR_FILL_W + MASK_INSET;
    }

    public static int maskGasBottom(int guiY) {
        return guiY + GAS_BAR_Y + BAR_FILL_H + MASK_INSET;
    }

    public static void fillItemSlotMask(net.minecraft.client.gui.GuiGraphics g, int guiX, int guiY) {
        int sx = guiX + ITEM_SLOT_X - MASK_INSET;
        int sy = guiY + ITEM_SLOT_Y - MASK_INSET;
        g.fill(sx, sy, sx + ITEM_SLOT_SIZE + 2 * MASK_INSET, sy + ITEM_SLOT_SIZE + 2 * MASK_INSET, MASK_COLOR);
    }
}
