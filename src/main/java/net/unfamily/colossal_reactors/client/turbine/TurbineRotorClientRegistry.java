package net.unfamily.colossal_reactors.client.turbine;

import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.AABB;
import net.unfamily.colossal_reactors.ClientConfig;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineControllerBlock;
import net.unfamily.colossal_reactors.block.TurbineVisualState;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client registry for turbine rotor geometry, animation runtime, and visibility.
 */
public final class TurbineRotorClientRegistry {

    private static final Map<BlockPos, ClientEntry> ENTRIES = new ConcurrentHashMap<>();
    private static final Map<BlockPos, TurbineVisualState> LAST_VISUAL = new ConcurrentHashMap<>();
    private static final Long2FloatOpenHashMap CLIENT_ROTATION_FACTOR = new Long2FloatOpenHashMap();
    private static final LongOpenHashSet HIDDEN_STATIC = new LongOpenHashSet();
    private static final LongOpenHashSet PREVIOUS_HIDDEN = new LongOpenHashSet();

    private TurbineRotorClientRegistry() {}

    public enum VisibilityState {
        INACTIVE,
        DORMANT,
        ACTIVE
    }

    public static final class ClientEntry {
        @Nullable
        TurbineRotorGeometry geometry;
        int appliedStructureRevision;
        boolean visualOn;
        boolean redstoneGateOpen;
        boolean outputReturnBufferFull;
        boolean powered;
        float loadFactor;
        float angleDegrees;
        float lastRenderPartial = -1f;
        boolean wasSpinning;
        VisibilityState visibility = VisibilityState.INACTIVE;
        int structureCheckCooldown;
        /** Positions hidden for static rod/blade bake while this turbine is animating (per-controller). */
        final LongOpenHashSet activeHiddenPositions = new LongOpenHashSet();

        boolean shouldAnimate() {
            if (!visualOn || geometry == null || geometry.isEmpty()) {
                return false;
            }
            if (!redstoneGateOpen || outputReturnBufferFull || !powered) {
                return false;
            }
            return loadFactor >= ClientConfig.TURBINE_ROTOR_MIN_LOAD_TO_SPIN.get().floatValue();
        }

        boolean hasRenderableGeometry() {
            return visualOn && geometry != null && !geometry.isEmpty();
        }

        float getAngleDegrees(float partialTick, BlockPos controllerPos) {
            if (!visualOn || geometry == null) {
                return 0f;
            }
            if (!shouldAnimate()) {
                return angleDegrees;
            }
            float factor = CLIENT_ROTATION_FACTOR.getOrDefault(controllerPos.asLong(), loadFactor);
            float speed = factor * ClientConfig.TURBINE_ROTOR_MAX_DEG_PER_TICK.get().floatValue();
            return angleDegrees + speed * partialTick;
        }

        void advanceAngle(BlockPos controllerPos) {
            if (!shouldAnimate()) {
                lastRenderPartial = -1f;
                return;
            }
            float factor = CLIENT_ROTATION_FACTOR.getOrDefault(controllerPos.asLong(), loadFactor);
            float speed = factor * ClientConfig.TURBINE_ROTOR_MAX_DEG_PER_TICK.get().floatValue();
            if (speed > 0f) {
                angleDegrees = (angleDegrees + speed) % 360f;
            }
            lastRenderPartial = -1f;
        }

        void advanceRenderPartialTick(BlockPos controllerPos, float partialTick) {
            if (!shouldAnimate()) {
                lastRenderPartial = partialTick;
                wasSpinning = false;
                return;
            }
            wasSpinning = true;
            float factor = CLIENT_ROTATION_FACTOR.getOrDefault(controllerPos.asLong(), loadFactor);
            float speed = factor * ClientConfig.TURBINE_ROTOR_MAX_DEG_PER_TICK.get().floatValue();
            if (lastRenderPartial < 0f || partialTick < lastRenderPartial) {
                lastRenderPartial = partialTick;
                return;
            }
            float delta = partialTick - lastRenderPartial;
            if (delta > 0f && speed > 0f) {
                angleDegrees = (angleDegrees + speed * delta) % 360f;
            }
            lastRenderPartial = partialTick;
        }

        Direction rodFacing(int index) {
            if (geometry == null || index >= geometry.rodFacings().length) {
                return Direction.NORTH;
            }
            return Direction.from3DDataValue(geometry.rodFacings()[index] & 0xFF);
        }
    }

    @Nullable
    public static ClientEntry getEntry(BlockPos controllerPos) {
        return ENTRIES.get(controllerPos);
    }

    public static int getAppliedStructureRevision(BlockPos controllerPos) {
        ClientEntry entry = ENTRIES.get(controllerPos);
        return entry != null ? entry.appliedStructureRevision : -1;
    }

    public static void onBlockEntityLoad(TurbineControllerBlockEntity controller) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            removeEntry(controller.getBlockPos());
            return;
        }
        updateVisualState(controller, controller.getLevel());
        resetGeometryCache(controller);
        resetRuntimeCache(controller);
    }

    /**
     * Full client resync after block-entity packet (routes to the right cache resets).
     */
    public static void onControllerSync(TurbineControllerBlockEntity controller) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            removeEntry(controller.getBlockPos());
            return;
        }
        Level level = controller.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        updateVisualState(controller, level);
        ClientEntry entry = ENTRIES.get(controller.getBlockPos());
        if (entry == null || controller.getStructureRevision() != entry.appliedStructureRevision) {
            resetGeometryCache(controller, false);
        }
        resetRuntimeCache(controller);
    }

    /** After a server structure sync packet (bounds + rods + revision already applied on the BE). */
    public static void onStructureSyncPacket(TurbineControllerBlockEntity controller) {
        resetGeometryCache(controller, false, true);
        resetRuntimeCache(controller);
    }

    /**
     * Drops and rebuilds rod/blade layout from the live level (rod/blade edit, validation, structure packet).
     */
    public static void resetGeometryCache(TurbineControllerBlockEntity controller) {
        resetGeometryCache(controller, false, false);
    }

    public static void resetGeometryCache(TurbineControllerBlockEntity controller, boolean forceValidation) {
        resetGeometryCache(controller, forceValidation, false);
    }

    public static void resetGeometryCache(
            TurbineControllerBlockEntity controller,
            boolean forceValidation,
            boolean fromStructurePacket) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            return;
        }
        Level level = controller.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        BlockPos pos = controller.getBlockPos();
        BlockState ctrlState = level.getBlockState(pos);
        if (!ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())
                || ctrlState.getValue(TurbineControllerBlock.VISUAL) != TurbineVisualState.ON) {
            removeEntry(pos);
            rebuildHiddenSet();
            return;
        }

        ClientEntry entry = ENTRIES.computeIfAbsent(pos, p -> new ClientEntry());
        updateVisualState(controller, level);
        clearEntryHiddenPositions(entry);
        entry.geometry = null;
        entry.appliedStructureRevision = -1;

        if (!fromStructurePacket && !controller.getCachedResult().valid()) {
            if (!ensureValidationResult(controller, level, ctrlState, forceValidation)) {
                updateEntryHiddenPositions(entry);
                markAssemblyRenderDirty(controller);
                return;
            }
        } else if (fromStructurePacket && !controller.getCachedResult().valid()) {
            if (!ensureValidationResult(controller, level, ctrlState, true)) {
                updateEntryHiddenPositions(entry);
                markAssemblyRenderDirty(controller);
                return;
            }
        } else if (!fromStructurePacket && forceValidation) {
            if (!ensureValidationResult(controller, level, ctrlState, true)) {
                updateEntryHiddenPositions(entry);
                markAssemblyRenderDirty(controller);
                return;
            }
        }

        TurbineValidation.Result result = controller.getCachedResult();
        int revision = controller.getStructureRevision();
        TurbineRotorGeometry geometry = TurbineRotorGeometry.fromController(level, result, controller, revision);
        if (geometry == null) {
            geometry = TurbineRotorGeometry.build(level, result, controller, revision);
        }
        entry.geometry = geometry;
        entry.appliedStructureRevision = revision;
        updateVisibility(entry);
        updateEntryHiddenPositions(entry);
        markAssemblyRenderDirty(controller);
    }

    /**
     * Refreshes spin/gate/RF fields only (runtime packet, redstone change, simulation tick).
     */
    public static void resetRuntimeCache(TurbineControllerBlockEntity controller) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            return;
        }
        Level level = controller.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        BlockPos pos = controller.getBlockPos();
        ClientEntry entry = ENTRIES.computeIfAbsent(pos, p -> new ClientEntry());
        updateVisualState(controller, level);
        boolean wasAnimating = entry.shouldAnimate();
        TurbineControllerBlockEntity source = TurbineRotorSimulationSource.forRendering(controller);
        syncRuntime(entry, controller, source);
        updateVisibility(entry);
        entry.wasSpinning = entry.shouldAnimate();
        if (wasAnimating != entry.shouldAnimate()) {
            updateEntryHiddenPositions(entry);
            markAssemblyRenderDirty(controller);
        }
    }

    /** Clears all client caches for this controller (VISUAL OFF, chunk unload). */
    public static void resetAllCaches(TurbineControllerBlockEntity controller) {
        removeEntry(controller.getBlockPos());
        markAssemblyRenderDirty(controller);
    }

    /** @deprecated use {@link #resetGeometryCache} */
    public static void invalidateStructure(TurbineControllerBlockEntity controller) {
        resetGeometryCache(controller);
    }

    public static void invalidateStructure(Level level, BlockPos structurePos) {
        if (!level.isClientSide()) {
            return;
        }
        TurbineControllerBlockEntity controller = findControllerOwningStructure(level, structurePos);
        if (controller != null) {
            resetGeometryCache(controller, true, false);
            resetRuntimeCache(controller);
        }
    }

    public static void onClientRedstoneChanged(Level level, BlockPos portPos) {
        if (!level.isClientSide()) {
            return;
        }
        TurbineControllerBlockEntity controller = findControllerOwningStructure(level, portPos);
        if (controller != null) {
            resetRuntimeCache(controller);
        }
    }

    public static void pollController(TurbineControllerBlockEntity controller, float partialTick) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            return;
        }
        ClientEntry entry = ENTRIES.get(controller.getBlockPos());
        if (entry == null) {
            return;
        }
        updateVisibility(entry);
        if (entry.visibility != VisibilityState.ACTIVE) {
            return;
        }
        touchRuntime(controller);
        if (entry.shouldAnimate()) {
            entry.advanceRenderPartialTick(controller.getBlockPos(), partialTick);
        }
    }

    public static void clientTick() {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            if (!ENTRIES.isEmpty() || !HIDDEN_STATIC.isEmpty()) {
                ENTRIES.clear();
                CLIENT_ROTATION_FACTOR.clear();
                applyHiddenSet(new LongOpenHashSet());
            }
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        discoverNearbyControllers(level);
        int checkInterval = ClientConfig.TURBINE_ROTOR_STRUCTURE_CHECK_INTERVAL_TICKS.get();

        Iterator<Map.Entry<BlockPos, ClientEntry>> it = ENTRIES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, ClientEntry> mapEntry = it.next();
            BlockPos controllerPos = mapEntry.getKey();
            ClientEntry entry = mapEntry.getValue();
            BlockEntity be = level.getBlockEntity(controllerPos);
            if (!(be instanceof TurbineControllerBlockEntity controller)) {
                cleanupRemovedEntry(controllerPos, entry);
                it.remove();
                continue;
            }
            updateVisualState(controller, level);
            if (!entry.visualOn) {
                CLIENT_ROTATION_FACTOR.remove(controllerPos.asLong());
                cleanupRemovedEntry(controllerPos, entry);
                it.remove();
                continue;
            }
            updateVisibility(entry);
            if (entry.visibility == VisibilityState.DORMANT) {
                continue;
            }
            if (entry.visibility == VisibilityState.INACTIVE) {
                continue;
            }
            touchRuntime(controller);
            if (entry.structureCheckCooldown <= 0) {
                if (controller.getStructureRevision() != entry.appliedStructureRevision) {
                    resetGeometryCache(controller, false, false);
                }
                entry.structureCheckCooldown = checkInterval;
            } else {
                entry.structureCheckCooldown--;
            }
            if (entry.shouldAnimate()) {
                entry.advanceAngle(controllerPos);
            }
            boolean wasSpinning = entry.wasSpinning;
            entry.wasSpinning = entry.shouldAnimate();
            if (wasSpinning != entry.wasSpinning) {
                updateEntryHiddenPositions(entry);
            }
        }
    }

    public static void ensureAssemblyState(TurbineControllerBlockEntity controller) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            removeEntry(controller.getBlockPos());
            return;
        }
        Level level = controller.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        BlockPos pos = controller.getBlockPos();
        BlockState ctrlState = level.getBlockState(pos);
        if (!ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())) {
            return;
        }
        TurbineVisualState visual = ctrlState.getValue(TurbineControllerBlock.VISUAL);
        TurbineVisualState previous = LAST_VISUAL.get(pos);
        if (previous != visual) {
            onVisualChanged(controller, previous != null ? previous : TurbineVisualState.OFF, visual);
        } else {
            LAST_VISUAL.putIfAbsent(pos, visual);
        }
        if (visual != TurbineVisualState.ON) {
            return;
        }
        ClientEntry entry = ENTRIES.get(pos);
        if (entry == null || entry.geometry == null) {
            resetGeometryCache(controller);
            resetRuntimeCache(controller);
        }
    }

    public static boolean shouldHideStatic(BlockPos worldPos) {
        long key = worldPos.asLong();
        for (ClientEntry entry : ENTRIES.values()) {
            if (entry.shouldAnimate() && entry.activeHiddenPositions.contains(key)) {
                return true;
            }
        }
        return false;
    }

    public static boolean shouldAnimate(TurbineControllerBlockEntity controller) {
        ClientEntry entry = ENTRIES.get(controller.getBlockPos());
        return entry != null && entry.shouldAnimate()
                && entry.visibility == VisibilityState.ACTIVE
                && TurbineRotorVisibility.shouldRenderAssembly(entry.geometry);
    }

    public static boolean shouldRunBer(TurbineControllerBlockEntity controller) {
        ClientEntry entry = ENTRIES.get(controller.getBlockPos());
        if (entry == null || !entry.hasRenderableGeometry()) {
            return false;
        }
        if (entry.visibility != VisibilityState.ACTIVE) {
            return false;
        }
        return TurbineRotorVisibility.shouldRenderAssembly(entry.geometry);
    }

    public static boolean hasRenderableGeometry(TurbineControllerBlockEntity controller) {
        ClientEntry entry = ENTRIES.get(controller.getBlockPos());
        return entry != null && entry.hasRenderableGeometry();
    }

    @Nullable
    public static AABB getRenderBounds(TurbineControllerBlockEntity controller) {
        ClientEntry entry = ENTRIES.get(controller.getBlockPos());
        if (entry != null && entry.geometry != null) {
            return entry.geometry.bounds();
        }
        TurbineControllerBlockEntity source = TurbineRotorSimulationSource.forRendering(controller);
        TurbineValidation.Result result = source.getCachedResult();
        if (result.valid()) {
            return new AABB(
                    result.minX(), result.minY(), result.minZ(),
                    result.maxX() + 1.0, result.maxY() + 1.0, result.maxZ() + 1.0);
        }
        BlockPos p = controller.getBlockPos();
        return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0);
    }

    public static float getClientRotationFactor(BlockPos controllerPos) {
        return CLIENT_ROTATION_FACTOR.getOrDefault(controllerPos.asLong(), 0f);
    }

    private static void onVisualChanged(
            TurbineControllerBlockEntity controller,
            TurbineVisualState previous,
            TurbineVisualState current) {
        BlockPos pos = controller.getBlockPos();
        LAST_VISUAL.put(pos, current);
        if (current != TurbineVisualState.ON) {
            resetAllCaches(controller);
            return;
        }
        Level level = controller.getLevel();
        if (level != null) {
            refreshClientValidationCache(controller, level, level.getBlockState(pos));
        }
        resetGeometryCache(controller);
        resetRuntimeCache(controller);
    }

    private static void removeEntry(BlockPos pos) {
        ClientEntry entry = ENTRIES.remove(pos);
        if (entry != null) {
            clearEntryHiddenPositions(entry);
        }
        CLIENT_ROTATION_FACTOR.remove(pos.asLong());
        LAST_VISUAL.remove(pos);
    }

    private static void syncRuntime(ClientEntry entry, TurbineControllerBlockEntity controller, TurbineControllerBlockEntity source) {
        entry.redstoneGateOpen = source.isRedstoneGateOpen();
        entry.powered = source.isProducingEnergy();
        entry.outputReturnBufferFull = source.isOutputReturnBufferFull();
        entry.loadFactor = source.getRotorLoadFactor();
        BlockPos pos = controller.getBlockPos();
        float factor = computeRotationFactor(controller, source, entry);
        if (factor > 0f) {
            CLIENT_ROTATION_FACTOR.put(pos.asLong(), factor);
        } else {
            CLIENT_ROTATION_FACTOR.remove(pos.asLong());
        }
    }

    /** Per-frame runtime refresh without full cache reset (not used on block edits). */
    private static void touchRuntime(TurbineControllerBlockEntity controller) {
        ClientEntry entry = ENTRIES.get(controller.getBlockPos());
        if (entry == null) {
            return;
        }
        TurbineControllerBlockEntity source = TurbineRotorSimulationSource.forRendering(controller);
        boolean wasAnimating = entry.shouldAnimate();
        syncRuntime(entry, controller, source);
        updateVisibility(entry);
        entry.wasSpinning = entry.shouldAnimate();
        if (wasAnimating != entry.shouldAnimate()) {
            updateEntryHiddenPositions(entry);
            markAssemblyRenderDirty(controller);
        }
    }

    private static boolean ensureValidationResult(
            TurbineControllerBlockEntity controller,
            Level level,
            BlockState ctrlState,
            boolean force) {
        if (!force && controller.getCachedResult().valid()) {
            return true;
        }
        return refreshClientValidationCache(controller, level, ctrlState);
    }

    @Nullable
    private static TurbineControllerBlockEntity findControllerOwningStructure(Level level, BlockPos structurePos) {
        ChunkPos center = new ChunkPos(structurePos);
        int radius = TurbineControllerBlock.structureNotifyChunkRadius();
        TurbineControllerBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;

        for (int cx = center.x - radius; cx <= center.x + radius; cx++) {
            for (int cz = center.z - radius; cz <= center.z + radius; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (!(be instanceof TurbineControllerBlockEntity controller)) {
                        continue;
                    }
                    BlockState ctrlState = level.getBlockState(controller.getBlockPos());
                    if (!ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())
                            || ctrlState.getValue(TurbineControllerBlock.VISUAL) != TurbineVisualState.ON) {
                        continue;
                    }
                    TurbineValidation.Result result = controller.getCachedResult();
                    if (!TurbineControllerBlock.containsBlock(result, structurePos)) {
                        continue;
                    }
                    double dist = structurePos.distSqr(controller.getBlockPos());
                    if (dist < bestDist) {
                        bestDist = dist;
                        best = controller;
                    }
                }
            }
        }
        return best;
    }

    private static void clearEntryHiddenPositions(ClientEntry entry) {
        if (entry.activeHiddenPositions.isEmpty()) {
            return;
        }
        LongOpenHashSet old = new LongOpenHashSet(entry.activeHiddenPositions);
        entry.activeHiddenPositions.clear();
        notifyHiddenDiff(old, new LongOpenHashSet());
        rebuildGlobalHiddenUnion();
    }

    private static void updateEntryHiddenPositions(ClientEntry entry) {
        LongOpenHashSet old = new LongOpenHashSet(entry.activeHiddenPositions);
        entry.activeHiddenPositions.clear();
        if (entry.shouldAnimate() && entry.geometry != null) {
            entry.activeHiddenPositions.addAll(entry.geometry.bladeHidePositions());
        }
        if (!hiddenSetsEqual(old, entry.activeHiddenPositions)) {
            notifyHiddenDiff(old, entry.activeHiddenPositions);
            rebuildGlobalHiddenUnion();
        }
    }

    private static boolean hiddenSetsEqual(LongOpenHashSet a, LongOpenHashSet b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (var iter = a.longIterator(); iter.hasNext(); ) {
            if (!b.contains(iter.nextLong())) {
                return false;
            }
        }
        return true;
    }

    private static void cleanupRemovedEntry(BlockPos controllerPos, ClientEntry entry) {
        clearEntryHiddenPositions(entry);
    }

    private static float computeRotationFactor(
            TurbineControllerBlockEntity controller,
            TurbineControllerBlockEntity source,
            ClientEntry entry) {
        if (!entry.visualOn || entry.geometry == null || entry.geometry.isEmpty()) {
            return 0f;
        }
        Level level = controller.getLevel();
        if (level == null) {
            return 0f;
        }
        BlockState ctrlState = level.getBlockState(controller.getBlockPos());
        if (!ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())
                || ctrlState.getValue(TurbineControllerBlock.VISUAL) != TurbineVisualState.ON) {
            return 0f;
        }
        TurbineValidation.Result result = source.getCachedResult();
        if (result == null || !result.valid()) {
            return 0f;
        }
        if (!source.isRedstoneGateOpen() || source.isOutputReturnBufferFull() || !source.isProducingEnergy()) {
            return 0f;
        }
        return source.getRotorLoadFactor();
    }

    private static void updateVisualState(TurbineControllerBlockEntity controller, Level level) {
        BlockState ctrlState = level.getBlockState(controller.getBlockPos());
        ClientEntry entry = ENTRIES.computeIfAbsent(controller.getBlockPos(), p -> new ClientEntry());
        entry.visualOn = ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())
                && ctrlState.getValue(TurbineControllerBlock.VISUAL) == TurbineVisualState.ON;
    }

    private static void updateVisibility(ClientEntry entry) {
        if (!entry.visualOn || entry.geometry == null) {
            entry.visibility = VisibilityState.INACTIVE;
            return;
        }
        if (TurbineRotorVisibility.isWithinRenderDistance(entry.geometry.bounds())) {
            entry.visibility = VisibilityState.ACTIVE;
        } else {
            entry.visibility = VisibilityState.DORMANT;
        }
    }

    private static void discoverNearbyControllers(Level level) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        double maxDist = ClientConfig.getTurbineRotorRenderDistanceBlocks() + 16.0;
        ChunkPos center = player.chunkPosition();
        int chunkRadius = (int) Math.ceil(maxDist / 16.0) + 1;
        for (int cx = center.x - chunkRadius; cx <= center.x + chunkRadius; cx++) {
            for (int cz = center.z - chunkRadius; cz <= center.z + chunkRadius; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof TurbineControllerBlockEntity controller) {
                        BlockState state = level.getBlockState(controller.getBlockPos());
                        if (state.getValue(TurbineControllerBlock.VISUAL) == TurbineVisualState.ON) {
                            ensureAssemblyState(controller);
                        }
                    }
                }
            }
        }
    }

    private static boolean refreshClientValidationCache(
            TurbineControllerBlockEntity controller,
            Level level,
            BlockState ctrlState) {
        if (!ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())) {
            return false;
        }
        Direction into = ctrlState.getValue(TurbineControllerBlock.FACING).getOpposite();
        BlockPos start = controller.getBlockPos().relative(into);
        TurbineValidation.Result result = TurbineValidation.validateWithRodAlignment(level, start, into, -1);
        if (!result.valid()) {
            return false;
        }
        controller.applyClientValidationResultQuiet(level, result);
        return true;
    }

    private static void rebuildGlobalHiddenUnion() {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            applyHiddenSet(new LongOpenHashSet());
            return;
        }
        LongOpenHashSet next = new LongOpenHashSet();
        for (ClientEntry entry : ENTRIES.values()) {
            next.addAll(entry.activeHiddenPositions);
        }
        applyHiddenSet(next);
    }

    /** @deprecated use per-entry {@link #updateEntryHiddenPositions} */
    private static void rebuildHiddenSet() {
        for (Map.Entry<BlockPos, ClientEntry> mapEntry : ENTRIES.entrySet()) {
            updateEntryHiddenPositions(mapEntry.getValue());
        }
    }

    private static void applyHiddenSet(LongOpenHashSet next) {
        notifyHiddenDiff(PREVIOUS_HIDDEN, next);
        PREVIOUS_HIDDEN.clear();
        PREVIOUS_HIDDEN.addAll(next);
        HIDDEN_STATIC.clear();
        HIDDEN_STATIC.addAll(next);
    }

    private static void notifyHiddenDiff(LongOpenHashSet previous, LongOpenHashSet next) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        LongOpenHashSet changed = new LongOpenHashSet();
        for (LongIterator iter = next.longIterator(); iter.hasNext(); ) {
            long pos = iter.nextLong();
            if (!previous.contains(pos)) {
                changed.add(pos);
            }
        }
        for (LongIterator iter = previous.longIterator(); iter.hasNext(); ) {
            long pos = iter.nextLong();
            if (!next.contains(pos)) {
                changed.add(pos);
            }
        }
        if (changed.isEmpty()) {
            return;
        }
        LevelRenderer levelRenderer = Minecraft.getInstance().levelRenderer;
        for (LongIterator iter = changed.longIterator(); iter.hasNext(); ) {
            BlockPos pos = BlockPos.of(iter.nextLong());
            levelRenderer.setBlocksDirty(pos.getX(), pos.getY(), pos.getZ(), pos.getX(), pos.getY(), pos.getZ());
        }
    }

    private static void markAssemblyRenderDirty(TurbineControllerBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        AABB bounds = getRenderBounds(controller);
        if (bounds != null) {
            Minecraft.getInstance().levelRenderer.setBlocksDirty(
                    (int) bounds.minX, (int) bounds.minY, (int) bounds.minZ,
                    (int) bounds.maxX, (int) bounds.maxY, (int) bounds.maxZ);
        }
    }
}
