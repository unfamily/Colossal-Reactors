package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.item.ModItems;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineBladeBlock;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Balanced ring growth of turbine blades on a rod; max radius from {@link Config#MAX_TURBINE_BLADE_RING}. */
public final class TurbineBladePlacement {

    private TurbineBladePlacement() {}

    public static int maxRing() {
        return Config.MAX_TURBINE_BLADE_RING.get();
    }

    public static int maxBladesPerRod() {
        return maxRing() * 4;
    }

    /** Four directions perpendicular to the rod axis (stable order). */
    public static List<Direction> lateralDirections(Direction rodAxis) {
        List<Direction> dirs = new ArrayList<>(4);
        for (Direction d : Direction.values()) {
            if (d.getAxis() != rodAxis.getAxis()) {
                dirs.add(d);
            }
        }
        return dirs;
    }

    /** Blade count on one lateral axis from the rod outward (capped by config). */
    public static int depthAlong(Level level, BlockPos rodPos, Direction lateralDir) {
        Block blade = ModBlocks.TURBINE_BLADE.get();
        int depth = 0;
        BlockPos pos = rodPos.relative(lateralDir);
        int cap = maxRing();
        while (depth < cap && level.getBlockState(pos).is(blade)) {
            depth++;
            pos = pos.relative(lateralDir);
        }
        return depth;
    }

    public static int totalBladesOnRod(Level level, BlockPos rodPos, Direction rodAxis) {
        int total = 0;
        for (Direction dir : lateralDirections(rodAxis)) {
            total += depthAlong(level, rodPos, dir);
        }
        return total;
    }

    /** Smallest ring index (1-based) that still has a free lateral slot. */
    public static int currentRing(Level level, BlockPos rodPos, Direction rodAxis) {
        int limit = maxRing();
        List<Direction> lateral = lateralDirections(rodAxis);
        for (int ring = 1; ring <= limit; ring++) {
            for (Direction dir : lateral) {
                if (!level.getBlockState(rodPos.relative(dir, ring)).is(ModBlocks.TURBINE_BLADE.get())) {
                    return ring;
                }
            }
        }
        return limit + 1;
    }

    public static Optional<Direction> chooseLateralDirection(Level level, BlockPos rodPos, Direction rodAxis) {
        int total = totalBladesOnRod(level, rodPos, rodAxis);
        if (total >= maxBladesPerRod()) {
            return Optional.empty();
        }
        int ring = currentRing(level, rodPos, rodAxis);
        if (ring > maxRing()) {
            return Optional.empty();
        }

        List<Direction> lateral = lateralDirections(rodAxis);
        List<Direction> missing = new ArrayList<>();
        int minDepth = Integer.MAX_VALUE;
        for (Direction dir : lateral) {
            if (!level.getBlockState(rodPos.relative(dir, ring)).is(ModBlocks.TURBINE_BLADE.get())) {
                missing.add(dir);
                minDepth = Math.min(minDepth, depthAlong(level, rodPos, dir));
            }
        }
        if (missing.isEmpty()) {
            return Optional.empty();
        }

        // Second blade: opposite side of the first for balance
        if (total == 1) {
            for (Direction direction : lateral) {
                if (depthAlong(level, rodPos, direction) == 1) {
                    Direction opposite = direction.getOpposite();
                    if (missing.contains(opposite)) {
                        return Optional.of(opposite);
                    }
                    break;
                }
            }
        }

        // Prefer sides with fewer blades; tie-break round-robin
        List<Direction> candidates = new ArrayList<>();
        for (Direction dir : missing) {
            if (depthAlong(level, rodPos, dir) == minDepth) {
                candidates.add(dir);
            }
        }
        if (candidates.isEmpty()) {
            candidates = missing;
        }
        return Optional.of(candidates.get(total % candidates.size()));
    }

    /** All blade positions linked to this rod (contiguous blades on each lateral axis). */
    public static List<BlockPos> collectBladePositions(Level level, BlockPos rodPos, Direction rodAxis) {
        List<BlockPos> blades = new ArrayList<>();
        Block blade = ModBlocks.TURBINE_BLADE.get();
        int scanCap = Math.max(maxRing(), 64);
        for (Direction dir : lateralDirections(rodAxis)) {
            BlockPos pos = rodPos.relative(dir);
            int distance = 0;
            while (distance < scanCap && level.getBlockState(pos).is(blade)) {
                blades.add(pos.immutable());
                pos = pos.relative(dir);
                distance++;
            }
        }
        return blades;
    }

    public static void dropBladesOnRod(Level level, BlockPos rodPos, BlockState rodState) {
        if (!(rodState.getBlock() instanceof TurbineRodBlock)) {
            return;
        }
        Direction rodAxis = rodState.getValue(TurbineRodBlock.FACING);
        List<BlockPos> blades = collectBladePositions(level, rodPos, rodAxis);
        if (blades.isEmpty()) {
            return;
        }
        for (BlockPos bladePos : blades) {
            level.removeBlock(bladePos, false);
        }
        int remaining = blades.size();
        int maxStack = new ItemStack(ModItems.TURBINE_BLADE.get()).getMaxStackSize();
        while (remaining > 0) {
            int count = Math.min(remaining, maxStack);
            Block.popResource(level, rodPos, new ItemStack(ModItems.TURBINE_BLADE.get(), count));
            remaining -= count;
        }
    }

    /**
     * Places the next blade on the rod. Returns true if placed.
     */
    public static boolean placeNextBlade(Level level, BlockPos rodPos, BlockState rodState) {
        if (!(rodState.getBlock() instanceof TurbineRodBlock)) {
            return false;
        }
        if (!rodState.hasProperty(TurbineRodBlock.FACING)) {
            return false;
        }
        Direction rodAxis = rodState.getValue(TurbineRodBlock.FACING);
        Optional<Direction> lateral = chooseLateralDirection(level, rodPos, rodAxis);
        if (lateral.isEmpty()) {
            return false;
        }
        int ring = currentRing(level, rodPos, rodAxis);
        BlockPos placePos = rodPos.relative(lateral.get(), ring);
        if (!level.getBlockState(placePos).canBeReplaced()) {
            return false;
        }
        BlockState bladeState = ModBlocks.TURBINE_BLADE.get().defaultBlockState()
                .setValue(TurbineBladeBlock.FACING, lateral.get());
        return level.setBlock(placePos, bladeState, Block.UPDATE_ALL);
    }
}
