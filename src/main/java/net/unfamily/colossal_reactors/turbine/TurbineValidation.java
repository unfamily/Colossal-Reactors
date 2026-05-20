package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ModBlocks;

/**
 * Validates turbine multiblock structure (shell, rods, blades, coil zone).
 */
public final class TurbineValidation {

    public record Result(
            boolean valid,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            int bladeCount, int validBladeCount, int coilBlockCount,
            double coilEfficiency, double bladeEfficiency,
            double maxSteamMbPerTick, double estimatedRfPerTick
    ) {
        public static Result invalid() {
            return new Result(false, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
    }

    private TurbineValidation() {}

    public static Result validate(Level level, BlockPos start, Direction intoTurbine) {
        return validate(level, start, intoTurbine, Config.TURBINE_DEFAULT_COIL_LAYER_COUNT.get());
    }

    public static Result validate(Level level, BlockPos start, Direction intoTurbine, int coilLayerCount) {
        if (start == null || level == null || !level.isLoaded(start)) {
            return Result.invalid();
        }
        if (!isShellBlock(level.getBlockState(start))) {
            return Result.invalid();
        }
        if (intoTurbine.getAxis() == Direction.Axis.Y) {
            return Result.invalid();
        }
        Direction left = intoTurbine.getCounterClockWise();
        Direction right = intoTurbine.getClockWise();

        int maxW = Config.MAX_TURBINE_WIDTH.get();
        int maxL = Config.MAX_TURBINE_LENGTH.get();
        int maxH = Config.MAX_TURBINE_HEIGHT.get();
        int maxHorizontal = Math.max(maxW, maxL);

        BlockPos pos = start;
        int steps;
        steps = 0;
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
        while (steps < maxHorizontal && canStepDepth(level, pos, intoTurbine)) {
            pos = pos.relative(intoTurbine);
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
        if (width > maxW || length > maxL || height > maxH) {
            return Result.invalid();
        }

        RegistryAccess registry = level.registryAccess();
        int interiorH = height - 2;
        int coilLayers = TurbineRodSpaceLayout.coilLayerCount(interiorH, coilLayerCount);
        int coilStartInteriorY = TurbineRodSpaceLayout.coilZoneStartY(interiorH, coilLayers);

        int bladeCount = 0;
        int validBladeCount = 0;
        int coilBlockCount = 0;
        double sumCoe = 0;
        double sumMax = 0;

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    boolean onBorder = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                    BlockState state = level.getBlockState(p);
                    if (onBorder) {
                        if (!isShellBlock(state) && !isRodController(state)) {
                            return Result.invalid();
                        }
                    } else {
                        int interiorY = y - minY - 1;
                        boolean coilLayer = interiorY >= coilStartInteriorY;
                        if (state.is(ModBlocks.TURBINE_ROD.get())) {
                            // ok
                        } else if (state.is(ModBlocks.TURBINE_BLADE.get())) {
                            bladeCount++;
                            validBladeCount++;
                        } else if (coilLayer) {
                            if (!ElecCoilLoader.isCoilBlock(state, registry) && !state.isAir()) {
                                return Result.invalid();
                            }
                            if (!state.isAir()) {
                                coilBlockCount++;
                                var m = ElecCoilLoader.getModifiersForBlockOrDefault(state, registry);
                                sumCoe += m.effCoe();
                                sumMax += m.effMax();
                            }
                        } else if (state.isAir()) {
                            // ok in rod zone
                        } else {
                            return Result.invalid();
                        }
                    }
                }
            }
        }

        if (!Boolean.TRUE.equals(Config.ALLOW_MULTIPLE_TURBINE_CONTROLLERS.get())) {
            int controllers = countControllersOnFaces(level, minX, minY, minZ, maxX, maxY, maxZ);
            if (controllers != 1) {
                return Result.invalid();
            }
        }

        double coilEff = coilBlockCount > 0
                ? Math.min(sumCoe / coilBlockCount, sumMax / coilBlockCount)
                : Config.TURBINE_EMPTY_COIL_EFFICIENCY.get();
        Direction axis = findRodControllerAxis(level, minX, minY, minZ, maxX, maxY, maxZ);
        double bladeEff = axis != null
                ? TurbineBladeEfficiency.computeFromBounds(level, minX, minY, minZ, maxX, maxY, maxZ, axis)
                : 1.0;

        if (Boolean.TRUE.equals(Config.TURBINE_REQUIRE_BALANCED_BLADE_RINGS.get())) {
            validBladeCount = countValidBalancedBlades(level, minX, minY, minZ, maxX, maxY, maxZ);
        } else {
            validBladeCount = bladeCount;
        }

        double steamCap = validBladeCount * Config.TURBINE_STEAM_MB_PER_BLADE_PER_TICK.get()
                * Config.TURBINE_CONSUMPTION_MULTIPLIER.get();
        TurbineGenerationDefinition gen = TurbineGenerationLoader.getDefault();
        double rfPerMb = gen != null ? gen.rfProduction() : Config.TURBINE_DEFAULT_RF_PER_STEAM_MB.get();
        double rf = steamCap * rfPerMb * coilEff * bladeEff * Config.TURBINE_PRODUCTION_MULTIPLIER.get();
        rf = Math.max(rf, Config.TURBINE_MIN_RF_PER_TICK.get());

        return new Result(true, minX, minY, minZ, maxX, maxY, maxZ,
                bladeCount, validBladeCount, coilBlockCount, coilEff, bladeEff, steamCap, rf);
    }

    private static int countValidBalancedBlades(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int total = 0;
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (state.is(ModBlocks.TURBINE_ROD.get()) && state.hasProperty(net.unfamily.colossal_reactors.block.TurbineRodBlock.FACING)) {
                        Direction rodAxis = state.getValue(net.unfamily.colossal_reactors.block.TurbineRodBlock.FACING);
                        int blades = TurbineBladePlacement.totalBladesOnRod(level, p, rodAxis);
                        if (blades % 4 == 0) total += blades;
                    }
                }
            }
        }
        return total;
    }

    @javax.annotation.Nullable
    private static Direction findRodControllerAxis(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                BlockState s = level.getBlockState(new BlockPos(x, maxY, z));
                if (s.is(ModBlocks.TURBINE_ROD_CONTROLLER.get()) && s.hasProperty(net.unfamily.colossal_reactors.block.TurbineRodControllerBlock.FACING)) {
                    return s.getValue(net.unfamily.colossal_reactors.block.TurbineRodControllerBlock.FACING);
                }
            }
        }
        return Direction.UP;
    }

    public static boolean isShellBlock(BlockState state) {
        return state.is(ModBlocks.TURBINE_CASING.get())
                || state.is(ModBlocks.TURBINE_GLASS.get())
                || isTurbinePowerPort(state)
                || state.is(ModBlocks.TURBINE_REDSTONE_PORT.get())
                || state.is(ModBlocks.TURBINE_RESOURCE_PORT.get());
    }

    public static boolean isTurbinePowerPort(BlockState state) {
        return state.is(ModBlocks.TURBINE_POWER_PORT.get()) || state.is(ModBlocks.TURBINE_HIGH_COND_POWER_PORT.get());
    }

    private static boolean isShellOrRodController(BlockState state) {
        return isShellBlock(state) || isRodController(state);
    }

    private static boolean isRodController(BlockState state) {
        return state.is(ModBlocks.TURBINE_ROD_CONTROLLER.get());
    }

    private static boolean canStepDepth(Level level, BlockPos pos, Direction into) {
        BlockPos next = pos.relative(into);
        BlockState nextState = level.getBlockState(next);
        BlockState current = level.getBlockState(pos);
        if (isShellOrRodController(nextState)) return true;
        if (nextState.is(ModBlocks.TURBINE_ROD.get()) || nextState.is(ModBlocks.TURBINE_BLADE.get())) return true;
        if (!isShellOrRodController(current) && (nextState.isAir() || ElecCoilLoader.isCoilBlock(nextState, level.registryAccess()))) {
            return true;
        }
        return false;
    }

    private static int countControllersOnFaces(Level level, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int count = 0;
        for (int y = minY; y <= maxY; y++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(minX - 1, y, z)).is(ModBlocks.TURBINE_CONTROLLER.get())) count++;
                if (level.getBlockState(new BlockPos(maxX + 1, y, z)).is(ModBlocks.TURBINE_CONTROLLER.get())) count++;
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                if (level.getBlockState(new BlockPos(x, minY - 1, z)).is(ModBlocks.TURBINE_CONTROLLER.get())) count++;
                if (level.getBlockState(new BlockPos(x, maxY + 1, z)).is(ModBlocks.TURBINE_CONTROLLER.get())) count++;
            }
        }
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (level.getBlockState(new BlockPos(x, y, minZ - 1)).is(ModBlocks.TURBINE_CONTROLLER.get())) count++;
                if (level.getBlockState(new BlockPos(x, y, maxZ + 1)).is(ModBlocks.TURBINE_CONTROLLER.get())) count++;
            }
        }
        return count;
    }
}
