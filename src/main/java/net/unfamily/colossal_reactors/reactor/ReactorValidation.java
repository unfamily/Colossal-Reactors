package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;

/**
 * Validates reactor multiblock structure: parallelepiped, casing border, rod columns with rod_controller on top.
 * Bounds are found by walking the shell: down+left to find min corner, up+right+depth to find max corner.
 */
public final class ReactorValidation {

    public record Result(boolean valid, int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                        int rodCount, int rodColumns, int coolantCount) {}

    private ReactorValidation() {}

    /**
     * Validate reactor structure. The given pos should be the block "behind" the controller (inside the reactor shell).
     * The direction is from controller towards that block (into the reactor).
     */
    public static Result validate(Level level, BlockPos start, Direction intoReactor) {
        if (Boolean.TRUE.equals(Config.REACTOR_VALIDATION_DEBUG.get())) {
            ColossalReactors.LOGGER.info("[ReactorValidation] validate() called: start={} direction={}", start, intoReactor);
        }
        if (start == null || level == null || !level.isLoaded(start)) {
            return invalid(level, start, "null or unloaded start");
        }
        if (!isShellBlock(level.getBlockState(start))) {
            return invalid(level, start, "start block is not shell: " + level.getBlockState(start).getBlock().getDescriptionId());
        }

        // Controller on a vertical face: intoReactor must be horizontal (so "left/right" are along the face)
        if (intoReactor.getAxis() == Direction.Axis.Y) {
            return invalid(level, start, "intoReactor must be horizontal (controller on vertical face)");
        }
        Direction left = intoReactor.getCounterClockWise();
        Direction right = intoReactor.getClockWise();

        int maxW = Config.MAX_REACTOR_WIDTH.get();
        int maxL = Config.MAX_REACTOR_LENGTH.get();
        int maxH = Config.MAX_REACTOR_HEIGHT.get();
        int maxHorizontal = Math.max(maxW, maxL);

        BlockPos pos = start;
        int steps;

        // Walk down until next block is not shell (never exceed config height)
        steps = 0;
        while (steps < maxH && isShellOrRodController(level.getBlockState(pos.below()))) {
            pos = pos.below();
            steps++;
        }
        // Walk left until next block is not shell (never exceed config horizontal)
        steps = 0;
        while (steps < maxHorizontal && isShellOrRodController(level.getBlockState(pos.relative(left)))) {
            pos = pos.relative(left);
            steps++;
        }
        BlockPos minCorner = pos;

        // Walk up until next block is not shell
        steps = 0;
        while (steps < maxH && isShellOrRodController(level.getBlockState(pos.above()))) {
            pos = pos.above();
            steps++;
        }
        // Walk right until next block is not shell
        steps = 0;
        while (steps < maxHorizontal && isShellOrRodController(level.getBlockState(pos.relative(right)))) {
            pos = pos.relative(right);
            steps++;
        }
        // Walk into reactor (depth): step only into shell or rod; never step from shell into air (that would be outside)
        steps = 0;
        while (steps < maxHorizontal && canStepDepth(level, pos, intoReactor)) {
            pos = pos.relative(intoReactor);
            steps++;
        }
        BlockPos maxCorner = pos;

        int minX = Math.min(minCorner.getX(), maxCorner.getX());
        int minY = Math.min(minCorner.getY(), maxCorner.getY());
        int minZ = Math.min(minCorner.getZ(), maxCorner.getZ());
        int maxX = Math.max(minCorner.getX(), maxCorner.getX());
        int maxY = Math.max(minCorner.getY(), maxCorner.getY());
        int maxZ = Math.max(minCorner.getZ(), maxCorner.getZ());

        int width = maxX - minX + 1;
        int length = maxZ - minZ + 1;
        int height = maxY - minY + 1;
        debug("CORNERS: minCorner={} maxCorner={} bounds=[{} {} {}]..[{} {} {}] width={} length={} height={}",
                minCorner, maxCorner, minX, minY, minZ, maxX, maxY, maxZ, width, length, height);
        if (width > maxW || length > maxL || height > maxH) {
            return invalid(level, start, "reactor too large: " + width + "x" + length + "x" + height + " (max " + maxW + "x" + maxL + "x" + maxH + ")");
        }

        int rodCount = 0;
        int coolantCount = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    boolean onBorder = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    BlockState state = level.getBlockState(p);

                    if (onBorder) {
                        if (!isShellBlock(state) && !isRodController(state)) {
                            return invalid(level, start, "border must be shell or rod_controller at " + p + " = " + state.getBlock().getDescriptionId());
                        }
                    } else {
                        if (state.is(ModBlocks.REACTOR_ROD.get())) {
                            rodCount++;
                        } else if (state.isAir() || HeatSinkLoader.isHeatSinkBlock(state, level.registryAccess())) {
                            coolantCount++;
                        } else {
                            return invalid(level, start, "interior must be rod, air or heat sink at " + p + " = " + state.getBlock().getDescriptionId());
                        }
                    }
                }
            }
        }

        debug("BORDER/INTERIOR scan: rodCount={} coolantCount={}", rodCount, coolantCount);

        int interiorFloorY = minY + 1;
        int interiorCeilingY = maxY - 1;
        int rodColumnsExpected = 0;
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                if (level.getBlockState(new BlockPos(x, maxY, z)).is(ModBlocks.ROD_CONTROLLER.get())) {
                    rodColumnsExpected++;
                    for (int y = interiorFloorY; y <= interiorCeilingY; y++) {
                        BlockPos columnPos = new BlockPos(x, y, z);
                        if (!level.getBlockState(columnPos).is(ModBlocks.REACTOR_ROD.get())) {
                            return invalid(level, start, "rod column incomplete at " + columnPos + " (column " + x + "," + z + ")");
                        }
                    }
                }
            }
        }
        debug("ROD COLUMNS: rodColumnsExpected={} interiorHeight={}", rodColumnsExpected, interiorCeilingY - interiorFloorY + 1);
        if (rodColumnsExpected > 0 && rodCount != rodColumnsExpected * (interiorCeilingY - interiorFloorY + 1)) {
            return invalid(level, start, "rod count mismatch: rodCount=" + rodCount + " expected " + rodColumnsExpected + "*" + (interiorCeilingY - interiorFloorY + 1));
        }

        if (!Boolean.TRUE.equals(Config.ALLOW_MULTIPLE_REACTOR_CONTROLLERS.get())) {
            int controllerCount = countReactorControllersOnOuterFaces(level, minX, minY, minZ, maxX, maxY, maxZ);
            if (controllerCount != 1) {
                return invalid(level, start, "reactor must have exactly one reactor controller on outer faces (found " + controllerCount + "). Enable allowMultipleReactorControllers in config to allow more.");
            }
        }

        Result result = new Result(true, minX, minY, minZ, maxX, maxY, maxZ, rodCount, rodColumnsExpected, coolantCount);
        debug("VALID result: valid=true min=({},{},{}) max=({},{},{}) rodCount={} rodColumns={} coolantCount={}",
                result.minX(), result.minY(), result.minZ(), result.maxX(), result.maxY(), result.maxZ(),
                result.rodCount(), result.rodColumns(), result.coolantCount());
        return result;
    }

    /** True if the block is part of the reactor shell (casing, glass, any port). Used for adjacency penalty. */
    public static boolean isShellBlock(BlockState state) {
        return state.is(ModBlocks.REACTOR_CASING.get())
                || state.is(ModBlocks.REACTOR_GLASS.get())
                || state.is(ModBlocks.POWER_PORT.get())
                || state.is(ModBlocks.REDSTONE_PORT.get())
                || state.is(ModBlocks.RESOURCE_PORT.get());
    }

    private static boolean isShellOrRodController(BlockState state) {
        return isShellBlock(state) || state.is(ModBlocks.ROD_CONTROLLER.get());
    }

    /** Can step in depth direction: into shell or rod; into air or heat sink only when current is interior (not shell, else we'd step outside). */
    private static boolean canStepDepth(Level level, BlockPos pos, Direction intoReactor) {
        BlockPos next = pos.relative(intoReactor);
        BlockState nextState = level.getBlockState(next);
        BlockState currentState = level.getBlockState(pos);
        if (isShellOrRodController(nextState)) return true;
        if (nextState.is(ModBlocks.REACTOR_ROD.get())) return true;
        if (!isShellOrRodController(currentState) && (nextState.isAir() || HeatSinkLoader.isHeatSinkBlock(nextState, level.registryAccess()))) return true;
        return false;
    }

    private static boolean isRodController(BlockState state) {
        return state.is(ModBlocks.ROD_CONTROLLER.get());
    }

    /** Count reactor controller blocks on the 6 outer faces of the reactor box (one step outside min/max). */
    private static int countReactorControllersOnOuterFaces(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int count = 0;
        // Face x = minX - 1
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(minX - 1, y, z)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        // Face x = maxX + 1
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(maxX + 1, y, z)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        // Face y = minY - 1
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(x, minY - 1, z)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        // Face y = maxY + 1
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(x, maxY + 1, z)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        // Face z = minZ - 1
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (level.getBlockState(new BlockPos(x, y, minZ - 1)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        // Face z = maxZ + 1
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (level.getBlockState(new BlockPos(x, y, maxZ + 1)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        return count;
    }

    private static Result invalid() {
        return new Result(false, 0, 0, 0, 0, 0, 0, 0, 0, 0);
    }

    private static Result invalid(Level level, BlockPos start, String reason) {
        if (Boolean.TRUE.equals(Config.REACTOR_VALIDATION_DEBUG.get())) {
            ColossalReactors.LOGGER.info("[ReactorValidation] INVALID: {} (start={})", reason, start);
        }
        return invalid();
    }

    private static void debug(String format, Object... args) {
        if (Boolean.TRUE.equals(Config.REACTOR_VALIDATION_DEBUG.get())) {
            ColossalReactors.LOGGER.info("[ReactorValidation] " + format, args);
        }
    }
}
