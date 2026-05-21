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
import net.unfamily.colossal_reactors.ClientConfig;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineControllerBlock;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;
import net.unfamily.colossal_reactors.block.TurbineVisualState;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import net.unfamily.colossal_reactors.turbine.TurbineBladePlacement;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side rotor animation state keyed by turbine controller position.
 */
public final class TurbineRotorAnimationManager {

    private static final Map<BlockPos, RotorState> STATES = new ConcurrentHashMap<>();
    /** Mekanism-style 0..1 spin factor per controller (flow rate), updated each refresh. */
    private static final Long2FloatOpenHashMap CLIENT_ROTATION_FACTOR = new Long2FloatOpenHashMap();
    private static final LongOpenHashSet HIDDEN_STATIC = new LongOpenHashSet();
    private static final LongOpenHashSet PREVIOUS_HIDDEN = new LongOpenHashSet();

    private TurbineRotorAnimationManager() {}

    /** Called from BER each frame so runtime matches simulation before drawing (Mekanism-style). */
    public static void pollController(TurbineControllerBlockEntity controller, float partialTick) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            return;
        }
        applySyncedRuntime(controller);
        RotorState state = STATES.get(controller.getBlockPos());
        if (state != null) {
            state.refreshRuntime(controller);
        }
    }

    /**
     * Pushes Mekanism-style {@code clientRotation} from the synced controller into the client rotation map.
     * Call after every runtime block-entity packet and each frame before BER draws.
     */
    public static void applySyncedRuntime(TurbineControllerBlockEntity controller) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            return;
        }
        Level level = controller.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        BlockPos pos = controller.getBlockPos();
        float factor = computeClientRotationFactor(controller);
        if (factor > 0f) {
            CLIENT_ROTATION_FACTOR.put(pos.asLong(), factor);
        } else {
            CLIENT_ROTATION_FACTOR.remove(pos.asLong());
        }
        ensureAssemblyState(controller);
        RotorState state = STATES.get(pos);
        if (state != null) {
            state.syncFromController(controller);
        }
    }

    /**
     * Mekanism-style 0..1 flow: redstone gate allows run, and RF is actually being produced (rf / maxRf).
     */
    public static float computeClientRotationFactor(TurbineControllerBlockEntity controller) {
        if (!isRotorAllowedToSpin(controller)) {
            return 0f;
        }
        TurbineControllerBlockEntity source = TurbineRotorSimulationSource.forRendering(controller);
        return source.getRotorLoadFactor();
    }

    /**
     * Same rules as turbine simulation + GUI: ON, redstone mode active, producing RF, not buffer-blocked.
     */
    public static boolean isRotorAllowedToSpin(TurbineControllerBlockEntity controller) {
        Level level = controller.getLevel();
        if (level == null) {
            return false;
        }
        BlockState ctrlState = level.getBlockState(controller.getBlockPos());
        if (!ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())
                || ctrlState.getValue(TurbineControllerBlock.VISUAL) != TurbineVisualState.ON) {
            return false;
        }
        TurbineControllerBlockEntity source = TurbineRotorSimulationSource.forRendering(controller);
        TurbineValidation.Result result = source.getCachedResult();
        if (result == null || !result.valid()) {
            return false;
        }
        if (!TurbineControllerBlock.isRedstoneGateSatisfied(level, source, result)) {
            return false;
        }
        if (source.isOutputReturnBufferFull()) {
            return false;
        }
        if (!source.isProducingEnergy()) {
            return false;
        }
        float min = ClientConfig.TURBINE_ROTOR_MIN_LOAD_TO_SPIN.get().floatValue();
        return source.getRotorLoadFactor() >= min;
    }

    /** Immediate client refresh when a turbine redstone port neighbor updates. */
    public static void onClientRedstoneChanged(Level level, BlockPos portPos) {
        if (!level.isClientSide()) {
            return;
        }
        ChunkPos center = new ChunkPos(portPos);
        for (int cx = center.x - 1; cx <= center.x + 1; cx++) {
            for (int cz = center.z - 1; cz <= center.z + 1; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
                    if (be instanceof TurbineControllerBlockEntity controller) {
                        applySyncedRuntime(controller);
                    }
                }
            }
        }
    }

    /** 0..1 rotation factor synced from server production (like Mekanism {@code clientRotation}). */
    public static float getClientRotationFactor(BlockPos controllerPos) {
        return CLIENT_ROTATION_FACTOR.getOrDefault(controllerPos.asLong(), 0f);
    }

    public static void onControllerSync(TurbineControllerBlockEntity controller) {
        applySyncedRuntime(controller);
        rebuildHiddenSet();
    }

    /** Registers or refreshes assembly state; safe to call from BER each frame. */
    public static void ensureAssemblyState(TurbineControllerBlockEntity controller) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            STATES.remove(controller.getBlockPos());
            return;
        }
        Level level = controller.getLevel();
        if (level == null || !level.isClientSide()) {
            return;
        }
        BlockState ctrlState = level.getBlockState(controller.getBlockPos());
        if (!ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())
                || ctrlState.getValue(TurbineControllerBlock.VISUAL) != TurbineVisualState.ON) {
            if (STATES.remove(controller.getBlockPos()) != null) {
                rebuildHiddenSet();
            }
            return;
        }
        if (!controller.getCachedResult().valid()) {
            tryPopulateClientValidationCache(controller, level, ctrlState);
        }
        boolean hadState = STATES.containsKey(controller.getBlockPos());
        boolean hasState = mergeOrRegister(controller);
        if (hadState != hasState) {
            rebuildHiddenSet();
        }
    }

    public static void clientTick() {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            if (!STATES.isEmpty() || !HIDDEN_STATIC.isEmpty()) {
                STATES.clear();
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

        Iterator<Map.Entry<BlockPos, RotorState>> it = STATES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, RotorState> entry = it.next();
            BlockEntity be = level.getBlockEntity(entry.getKey());
            if (!(be instanceof TurbineControllerBlockEntity controller)) {
                it.remove();
                continue;
            }
            if (!entry.getValue().refreshRuntime(controller)) {
                CLIENT_ROTATION_FACTOR.remove(entry.getKey().asLong());
                it.remove();
                continue;
            }
            entry.getValue().advanceAngle();
        }
        rebuildHiddenSet();
    }

    @Nullable
    public static RotorState getState(BlockPos controllerPos) {
        return STATES.get(controllerPos);
    }

    public static boolean shouldHideStatic(BlockPos worldPos) {
        return HIDDEN_STATIC.contains(worldPos.asLong());
    }

    public static boolean shouldSpin(BlockPos controllerPos) {
        RotorState state = STATES.get(controllerPos);
        return state != null && state.shouldSpin();
    }

    public static boolean shouldRenderAssembly(BlockPos controllerPos) {
        RotorState state = STATES.get(controllerPos);
        return state != null && state.isAssemblyReady();
    }

    private static void discoverNearbyControllers(Level level) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ChunkPos center = player.chunkPosition();
        for (int cx = center.x - 2; cx <= center.x + 2; cx++) {
            for (int cz = center.z - 2; cz <= center.z + 2; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof TurbineControllerBlockEntity controller) {
                        applySyncedRuntime(controller);
                    }
                }
            }
        }
    }

    private static boolean tryPopulateClientValidationCache(
            TurbineControllerBlockEntity controller,
            Level level,
            BlockState ctrlState) {
        Direction into = ctrlState.getValue(TurbineControllerBlock.FACING).getOpposite();
        BlockPos start = controller.getBlockPos().relative(into);
        TurbineValidation.Result result = TurbineValidation.validateWithRodAlignment(level, start, into, -1);
        if (!result.valid()) {
            return false;
        }
        controller.applyClientValidationResult(level, result);
        return true;
    }

    /** @return true if an assembly-ready state is registered for this controller */
    private static boolean mergeOrRegister(TurbineControllerBlockEntity controller) {
        BlockPos pos = controller.getBlockPos();
        RotorState existing = STATES.get(pos);
        float preservedAngle = existing != null ? existing.angleDegrees : 0f;
        RotorState created = RotorState.fromController(controller);
        if (created == null || !created.isAssemblyReady()) {
            return STATES.remove(pos) != null;
        }
        created.angleDegrees = preservedAngle;
        STATES.put(pos, created);
        return true;
    }

    private static void rebuildHiddenSet() {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            applyHiddenSet(new LongOpenHashSet());
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        LongOpenHashSet next = new LongOpenHashSet();
        for (RotorState state : STATES.values()) {
            if (!state.shouldHideStaticBlocks()) {
                continue;
            }
            for (int i = 0; i < state.rodPositions.length; i++) {
                long rodLong = state.rodPositions[i];
                next.add(rodLong);
                BlockPos rodPos = BlockPos.of(rodLong);
                Direction axis = state.rodFacing(i);
                for (BlockPos bladePos : TurbineBladePlacement.collectBladePositions(level, rodPos, axis)) {
                    next.add(bladePos.asLong());
                }
            }
        }
        applyHiddenSet(next);
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

    public static final class RotorState {
        private final BlockPos controllerPos;
        private long[] rodPositions;
        private byte[] rodFacings;
        private final int minX;
        private final int minY;
        private final int minZ;
        private final int maxX;
        private final int maxY;
        private final int maxZ;
        private boolean assemblyValid;
        private boolean visualOn;
        private boolean powered;
        private boolean redstoneGateOpen;
        private boolean outputReturnBufferFull;
        private float loadFactor;
        private float angleDegrees;

        private RotorState(
                BlockPos controllerPos,
                long[] rodPositions,
                byte[] rodFacings,
                int minX, int minY, int minZ,
                int maxX, int maxY, int maxZ,
                boolean assemblyValid,
                boolean visualOn,
                boolean powered,
                boolean redstoneGateOpen,
                boolean outputReturnBufferFull,
                float loadFactor) {
            this.controllerPos = controllerPos;
            this.rodPositions = rodPositions;
            this.rodFacings = rodFacings;
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.assemblyValid = assemblyValid;
            this.visualOn = visualOn;
            this.powered = powered;
            this.redstoneGateOpen = redstoneGateOpen;
            this.outputReturnBufferFull = outputReturnBufferFull;
            this.loadFactor = loadFactor;
        }

        @Nullable
        static RotorState fromController(TurbineControllerBlockEntity controller) {
            TurbineControllerBlockEntity source = TurbineRotorSimulationSource.forRendering(controller);
            TurbineValidation.Result result = source.getCachedResult();
            if (result == null || !result.valid()) {
                return null;
            }
            Level level = controller.getLevel();
            BlockState ctrlState = level != null
                    ? level.getBlockState(controller.getBlockPos())
                    : controller.getBlockState();
            boolean visualOn = ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())
                    && ctrlState.getValue(TurbineControllerBlock.VISUAL) == TurbineVisualState.ON;
            float load = source.getRotorLoadFactor();
            long[] rods = source.getCachedRodPositions();
            byte[] facings = source.getCachedRodFacings();
            if (rods.length == 0) {
                rods = scanRodsClient(level, result);
                facings = scanFacingsClient(level, rods);
            }
            return new RotorState(
                    controller.getBlockPos(),
                    rods,
                    facings,
                    result.minX(), result.minY(), result.minZ(),
                    result.maxX(), result.maxY(), result.maxZ(),
                    true,
                    visualOn,
                    source.isPowered(),
                    source.isRedstoneGateOpen(),
                    source.isOutputReturnBufferFull(),
                    load);
        }

        /**
         * Updates runtime flags from the live client entity. Returns false if this entry should be removed.
         */
        boolean refreshRuntime(TurbineControllerBlockEntity controller) {
            Level level = controller.getLevel();
            if (level == null || !level.isClientSide()) {
                return false;
            }
            BlockState ctrlState = level.getBlockState(controller.getBlockPos());
            boolean prevVisual = visualOn;
            visualOn = ctrlState.is(ModBlocks.TURBINE_CONTROLLER.get())
                    && ctrlState.getValue(TurbineControllerBlock.VISUAL) == TurbineVisualState.ON;
            if (!visualOn) {
                return false;
            }

            TurbineValidation.Result result = controller.getCachedResult();
            if (result == null || !result.valid()) {
                if (!tryPopulateClientValidationCache(controller, level, ctrlState)) {
                    return false;
                }
                result = controller.getCachedResult();
            }

            TurbineControllerBlockEntity source = TurbineRotorSimulationSource.forRendering(controller);
            TurbineValidation.Result resultForGate = source.getCachedResult();
            syncFromController(source);
            if (resultForGate != null && resultForGate.valid()) {
                redstoneGateOpen = TurbineControllerBlock.isRedstoneGateSatisfied(level, source, resultForGate);
            }
            powered = source.isProducingEnergy();
            if (!prevVisual) {
                TurbineRotorAnimationManager.onControllerSync(controller);
            }

            long[] rods = source.getCachedRodPositions();
            byte[] facings = controller.getCachedRodFacings();
            if (rods.length == 0) {
                rods = scanRodsClient(level, result);
                facings = scanFacingsClient(level, rods);
            }
            if (!java.util.Arrays.equals(rodPositions, rods)) {
                rodPositions = rods;
                rodFacings = facings;
                TurbineRotorAnimationManager.onControllerSync(controller);
            }
            CLIENT_ROTATION_FACTOR.put(controllerPos.asLong(), computeClientRotationFactor(controller));
            return true;
        }

        void syncFromController(TurbineControllerBlockEntity source) {
            assemblyValid = true;
            powered = source.isProducingEnergy();
            outputReturnBufferFull = source.isOutputReturnBufferFull();
            loadFactor = source.getRotorLoadFactor();
        }

        /** Degrees per game tick from synced production factor (Mekanism-style: 0 when not running). */
        float rotationSpeedDegreesPerTick() {
            if (!shouldSpin()) {
                return 0f;
            }
            float factor = CLIENT_ROTATION_FACTOR.getOrDefault(controllerPos.asLong(), loadFactor);
            return factor * ClientConfig.TURBINE_ROTOR_MAX_DEG_PER_TICK.get().floatValue();
        }

        private static long[] scanRodsClient(@Nullable Level level, TurbineValidation.Result result) {
            if (level == null) {
                return new long[0];
            }
            it.unimi.dsi.fastutil.longs.LongArrayList rods = new it.unimi.dsi.fastutil.longs.LongArrayList();
            BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
            for (int x = result.minX(); x <= result.maxX(); x++) {
                for (int y = result.minY(); y <= result.maxY(); y++) {
                    for (int z = result.minZ(); z <= result.maxZ(); z++) {
                        p.set(x, y, z);
                        if (level.getBlockState(p).is(ModBlocks.TURBINE_ROD.get())) {
                            rods.add(p.asLong());
                        }
                    }
                }
            }
            return rods.toLongArray();
        }

        private static byte[] scanFacingsClient(@Nullable Level level, long[] rods) {
            if (level == null || rods.length == 0) {
                return new byte[0];
            }
            byte[] facings = new byte[rods.length];
            for (int i = 0; i < rods.length; i++) {
                BlockState state = level.getBlockState(BlockPos.of(rods[i]));
                facings[i] = (byte) state.getValue(TurbineRodBlock.FACING).ordinal();
            }
            return facings;
        }

        public boolean isAssemblyReady() {
            return ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()
                    && assemblyValid
                    && visualOn;
        }

        public boolean shouldHideStaticBlocks() {
            return isAssemblyReady();
        }

        public boolean shouldSpin() {
            if (!isAssemblyReady()) {
                return false;
            }
            if (!redstoneGateOpen || outputReturnBufferFull || !powered) {
                return false;
            }
            return loadFactor >= ClientConfig.TURBINE_ROTOR_MIN_LOAD_TO_SPIN.get().floatValue();
        }

        void advanceAngle() {
            float speed = rotationSpeedDegreesPerTick();
            if (speed > 0f) {
                angleDegrees = (angleDegrees + speed) % 360f;
            }
        }

        public float getAngleDegrees(float partialTick) {
            if (!isAssemblyReady()) {
                return 0f;
            }
            return angleDegrees + rotationSpeedDegreesPerTick() * partialTick;
        }

        public BlockPos controllerPos() {
            return controllerPos;
        }

        public long[] rodPositions() {
            return rodPositions;
        }

        public Direction rodFacing(int index) {
            int ord = index < rodFacings.length ? (rodFacings[index] & 0xFF) : 0;
            return Direction.from3DDataValue(ord);
        }
    }
}
