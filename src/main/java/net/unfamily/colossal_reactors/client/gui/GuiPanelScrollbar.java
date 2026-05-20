package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.unfamily.colossal_reactors.ColossalReactors;

import java.util.function.Consumer;

/**
 * Scrollbar for reactor controller / simulation text panels (Sound Muffler scroll logic).
 * Black track is drawn in-code (not in {@code reactor_controller.png}); thumb uses {@code scroller.png} only.
 */
public final class GuiPanelScrollbar {

    /** Track right edge on {@code reactor_controller.png} (x + width = 215). */
    public static final int TRACK_RIGHT = 215;
    public static final int TRACK_WIDTH = ReactorControllerGui.SCROLLER_WIDTH;
    public static final int TRACK_X = TRACK_RIGHT - TRACK_WIDTH;
    public static final int TRACK_TOP = 30;
    public static final int TRACK_BOTTOM = 180;

    /** Text panel uses the same vertical band; stops left of the track. */
    public static final int TEXT_TOP = TRACK_TOP;
    public static final int TEXT_BOTTOM = TRACK_BOTTOM;
    public static final int TEXT_RIGHT = TRACK_X;

    public static final int HANDLE_WIDTH = ReactorControllerGui.SCROLLER_WIDTH;
    public static final int HANDLE_HEIGHT = ReactorControllerGui.SCROLLER_HEIGHT;
    public static final int ARROW_BUTTON_SIZE = 12;
    /** Inset of arrow buttons from {@link #TRACK_TOP} / {@link #TRACK_BOTTOM}. */
    private static final int ARROW_EDGE_MARGIN = 2;
    /** Gap between arrow buttons and the black track. */
    private static final int ARROW_TRACK_GAP = 4;

    private static final int TRACK_COLOR = 0xFF000000;
    private static final int SCROLL_STEP = 12;

    private static final ResourceLocation HANDLE_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            ColossalReactors.MODID, "textures/gui/scroller.png");

    private int scrollOffset;
    private int contentHeight;
    private boolean dragging;
    private int dragStartY;
    private int dragStartScrollOffset;

    private Button scrollUpButton;
    private Button scrollDownButton;

    public int getScrollOffset() {
        return scrollOffset;
    }

    public int getTextViewportHeight() {
        return TEXT_BOTTOM - TEXT_TOP;
    }

    public boolean isNeeded() {
        return contentHeight > getTextViewportHeight();
    }

    public int getMaxScrollOffset() {
        return Math.max(0, contentHeight - getTextViewportHeight());
    }

    public void setContentHeight(int height) {
        contentHeight = Math.max(0, height);
        scrollOffset = Math.max(0, Math.min(scrollOffset, getMaxScrollOffset()));
    }

    public void resetScroll() {
        scrollOffset = 0;
    }

    public void createButtons(int leftPos, int topPos, Consumer<Button> addWidget, Runnable onChanged) {
        if (scrollUpButton != null) {
            return;
        }
        int x = leftPos + TRACK_X;
        scrollUpButton = Button.builder(Component.literal("\u25b2"), b -> {
            scrollBy(-SCROLL_STEP, true);
            onChanged.run();
        }).bounds(x, topPos + upButtonY(), ARROW_BUTTON_SIZE, ARROW_BUTTON_SIZE).build();
        scrollDownButton = Button.builder(Component.literal("\u25bc"), b -> {
            scrollBy(SCROLL_STEP, true);
            onChanged.run();
        }).bounds(x, topPos + downButtonY(), ARROW_BUTTON_SIZE, ARROW_BUTTON_SIZE).build();
        addWidget.accept(scrollUpButton);
        addWidget.accept(scrollDownButton);
        updateButtonVisibility();
    }

    /** Removes arrow widgets from the screen; call when the GUI closes or leaves scroll mode. */
    public void disposeButtons(Consumer<Button> removeWidget) {
        if (scrollUpButton != null) {
            removeWidget.accept(scrollUpButton);
            scrollUpButton = null;
        }
        if (scrollDownButton != null) {
            removeWidget.accept(scrollDownButton);
            scrollDownButton = null;
        }
    }

    public void ensureButtons(int leftPos, int topPos, Consumer<Button> addWidget, Runnable onChanged) {
        if (scrollUpButton == null) {
            createButtons(leftPos, topPos, addWidget, onChanged);
        } else {
            updateButtonVisibility();
        }
    }

    public void updateButtonVisibility() {
        boolean show = isNeeded();
        if (scrollUpButton != null) {
            scrollUpButton.visible = show;
            scrollUpButton.active = show && scrollOffset > 0;
        }
        if (scrollDownButton != null) {
            scrollDownButton.visible = show;
            scrollDownButton.active = show && scrollOffset < getMaxScrollOffset();
        }
    }

    public void render(GuiGraphics guiGraphics, int leftPos, int topPos) {
        updateButtonVisibility();
        if (!isNeeded()) {
            return;
        }
        int trackX = leftPos + TRACK_X;
        int trackY = topPos + trackYLocal();
        int trackH = getTrackHeight();
        guiGraphics.fill(trackX, trackY, trackX + TRACK_WIDTH, trackY + trackH, TRACK_COLOR);

        float ratio = getMaxScrollOffset() > 0 ? (float) scrollOffset / getMaxScrollOffset() : 0f;
        int handleY = trackY + (int) (ratio * Math.max(0, trackH - HANDLE_HEIGHT));
        guiGraphics.blit(HANDLE_TEXTURE, trackX, handleY, 0, 0, HANDLE_WIDTH, HANDLE_HEIGHT, HANDLE_WIDTH, HANDLE_HEIGHT);
    }

    private static int upButtonY() {
        return TRACK_TOP + ARROW_EDGE_MARGIN;
    }

    private static int downButtonY() {
        return TRACK_BOTTOM - ARROW_EDGE_MARGIN - ARROW_BUTTON_SIZE;
    }

    private static int trackYLocal() {
        return upButtonY() + ARROW_BUTTON_SIZE + ARROW_TRACK_GAP;
    }

    private static int trackBottomLocal() {
        return downButtonY() - ARROW_TRACK_GAP;
    }

    private static int getTrackHeight() {
        return trackBottomLocal() - trackYLocal();
    }

    public boolean isInPanelArea(double mouseX, double mouseY, int leftPos, int topPos, int contentLeft) {
        boolean inText = mouseX >= leftPos + contentLeft && mouseX < leftPos + TEXT_RIGHT
                && mouseY >= topPos + TEXT_TOP && mouseY < topPos + TEXT_BOTTOM;
        int trackX = leftPos + TRACK_X;
        boolean inScrollbar = mouseX >= trackX && mouseX < trackX + TRACK_WIDTH
                && mouseY >= topPos + TRACK_TOP && mouseY < topPos + TRACK_BOTTOM;
        return inText || inScrollbar;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int leftPos, int topPos) {
        if (!isNeeded()) {
            return false;
        }
        int trackX = leftPos + TRACK_X;
        int trackY = topPos + trackYLocal();
        int trackH = getTrackHeight();
        if (mouseX < trackX || mouseX >= trackX + TRACK_WIDTH
                || mouseY < trackY || mouseY >= trackY + trackH) {
            return false;
        }

        int handleRange = trackH - HANDLE_HEIGHT;
        int handleY = trackY;
        if (handleRange > 0) {
            float ratio = getMaxScrollOffset() > 0 ? (float) scrollOffset / getMaxScrollOffset() : 0f;
            handleY = trackY + (int) (ratio * handleRange);
        }

        if (mouseY >= handleY && mouseY < handleY + HANDLE_HEIGHT) {
            dragging = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
        } else {
            jumpScrollToMouseY((int) mouseY, trackY, handleRange);
            dragging = true;
            dragStartY = (int) mouseY;
            dragStartScrollOffset = scrollOffset;
        }
        updateButtonVisibility();
        return true;
    }

    /** Moves scroll so the thumb aligns with a track click (not on the thumb). */
    private void jumpScrollToMouseY(int mouseY, int trackY, int handleRange) {
        int maxOffset = getMaxScrollOffset();
        if (maxOffset <= 0 || handleRange <= 0) {
            scrollOffset = 0;
            return;
        }
        int clickOnRange = mouseY - trackY - HANDLE_HEIGHT / 2;
        clickOnRange = Math.max(0, Math.min(handleRange, clickOnRange));
        scrollOffset = Math.round((clickOnRange / (float) handleRange) * maxOffset);
    }

    public void mouseReleased() {
        dragging = false;
    }

    public void mouseMoved(double mouseY) {
        if (!dragging || !isNeeded()) {
            return;
        }
        int handleRange = getTrackHeight() - HANDLE_HEIGHT;
        if (handleRange <= 0) {
            return;
        }
        int deltaY = (int) mouseY - dragStartY;
        int maxOffset = getMaxScrollOffset();
        int deltaScroll = Math.round((float) deltaY / handleRange * maxOffset);
        scrollOffset = Math.max(0, Math.min(maxOffset, dragStartScrollOffset + deltaScroll));
        updateButtonVisibility();
    }

    public boolean mouseScrolled(double scrollY) {
        if (!isNeeded()) {
            return false;
        }
        if (scrollY > 0) {
            scrollBy(-SCROLL_STEP, false);
            return true;
        }
        if (scrollY < 0) {
            scrollBy(SCROLL_STEP, false);
            return true;
        }
        return false;
    }

    private void scrollBy(int delta, boolean playSound) {
        int maxOffset = getMaxScrollOffset();
        int next = Math.max(0, Math.min(maxOffset, scrollOffset + delta));
        if (next == scrollOffset) {
            return;
        }
        scrollOffset = next;
        if (playSound) {
            playClick();
        }
        updateButtonVisibility();
    }

    private static void playClick() {
        var mc = net.minecraft.client.Minecraft.getInstance();
        if (mc != null && mc.getSoundManager() != null) {
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        }
    }
}
