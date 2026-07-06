package net.unfamily.colossal_reactors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.unfamily.colossal_reactors.block.ReactorBuilderBlock;
import net.unfamily.colossal_reactors.block.TurbineBuilderBlock;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;
import net.unfamily.iskalib.client.marker.MarkRenderer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.jetbrains.annotations.Nullable;

/**
 * Per-player client footprint preview: markers scoped by builder block position via iskalib
 * {@link MarkRenderer#addBillboardMarker(BlockPos, BlockPos, int, int)} so overlapping previews
 * from different builders do not clear each other's world positions.
 */
public final class BuilderPreviewTracker {

    public static final int BUILDER_PREVIEW_DURATION_TICKS = 7_200_000;

    private static final int WORLD_POLL_INTERVAL_TICKS = 5;
    private static final int REFRESH_COOLDOWN_TICKS = 20;
    /** Self-heal marker display from layer cache (no server round-trip). */
    private static final int PERIODIC_RECONCILE_INTERVAL_TICKS = 40;

    /** Wrong block overlay (must match server preview payloads). */
    private static final int COLOR_OCCUPIED = 0xE0FF0000;
    private static final int COLOR_ROD = 0xE0FFFF00;
    private static final int COLOR_ROD_CONTROLLER = 0xE0FFFFFF;
    private static final int COLOR_FRAME_EDGE = 0x80FF00FF;
    private static final int COLOR_CLOSURE_DECK = 0xE070D8FF;

    /** Baseline marker priority when multiple layers exist (occupied handled separately). */
    private static final int[] BASELINE_COLOR_PRIORITY = {
            COLOR_ROD_CONTROLLER,
            COLOR_FRAME_EDGE,
            COLOR_CLOSURE_DECK,
            COLOR_ROD,
    };

    private static final Set<BlockPos> activePreviews = ConcurrentHashMap.newKeySet();
    private static final Map<BlockPos, Integer> lastWorldStateHashByBuilder = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Integer> refreshCooldownByBuilder = new ConcurrentHashMap<>();
    /** World changed while a refresh was on cooldown; flush when cooldown expires. */
    private static final Set<BlockPos> pendingWorldRefreshByBuilder = ConcurrentHashMap.newKeySet();
    /** Drops stale marker packets from an older footprint refresh (out-of-order network). */
    private static final Map<BlockPos, Integer> activeFootprintGeneration = new ConcurrentHashMap<>();
    /** All logical marker colors per world cell (red may overlay rod/outline colors). */
    private static final Map<BlockPos, Map<BlockPos, Set<Integer>>> markerLayersByOwner = new ConcurrentHashMap<>();
    /** Layer cache cleared on first marker of a new server footprint batch. */
    private static final Set<BlockPos> pendingLayerResetByOwner = ConcurrentHashMap.newKeySet();

    private static final int INVALID_FOOTPRINT_GENERATION = -1;

    private static long lastPollGameTime = Long.MIN_VALUE;
    private static long lastPeriodicReconcileGameTime = Long.MIN_VALUE;

    private BuilderPreviewTracker() {}

    public static boolean isPreviewActive(BlockPos builderOrigin) {
        return builderOrigin != null && activePreviews.contains(builderOrigin.immutable());
    }

    public static void setPreviewActive(BlockPos builderOrigin, boolean active) {
        if (builderOrigin == null) {
            return;
        }
        BlockPos key = builderOrigin.immutable();
        if (active) {
            activePreviews.add(key);
        } else {
            activePreviews.remove(key);
            clearTrackingForBuilder(key);
            MarkRenderer.getInstance().clearBillboardMarkersForOwner(key);
        }
    }

    /** Clears rendered markers only; keeps layer cache for local reconcile. */
    public static void clearMarkersForBuilder(BlockPos builderOrigin) {
        if (builderOrigin == null) {
            return;
        }
        BlockPos key = builderOrigin.immutable();
        activeFootprintGeneration.put(key, INVALID_FOOTPRINT_GENERATION);
        clearRendererForBuilder(key);
    }

    /**
     * Server footprint batch start: accept only markers with this generation.
     * Layer cache is kept until the first marker of the new batch arrives.
     */
    public static void applyFootprintClear(BlockPos builderOrigin, int footprintGeneration) {
        if (builderOrigin == null) {
            return;
        }
        BlockPos key = builderOrigin.immutable();
        activeFootprintGeneration.put(key, footprintGeneration);
        pendingLayerResetByOwner.add(key);
        clearRendererForBuilder(key);
    }

    public static void deactivateBuilder(BlockPos builderOrigin) {
        setPreviewActive(builderOrigin, false);
    }

    public static void clearAll() {
        MarkRenderer renderer = MarkRenderer.getInstance();
        for (BlockPos key : activePreviews) {
            renderer.clearBillboardMarkersForOwner(key);
        }
        activePreviews.clear();
        lastWorldStateHashByBuilder.clear();
        refreshCooldownByBuilder.clear();
        pendingWorldRefreshByBuilder.clear();
        activeFootprintGeneration.clear();
        markerLayersByOwner.clear();
        pendingLayerResetByOwner.clear();
        lastPollGameTime = Long.MIN_VALUE;
        lastPeriodicReconcileGameTime = Long.MIN_VALUE;
    }

    /**
     * Call when the client is about to request a footprint resync from the server.
     * Clears renderer only (layer cache kept), snapshots world hash, and applies refresh cooldown.
     */
    public static void onFootprintRefreshRequested(Level level, BlockPos builderOrigin) {
        if (level == null || builderOrigin == null) {
            return;
        }
        BlockPos key = builderOrigin.immutable();
        clearMarkersForBuilder(key);
        pruneStaleOccupiedMarkers(level, key);
        reconcileMarkersForBuilder(key);
        seedWorldHash(level, key);
        noteRefreshRequested(key);
    }

    public static void noteRefreshRequested(BlockPos builderOrigin) {
        if (builderOrigin == null) {
            return;
        }
        refreshCooldownByBuilder.put(builderOrigin.immutable(), REFRESH_COOLDOWN_TICKS);
    }

    /** Snapshot occupied blocks inside the footprint so the next world change can be detected. */
    public static void seedWorldHash(Level level, BlockPos builderOrigin) {
        if (level == null || builderOrigin == null) {
            return;
        }
        AABB volume = resolveFootprintVolume(level, builderOrigin);
        if (volume != null) {
            lastWorldStateHashByBuilder.put(builderOrigin.immutable(), computeOccupiedBlocksHash(level, volume));
        }
    }

    /**
     * Re-resolve markers immediately when a block inside an active preview volume changes.
     * Call from client block place/break hooks.
     */
    public static void onBlockInPreviewChanged(Level level, BlockPos worldPos) {
        if (level == null || worldPos == null || activePreviews.isEmpty()) {
            return;
        }
        Vec3 center = Vec3.atCenterOf(worldPos);
        for (BlockPos builder : List.copyOf(activePreviews)) {
            AABB volume = resolveFootprintVolume(level, builder);
            if (volume == null || !volume.contains(center)) {
                continue;
            }
            pruneStaleOccupiedMarkers(level, builder);
            reconcileMarkersForBuilder(builder);
            lastWorldStateHashByBuilder.put(builder, computeOccupiedBlocksHash(level, volume));
        }
    }

    public static void addMarker(BlockPos builderOrigin, BlockPos worldPos, int color, int durationTicks, int footprintGeneration) {
        if (worldPos == null || builderOrigin == null || builderOrigin.equals(BlockPos.ZERO)) {
            return;
        }
        BlockPos owner = builderOrigin.immutable();
        if (footprintGeneration != activeFootprintGeneration.getOrDefault(owner, INVALID_FOOTPRINT_GENERATION)) {
            return;
        }
        if (pendingLayerResetByOwner.remove(owner)) {
            clearLayerCacheForBuilder(owner);
        }
        BlockPos cell = worldPos.immutable();
        Level level = Minecraft.getInstance().level;
        if (level != null && color == COLOR_OCCUPIED) {
            BlockState state = level.getBlockState(cell);
            if (state.isAir() || state.canBeReplaced()) {
                return;
            }
        }
        int duration = durationTicks > 0 ? durationTicks : BUILDER_PREVIEW_DURATION_TICKS;
        markerLayersByOwner
                .computeIfAbsent(owner, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(cell, k -> ConcurrentHashMap.newKeySet())
                .add(color);
        syncResolvedMarker(owner, cell, duration);
    }

    /**
     * Returns builder origins that need a server footprint refresh because blocks inside the preview volume changed.
     */
    public static List<BlockPos> pollBuildersNeedingWorldRefresh(Level level) {
        if (level == null || activePreviews.isEmpty()) {
            return List.of();
        }
        long gameTime = level.getGameTime();
        if (gameTime - lastPollGameTime < WORLD_POLL_INTERVAL_TICKS) {
            return List.of();
        }
        lastPollGameTime = gameTime;

        for (BlockPos key : List.copyOf(refreshCooldownByBuilder.keySet())) {
            int remaining = refreshCooldownByBuilder.getOrDefault(key, 0) - WORLD_POLL_INTERVAL_TICKS;
            if (remaining <= 0) {
                refreshCooldownByBuilder.remove(key);
            } else {
                refreshCooldownByBuilder.put(key, remaining);
            }
        }

        List<BlockPos> needsRefresh = new ArrayList<>();
        for (BlockPos builder : activePreviews) {
            AABB volume = resolveFootprintVolume(level, builder);
            if (volume == null) {
                continue;
            }
            int hash = computeOccupiedBlocksHash(level, volume);
            Integer last = lastWorldStateHashByBuilder.get(builder);
            if (last != null && last != hash) {
                pruneStaleOccupiedMarkers(level, builder);
                reconcileMarkersForBuilder(builder);
                lastWorldStateHashByBuilder.put(builder, hash);
                if (refreshCooldownByBuilder.containsKey(builder)) {
                    pendingWorldRefreshByBuilder.add(builder);
                } else {
                    scheduleWorldRefresh(builder, needsRefresh);
                }
            } else if (last == null) {
                lastWorldStateHashByBuilder.put(builder, hash);
            }
        }

        for (BlockPos builder : List.copyOf(pendingWorldRefreshByBuilder)) {
            if (!refreshCooldownByBuilder.containsKey(builder)) {
                pendingWorldRefreshByBuilder.remove(builder);
                scheduleWorldRefresh(builder, needsRefresh);
            }
        }
        return needsRefresh;
    }

    /** Reconcile display from layer cache for every active preview (no server request). */
    public static void tickPeriodicReconcile(Level level) {
        if (level == null || activePreviews.isEmpty()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (gameTime - lastPeriodicReconcileGameTime < PERIODIC_RECONCILE_INTERVAL_TICKS) {
            return;
        }
        lastPeriodicReconcileGameTime = gameTime;
        for (BlockPos builder : List.copyOf(activePreviews)) {
            pruneStaleOccupiedMarkers(level, builder);
            reconcileMarkersForBuilder(builder);
        }
    }

    private static void scheduleWorldRefresh(BlockPos builder, List<BlockPos> needsRefresh) {
        refreshCooldownByBuilder.put(builder, REFRESH_COOLDOWN_TICKS);
        needsRefresh.add(builder);
    }

    private static void clearTrackingForBuilder(BlockPos builderKey) {
        lastWorldStateHashByBuilder.remove(builderKey);
        refreshCooldownByBuilder.remove(builderKey);
        pendingWorldRefreshByBuilder.remove(builderKey);
        activeFootprintGeneration.remove(builderKey);
        pendingLayerResetByOwner.remove(builderKey);
        clearLayerCacheForBuilder(builderKey);
    }

    private static void clearLayerCacheForBuilder(BlockPos owner) {
        markerLayersByOwner.remove(owner);
    }

    private static void clearRendererForBuilder(BlockPos owner) {
        MarkRenderer.getInstance().clearBillboardMarkersForOwner(owner);
    }

    private static void pruneStaleOccupiedMarkers(Level level, BlockPos owner) {
        Map<BlockPos, Set<Integer>> layers = markerLayersByOwner.get(owner);
        if (layers == null || layers.isEmpty()) {
            return;
        }
        for (Map.Entry<BlockPos, Set<Integer>> entry : layers.entrySet()) {
            Set<Integer> colors = entry.getValue();
            if (!colors.contains(COLOR_OCCUPIED)) {
                continue;
            }
            BlockState state = level.getBlockState(entry.getKey());
            if (state.isAir() || state.canBeReplaced()) {
                colors.remove(COLOR_OCCUPIED);
            }
        }
    }

    private static void reconcileMarkersForBuilder(BlockPos owner) {
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            pruneStaleOccupiedMarkers(level, owner);
        }
        refreshAllMarkersForOwner(owner, BUILDER_PREVIEW_DURATION_TICKS);
    }

    private static void syncResolvedMarker(BlockPos owner, BlockPos worldPos, int durationTicks) {
        Map<BlockPos, Set<Integer>> layers = markerLayersByOwner.get(owner);
        Set<Integer> colors = layers != null ? layers.get(worldPos) : null;
        if (colors == null || colors.isEmpty()) {
            refreshAllMarkersForOwner(owner, durationTicks);
            return;
        }
        Level level = Minecraft.getInstance().level;
        Integer resolved = level != null ? resolveDisplayColor(level, worldPos, colors) : null;
        if (resolved == null) {
            refreshAllMarkersForOwner(owner, durationTicks);
        } else {
            MarkRenderer.getInstance().addBillboardMarker(owner, worldPos, resolved, durationTicks);
        }
    }

    private static void refreshAllMarkersForOwner(BlockPos owner, int durationTicks) {
        Map<BlockPos, Set<Integer>> layers = markerLayersByOwner.get(owner);
        if (layers == null || layers.isEmpty()) {
            clearRendererForBuilder(owner);
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        clearRendererForBuilder(owner);
        for (Map.Entry<BlockPos, Set<Integer>> entry : layers.entrySet()) {
            Integer resolved = resolveDisplayColor(level, entry.getKey(), entry.getValue());
            if (resolved != null) {
                MarkRenderer.getInstance().addBillboardMarker(owner, entry.getKey(), resolved, durationTicks);
            }
        }
    }

    @Nullable
    private static Integer resolveDisplayColor(Level level, BlockPos worldPos, Set<Integer> layers) {
        if (layers.contains(COLOR_OCCUPIED)) {
            BlockState state = level.getBlockState(worldPos);
            if (!state.isAir() && !state.canBeReplaced()) {
                return COLOR_OCCUPIED;
            }
        }
        for (int priority : BASELINE_COLOR_PRIORITY) {
            if (layers.contains(priority)) {
                return priority;
            }
        }
        for (int color : layers) {
            if (color != COLOR_OCCUPIED) {
                return color;
            }
        }
        return null;
    }

    private static AABB resolveFootprintVolume(Level level, BlockPos builderPos) {
        BlockEntity be = level.getBlockEntity(builderPos);
        BlockState builderState = level.getBlockState(builderPos);
        if (be instanceof ReactorBuilderBlockEntity reactor && builderState.getBlock() instanceof ReactorBuilderBlock) {
            Direction facing = builderState.getValue(ReactorBuilderBlock.FACING);
            return ReactorBuilderBlockEntity.getReactorVolumeAABB(
                    builderPos, facing,
                    reactor.getSizeLeft(), reactor.getSizeRight(),
                    reactor.getSizeHeight(), reactor.getSizeDepth());
        }
        if (be instanceof TurbineBuilderBlockEntity turbine && builderState.getBlock() instanceof TurbineBuilderBlock) {
            Direction facing = builderState.getValue(TurbineBuilderBlock.FACING);
            return TurbineBuilderBlockEntity.getTurbineVolumeAABB(
                    builderPos, facing,
                    turbine.getSizeLeft(), turbine.getSizeRight(),
                    turbine.getSizeHeight(), turbine.getSizeDepth());
        }
        return null;
    }

    private static int computeOccupiedBlocksHash(Level level, AABB volume) {
        int minX = (int) Math.floor(volume.minX);
        int minY = (int) Math.floor(volume.minY);
        int minZ = (int) Math.floor(volume.minZ);
        int maxX = (int) Math.floor(volume.maxX - 1e-6);
        int maxY = (int) Math.floor(volume.maxY - 1e-6);
        int maxZ = (int) Math.floor(volume.maxZ - 1e-6);

        int hash = 1;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState state = level.getBlockState(new BlockPos(x, y, z));
                    if (!state.isAir() && !state.canBeReplaced()) {
                        hash = 31 * hash + x;
                        hash = 31 * hash + y;
                        hash = 31 * hash + z;
                        hash = 31 * hash + Block.getId(state);
                    }
                }
            }
        }
        return hash;
    }
}
