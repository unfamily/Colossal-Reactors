package net.unfamily.colossal_reactors.compat.emi;

import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.unfamily.colossal_reactors.compat.RecipeViewerLayout;

/** Shared slot/background layout for EMI recipes (matches JEI). */
public final class ColossalEmiRecipeLayout {
    private ColossalEmiRecipeLayout() {}

    public static void addStandardBackground(
            WidgetHolder widgets, int width, int height, boolean drawInputSlot, boolean drawOutputSlot) {
        widgets.addDrawable(
                0,
                0,
                width,
                height,
                (graphics, mouseX, mouseY, delta) ->
                        RecipeViewerLayout.draw(graphics, 0, 0, drawInputSlot, drawOutputSlot));
    }

    public static void addTextDrawer(WidgetHolder widgets, int width, int height, TextDrawer drawer) {
        widgets.addDrawable(0, 0, width, height, (graphics, mouseX, mouseY, delta) -> drawer.draw(graphics));
    }

    @FunctionalInterface
    public interface TextDrawer {
        void draw(GuiGraphics graphics);
    }

    public static int inSlotX() {
        return RecipeViewerLayout.SLOT_IN_X;
    }

    public static int inSlotY() {
        return RecipeViewerLayout.SLOT_IN_Y;
    }

    public static int outSlotX() {
        return RecipeViewerLayout.SLOT_OUT_X;
    }

    public static int outSlotY() {
        return RecipeViewerLayout.SLOT_OUT_Y;
    }
}
