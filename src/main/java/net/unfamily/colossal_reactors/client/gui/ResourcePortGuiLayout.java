package net.unfamily.colossal_reactors.client.gui;

/**
 * Shared layout constants for resource port and heating coil GUIs ({@code resource_port.png}, 176×176).
 * Tank fill areas: gas (11,21)–(22,74), liquid (37,21)–(48,74). Full draw rect (no code inset for frame).
 */
public final class ResourcePortGuiLayout {

    public static final int GUI_WIDTH = 176;
    public static final int GUI_HEIGHT = 176;

    /** Gas tank outer bounds (inclusive corners 11,21 and 22,74 on the texture). */
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

    public static final int TOGGLE_BTN_W = 58;
    public static final int TOGGLE_BTN_H = 16;
    public static final int TOGGLE_GAP = 3;
    public static final int TOGGLE_X = ITEM_SLOT_X + ITEM_SLOT_SIZE + TOGGLE_SLOT_GAP;
    public static final int TOGGLE_ROW0_Y = 17;

    /** Toggle column row 0: Insert / Extract / Eject. */
    public static final int TOGGLE_ROW_MODE = 0;
    /**
     * Toggle column row 1: Solid medium (reactor only).
     * Blocked on turbine ports in {@link ResourcePortScreen#applyTurbineLayout()}.
     */
    public static final int TOGGLE_ROW_SOLID = 1;
    /** Toggle column row 2: Liquid medium. On turbine ports, drawn one step up (row 1 Y). */
    public static final int TOGGLE_ROW_LIQUID = 2;
    /** Toggle column row 3: Gas medium. On turbine ports, drawn one step up (row 2 Y). */
    public static final int TOGGLE_ROW_GAS = 3;

    public static final int MASK_COLOR = 0xFFC6C6C6;
    public static final int MASK_INSET = 1;

    /** Right edge of liquid tank frame (+{@link #MASK_INSET} over fill rect). */
    public static final int LIQUID_FRAME_RIGHT = LIQUID_BAR_X + BAR_FILL_W + MASK_INSET;

    /** Fuel/Coolant role button under item slot (reactor only), centered on the slot. */
    public static final int FILTER_GAP_BELOW_SLOT = 4;
    public static final int FILTER_BTN_H = 14;
    /** Min gap from liquid tank fill / frame to the left edge of the filter button. */
    public static final int FILTER_GAP_LIQUID_RENDER = 5;
    public static final int FILTER_GAP_LEFT = 4;
    public static final int FILTER_GAP_TOGGLE_COLUMN = 4;
    private static final int FILTER_SLOT_CENTER_X = ITEM_SLOT_X + ITEM_SLOT_SIZE / 2;
    private static final int FILTER_MIN_LEFT_X = Math.max(
            LIQUID_BAR_X + BAR_FILL_W + FILTER_GAP_LIQUID_RENDER,
            LIQUID_FRAME_RIGHT + FILTER_GAP_LEFT);
    private static final int FILTER_MAX_HALF_W_FROM_LIQUID = FILTER_SLOT_CENTER_X - FILTER_MIN_LEFT_X;
    private static final int FILTER_MAX_HALF_W_FROM_TOGGLE = TOGGLE_X - FILTER_GAP_TOGGLE_COLUMN - FILTER_SLOT_CENTER_X;
    public static final int FILTER_BTN_W = 2 * Math.min(FILTER_MAX_HALF_W_FROM_LIQUID, FILTER_MAX_HALF_W_FROM_TOGGLE);
    public static final int FILTER_X = FILTER_SLOT_CENTER_X - FILTER_BTN_W / 2;
    public static final int FILTER_Y = ITEM_SLOT_Y + ITEM_SLOT_SIZE + FILTER_GAP_BELOW_SLOT;

    private ResourcePortGuiLayout() {}

    public static int toggleY(int row) {
        return TOGGLE_ROW0_Y + row * (TOGGLE_BTN_H + TOGGLE_GAP);
    }

    /**
     * Y offset for a medium/mode toggle button.
     * Turbine ports hide Solid ({@link #TOGGLE_ROW_SOLID}); Liquid and Gas use the slot above (one row up).
     */
    public static int mediumToggleY(int buttonRow, boolean turbinePort) {
        if (turbinePort && buttonRow >= TOGGLE_ROW_LIQUID) {
            return toggleY(buttonRow - 1);
        }
        return toggleY(buttonRow);
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

    /** Covers the item slot frame on turbine ports (18×18 + inset border). */
    public static void fillItemSlotMask(net.minecraft.client.gui.GuiGraphicsExtractor g, int guiX, int guiY) {
        int sx = guiX + ITEM_SLOT_X - MASK_INSET;
        int sy = guiY + ITEM_SLOT_Y - MASK_INSET;
        g.fill(sx, sy, sx + ITEM_SLOT_SIZE + 2 * MASK_INSET, sy + ITEM_SLOT_SIZE + 2 * MASK_INSET, MASK_COLOR);
    }
}
