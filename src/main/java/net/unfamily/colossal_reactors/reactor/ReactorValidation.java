package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.network.ModPayloads;
import org.jetbrains.annotations.Nullable;

/**
 * Validates reactor multiblock structure: parallelepiped, casing border, rod columns with rod_controller on top.
 */
public final class ReactorValidation {

    public enum FailureCode {
        BAD_START,
        CONTROLLER_FACE_Y,
        TOO_LARGE,
        SHELL_GAP,
        INTERIOR_UNKNOWN,
        ROD_COLUMN_INCOMPLETE,
        ROD_COUNT_MISMATCH,
        EXTERIOR_CONTROLLER_COUNT
    }

    public record ValidationReport(
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            int width, int height, int length,
            int exteriorControllers,
            int rodColumnsExpected
    ) {
        public static ValidationReport empty() {
            return new ValidationReport(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    public record Result(
            boolean valid,
            @Nullable FailureCode failure,
            @Nullable BlockPos failurePos,
            ValidationReport report,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            int rodCount, int rodColumns, int coolantCount
    ) {
        public static Result invalid() {
            return invalid(FailureCode.BAD_START, null, ValidationReport.empty());
        }

        public static Result invalid(FailureCode failure, @Nullable BlockPos failurePos, ValidationReport report) {
            return new Result(false, failure, failurePos, report,
                    report.minX(), report.minY(), report.minZ(),
                    report.maxX(), report.maxY(), report.maxZ(),
                    0, 0, 0);
        }
    }

    private static final int MARKER_COLOR_ERROR = 0xE0FF0000;
    private static final int MARKER_COLOR_ROD_HINT = 0x80FF8800;
    private static final int MARKER_DURATION_TICKS = 400;

    private ReactorValidation() {}

    public static Result validate(Level level, BlockPos start, Direction intoReactor) {
        ValidationReportBuilder report = new ValidationReportBuilder();

        if (start == null || level == null || !level.isLoaded(start)) {
            return fail(FailureCode.BAD_START, start, report.build());
        }
        if (!isShellBlock(level.getBlockState(start))) {
            return fail(FailureCode.BAD_START, start, report.build());
        }
        if (intoReactor.getAxis() == Direction.Axis.Y) {
            return fail(FailureCode.CONTROLLER_FACE_Y, start, report.build());
        }

        Direction left = intoReactor.getCounterClockWise();
        Direction right = intoReactor.getClockWise();

        int maxW = Config.MAX_REACTOR_WIDTH.get();
        int maxL = Config.MAX_REACTOR_LENGTH.get();
        int maxH = Config.MAX_REACTOR_HEIGHT.get();
        int maxHorizontal = Math.max(maxW, maxL);

        BlockPos pos = start;
        int steps = 0;
        while (steps < maxH && isShellOrRodController(level.getBlockState(pos.below()))) {
            pos = pos.below();
            steps++;
        }
        steps = 0;
        while (steps < maxHorizontal && isShellOrRodController(level.getBlockState(pos.relative(left)))) {
            pos = pos.relative(left);
            steps++;
        }
        BlockPos minCorner = pos;
        steps = 0;
        while (steps < maxH && isShellOrRodController(level.getBlockState(pos.above()))) {
            pos = pos.above();
            steps++;
        }
        steps = 0;
        while (steps < maxHorizontal && isShellOrRodController(level.getBlockState(pos.relative(right)))) {
            pos = pos.relative(right);
            steps++;
        }
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

        report.bounds(minX, minY, minZ, maxX, maxY, maxZ, width, height, length);

        if (width > maxW || length > maxL || height > maxH) {
            return fail(FailureCode.TOO_LARGE, start, report.build());
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
                            return fail(FailureCode.SHELL_GAP, p, report.build());
                        }
                    } else {
                        if (state.is(ModBlocks.REACTOR_ROD.get())) {
                            rodCount++;
                        } else if (state.isAir() || HeatSinkLoader.isHeatSinkBlock(state, level.registryAccess())) {
                            coolantCount++;
                        } else {
                            return fail(FailureCode.INTERIOR_UNKNOWN, p, report.build());
                        }
                    }
                }
            }
        }

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
                            report.rodColumnsExpected(rodColumnsExpected);
                            return fail(FailureCode.ROD_COLUMN_INCOMPLETE, columnPos, report.build());
                        }
                    }
                }
            }
        }
        report.rodColumnsExpected(rodColumnsExpected);

        if (rodColumnsExpected > 0 && rodCount != rodColumnsExpected * (interiorCeilingY - interiorFloorY + 1)) {
            return fail(FailureCode.ROD_COUNT_MISMATCH, new BlockPos(minX + 1, interiorFloorY, minZ + 1), report.build());
        }

        if (!Boolean.TRUE.equals(Config.ALLOW_MULTIPLE_REACTOR_CONTROLLERS.get())) {
            int controllerCount = countReactorControllersOnOuterFaces(level, minX, minY, minZ, maxX, maxY, maxZ);
            report.exteriorControllers(controllerCount);
            if (controllerCount != 1) {
                return fail(FailureCode.EXTERIOR_CONTROLLER_COUNT, null, report.build());
            }
        }

        Result result = new Result(true, null, null, report.build(),
                minX, minY, minZ, maxX, maxY, maxZ, rodCount, rodColumnsExpected, coolantCount);
        logDebug(report.build(), null, true);
        return result;
    }

    private static Result fail(FailureCode failure, @Nullable BlockPos pos, ValidationReport report) {
        logDebug(report, failure, false);
        return Result.invalid(failure, pos, report);
    }

    public static void sendFailureMarkers(ServerPlayer player, Level level, Result result) {
        if (result.valid() || result.failure() == null) {
            return;
        }
        if (result.failurePos() != null) {
            ModPayloads.sendPreviewMarker(player, result.failurePos(), MARKER_COLOR_ERROR, MARKER_DURATION_TICKS);
        }
        if (result.failure() != FailureCode.ROD_COLUMN_INCOMPLETE) {
            return;
        }
        ValidationReport r = result.report();
        if (r.width() <= 0 || r.rodColumnsExpected() <= 0) {
            return;
        }
        int interiorFloorY = r.minY() + 1;
        int interiorCeilingY = r.maxY() - 1;
        for (int x = r.minX() + 1; x < r.maxX(); x++) {
            for (int z = r.minZ() + 1; z < r.maxZ(); z++) {
                if (!level.getBlockState(new BlockPos(x, r.maxY(), z)).is(ModBlocks.ROD_CONTROLLER.get())) {
                    continue;
                }
                for (int y = interiorFloorY; y <= interiorCeilingY; y++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (result.failurePos() != null && result.failurePos().equals(p)) {
                        continue;
                    }
                    if (!level.getBlockState(p).is(ModBlocks.REACTOR_ROD.get())) {
                        ModPayloads.sendPreviewMarker(player, p, MARKER_COLOR_ROD_HINT, MARKER_DURATION_TICKS);
                    }
                }
            }
        }
    }

    public static net.minecraft.network.chat.Component failureMessage(Result result) {
        if (result.valid() || result.failure() == null) {
            return net.minecraft.network.chat.Component.translatable("message.colossal_reactors.reactor_valid");
        }
        return failureMessage(result.failure(), result.report());
    }

    public static net.minecraft.network.chat.Component failureMessage(FailureCode failure, ValidationReport r) {
        return switch (failure) {
            case BAD_START -> net.minecraft.network.chat.Component.translatable(
                    "message.colossal_reactors.reactor_invalid.bad_start");
            case CONTROLLER_FACE_Y -> net.minecraft.network.chat.Component.translatable(
                    "message.colossal_reactors.reactor_invalid.controller_face_y");
            case TOO_LARGE -> net.minecraft.network.chat.Component.translatable(
                    "message.colossal_reactors.reactor_invalid.too_large",
                    r.width(), r.height(), r.length());
            case SHELL_GAP -> net.minecraft.network.chat.Component.translatable(
                    "message.colossal_reactors.reactor_invalid.shell_gap");
            case INTERIOR_UNKNOWN -> net.minecraft.network.chat.Component.translatable(
                    "message.colossal_reactors.reactor_invalid.interior_unknown");
            case ROD_COLUMN_INCOMPLETE -> net.minecraft.network.chat.Component.translatable(
                    "message.colossal_reactors.reactor_invalid.rod_column_incomplete");
            case ROD_COUNT_MISMATCH -> net.minecraft.network.chat.Component.translatable(
                    "message.colossal_reactors.reactor_invalid.rod_count_mismatch");
            case EXTERIOR_CONTROLLER_COUNT -> net.minecraft.network.chat.Component.translatable(
                    "message.colossal_reactors.reactor_invalid.exterior_controller_count", r.exteriorControllers());
        };
    }

    private static void logDebug(ValidationReport report, @Nullable FailureCode failure, boolean valid) {
        if (!Boolean.TRUE.equals(Config.REACTOR_VALIDATION_DEBUG.get())) {
            return;
        }
        ColossalReactors.LOGGER.info(
                "[ReactorValidation] valid={} failure={} bounds=[{} {} {}]..[{} {} {}] size={}x{}x{} extCtrl={} rodCols={}",
                valid, failure,
                report.minX(), report.minY(), report.minZ(), report.maxX(), report.maxY(), report.maxZ(),
                report.width(), report.height(), report.length(),
                report.exteriorControllers(), report.rodColumnsExpected());
    }

    public static boolean isShellBlock(BlockState state) {
        return state.is(ModBlocks.REACTOR_CASING.get())
                || state.is(ModBlocks.REACTOR_GLASS.get())
                || ModBlocks.isPowerPort(state)
                || state.is(ModBlocks.REDSTONE_PORT.get())
                || state.is(ModBlocks.RESOURCE_PORT.get());
    }

    private static boolean isShellOrRodController(BlockState state) {
        return isShellBlock(state) || state.is(ModBlocks.ROD_CONTROLLER.get());
    }

    private static boolean canStepDepth(Level level, BlockPos pos, Direction intoReactor) {
        BlockPos next = pos.relative(intoReactor);
        BlockState nextState = level.getBlockState(next);
        BlockState currentState = level.getBlockState(pos);
        if (isShellOrRodController(nextState)) return true;
        if (nextState.is(ModBlocks.REACTOR_ROD.get())) return true;
        return !isShellOrRodController(currentState)
                && (nextState.isAir() || HeatSinkLoader.isHeatSinkBlock(nextState, level.registryAccess()));
    }

    private static boolean isRodController(BlockState state) {
        return state.is(ModBlocks.ROD_CONTROLLER.get());
    }

    private static int countReactorControllersOnOuterFaces(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(minX - 1, y, z)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(maxX + 1, y, z)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(x, minY - 1, z)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(x, maxY + 1, z)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (level.getBlockState(new BlockPos(x, y, minZ - 1)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (level.getBlockState(new BlockPos(x, y, maxZ + 1)).is(ModBlocks.REACTOR_CONTROLLER.get())) count++;
            }
        }
        return count;
    }

    private static final class ValidationReportBuilder {
        private int minX, minY, minZ, maxX, maxY, maxZ;
        private int width, height, length;
        private int exteriorControllers;
        private int rodColumnsExpected;

        void bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, int w, int h, int l) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
            this.width = w;
            this.height = h;
            this.length = l;
        }

        void exteriorControllers(int count) {
            this.exteriorControllers = count;
        }

        void rodColumnsExpected(int count) {
            this.rodColumnsExpected = count;
        }

        ValidationReport build() {
            return new ValidationReport(minX, minY, minZ, maxX, maxY, maxZ, width, height, length,
                    exteriorControllers, rodColumnsExpected);
        }
    }
}
