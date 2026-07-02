package net.unfamily.colossal_reactors.client;

import net.minecraft.core.BlockPos;
import net.unfamily.iskalib.client.marker.MarkRenderer;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks footprint preview markers per builder so only that machine's preview can be cleared
 * (iskalib {@link MarkRenderer} billboard map is global by world position).
 */
public final class BuilderPreviewTracker {

    /** Long-lived markers until the player toggles preview off. */
    public static final int BUILDER_PREVIEW_DURATION_TICKS = 7_200_000;

    private static final Map<BlockPos, Set<BlockPos>> markersByBuilder = new ConcurrentHashMap<>();

    private BuilderPreviewTracker() {}

    public static void addMarker(BlockPos builderOrigin, BlockPos worldPos, int color, int durationTicks) {
        if (worldPos == null) {
            return;
        }
        int duration = durationTicks > 0 ? durationTicks : BUILDER_PREVIEW_DURATION_TICKS;
        if (builderOrigin != null && !builderOrigin.equals(BlockPos.ZERO)) {
            markersByBuilder
                    .computeIfAbsent(builderOrigin.immutable(), k -> ConcurrentHashMap.newKeySet())
                    .add(worldPos.immutable());
        }
        MarkRenderer.getInstance().addBillboardMarker(worldPos, color, duration);
    }

    /** Removes only markers recorded for this builder (not other machines or ephemeral hints). */
    public static void clearForBuilder(BlockPos builderOrigin) {
        if (builderOrigin == null) {
            return;
        }
        Set<BlockPos> worldMarkers = markersByBuilder.remove(builderOrigin.immutable());
        if (worldMarkers == null) {
            return;
        }
        MarkRenderer renderer = MarkRenderer.getInstance();
        for (BlockPos worldPos : worldMarkers) {
            renderer.removeBillboardMarker(worldPos);
        }
    }
}
