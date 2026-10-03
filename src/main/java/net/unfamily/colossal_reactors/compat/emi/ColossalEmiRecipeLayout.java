package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.widget.WidgetHolder;
import me.shedaniel.rei.api.client.gui.compat.GuiGraphics;
import net.unfamily.colossal_reactors.compat.jei.JeiRecipeBackgroundDrawable;

/** Shared slot/background layout for EMI recipes (matches JEI). */
public final class ColossalEmiRecipeLayout {
    private ColossalEmiRecipeLayout() {}

    public static void addStandardBackground(WidgetHolder widgets, int width, int height, boolean twoSlotsWithArrow) {
        JeiRecipeBackgroundDrawable bg = new JeiRecipeBackgroundDrawable(width, height, twoSlotsWithArrow);
        widgets.addDrawable(0, 0, width, height, (graphics, mouseX, mouseY, delta) -> bg.draw(graphics, 0, 0));
    }

    public static void addTextDrawer(WidgetHolder widgets, int width, int height, TextDrawer drawer) {
        widgets.addDrawable(0, 0, width, height, (graphics, mouseX, mouseY, delta) -> drawer.draw(graphics));
    }

    @FunctionalInterface
    public interface TextDrawer {
        void draw(GuiGraphics graphics);
    }

    public static int inSlotX() {
        return JeiRecipeBackgroundDrawable.SLOT_IN_X;
    }

    public static int inSlotY() {
        return JeiRecipeBackgroundDrawable.SLOT_IN_Y;
    }

    public static int outSlotX() {
        return JeiRecipeBackgroundDrawable.SLOT_OUT_X;
    }

    public static int outSlotY() {
        return JeiRecipeBackgroundDrawable.SLOT_OUT_Y;
    }
}
