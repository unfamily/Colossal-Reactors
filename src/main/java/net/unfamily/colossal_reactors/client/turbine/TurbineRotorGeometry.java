package net.unfamily.colossal_reactors.client.turbine;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import net.unfamily.colossal_reactors.turbine.TurbineBladePlacement;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable snapshot of turbine rod/blade layout for client rendering.
 */
public record TurbineRotorGeometry(
        long[] rodPositions,
        byte[] rodFacings,
        LongOpenHashSet bladeHidePositions,
        AABB bounds,
        int structureRevision,
        long geometryFingerprint) {

    public boolean isEmpty() {
        return rodPositions.length == 0;
    }

    public static long computeFingerprint(long[] rods, byte[] facings) {
        long hash = 1L;
        for (long rod : rods) {
            hash = 31L * hash + rod;
        }
        for (byte facing : facings) {
            hash = 31L * hash + (facing & 0xFF);
        }
        return hash;
    }

    public static TurbineRotorGeometry build(
            Level level,
            TurbineValidation.Result result,
            TurbineControllerBlockEntity controller,
            int structureRevision) {
        long[] rods = scanRods(level, result);
        byte[] facings = scanFacings(level, rods);
        LongOpenHashSet hide = buildBladeHideSet(level, rods, facings);
        AABB bounds = new AABB(
                result.minX(), result.minY(), result.minZ(),
                result.maxX() + 1.0, result.maxY() + 1.0, result.maxZ() + 1.0);
        long fingerprint = computeFingerprint(rods, facings);
        return new TurbineRotorGeometry(rods, facings, hide, bounds, structureRevision, fingerprint);
    }

    /** Uses cached rod arrays from the controller when revision matches. */
    @Nullable
    public static TurbineRotorGeometry fromController(
            Level level,
            TurbineValidation.Result result,
            TurbineControllerBlockEntity controller,
            int appliedRevision) {
        if (level == null || result == null || !result.valid()) {
            return null;
        }
        TurbineControllerBlockEntity source = TurbineRotorSimulationSource.forRendering(controller);
        int serverRevision = source.getStructureRevision();
        long[] rods = source.getCachedRodPositions();
        byte[] facings = source.getCachedRodFacings();
        if (rods.length > 0 && serverRevision == appliedRevision && cachedRodsMatchWorld(level, result, rods)) {
            LongOpenHashSet hide = buildBladeHideSet(level, rods, facings);
            AABB bounds = new AABB(
                    result.minX(), result.minY(), result.minZ(),
                    result.maxX() + 1.0, result.maxY() + 1.0, result.maxZ() + 1.0);
            return new TurbineRotorGeometry(
                    rods, facings, hide, bounds, serverRevision, computeFingerprint(rods, facings));
        }
        return build(level, result, controller, serverRevision);
    }

    private static boolean cachedRodsMatchWorld(Level level, TurbineValidation.Result result, long[] cached) {
        long[] scanned = scanRods(level, result);
        if (cached.length != scanned.length) {
            return false;
        }
        LongOpenHashSet scannedSet = new LongOpenHashSet(scanned);
        for (long rodLong : cached) {
            if (!level.getBlockState(BlockPos.of(rodLong)).is(ModBlocks.TURBINE_ROD.get()) || !scannedSet.contains(rodLong)) {
                return false;
            }
        }
        return true;
    }

    private static LongOpenHashSet buildBladeHideSet(Level level, long[] rods, byte[] facings) {
        LongOpenHashSet hide = new LongOpenHashSet();
        for (int i = 0; i < rods.length; i++) {
            hide.add(rods[i]);
            BlockPos rodPos = BlockPos.of(rods[i]);
            Direction axis = Direction.from3DDataValue(i < facings.length ? facings[i] & 0xFF : 0);
            for (BlockPos bladePos : TurbineBladePlacement.collectBladePositions(level, rodPos, axis)) {
                hide.add(bladePos.asLong());
            }
        }
        return hide;
    }

    private static long[] scanRods(Level level, TurbineValidation.Result result) {
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

    private static byte[] scanFacings(Level level, long[] rods) {
        if (rods.length == 0) {
            return new byte[0];
        }
        byte[] facings = new byte[rods.length];
        for (int i = 0; i < rods.length; i++) {
            BlockState state = level.getBlockState(BlockPos.of(rods[i]));
            facings[i] = (byte) state.getValue(TurbineRodBlock.FACING).ordinal();
        }
        return facings;
    }
}
