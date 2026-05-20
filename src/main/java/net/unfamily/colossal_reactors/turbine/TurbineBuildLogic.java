package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineBuilderBlock;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;
import net.unfamily.colossal_reactors.block.TurbineRodControllerBlock;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;
import net.unfamily.colossal_reactors.item.ModItems;

/**
 * Server-side turbine build: frame, closure deck, rod controller, rods, blades, optional coil blocks.
 */
public final class TurbineBuildLogic {

    public static final int STAGE_FRAME = 0;
    public static final int STAGE_CLOSURE_DECK = 1;
    public static final int STAGE_ROD_CONTROLLERS = 2;
    public static final int STAGE_RODS = 3;
    public static final int STAGE_BLADES = 4;
    public static final int STAGE_COILS = 5;
    public static final int STAGE_DONE = 6;

    private enum PlaceResult { PLACED, WAIT, SKIP }

    private TurbineBuildLogic() {}

    public static boolean hasRedZone(ServerLevel level, TurbineBuilderBlockEntity builder) {
        if (level == null || builder == null) return true;
        BuildBounds b = bounds(level, builder);
        if (b == null) return true;
        TurbineRotorLayout layout = b.layout;
        int coilStart = layout.coilStartInterior();
        BlockPos controllerPos = layout.controllerPos(layout.primaryCenter());
        for (int x = b.minX; x <= b.maxX; x++) {
            for (int y = b.minY; y <= b.maxY; y++) {
                for (int z = b.minZ; z <= b.maxZ; z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(p);
                    boolean border = x == b.minX || x == b.maxX || y == b.minY || y == b.maxY || z == b.minZ || z == b.maxZ;
                    if (border) {
                        if (y == b.maxY && builder.isOpenTop() && st.isAir()) {
                            continue;
                        }
                        if (!st.isAir() && !TurbineValidation.isShellBlock(st) && !st.is(ModBlocks.TURBINE_ROD_CONTROLLER.get())) {
                            return true;
                        }
                    } else {
                        if (p.equals(controllerPos) && st.is(ModBlocks.TURBINE_ROD_CONTROLLER.get())) {
                            continue;
                        }
                        if (layout.isCoilZoneWorld(x, y, z)) {
                            if (!st.isAir() && !ElecCoilLoader.isCoilBlock(st, level.registryAccess())
                                    && !st.is(ModBlocks.TURBINE_CASING.get())
                                    && !st.is(ModBlocks.TURBINE_GLASS.get())) {
                                return true;
                            }
                        } else if (!st.isAir() && !st.is(ModBlocks.TURBINE_ROD.get())
                                && !st.is(ModBlocks.TURBINE_BLADE.get())
                                && !st.is(ModBlocks.TURBINE_CASING.get())
                                && !st.is(ModBlocks.TURBINE_GLASS.get())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean tick(TurbineBuilderBlockEntity builder, int steps) {
        for (int i = 0; i < Math.max(1, steps); i++) {
            if (!tickOnce(builder)) {
                return false;
            }
        }
        return builder.getBuildStage() < STAGE_DONE;
    }

    private static boolean tickOnce(TurbineBuilderBlockEntity builder) {
        ServerLevel level = (ServerLevel) builder.getLevel();
        if (level == null) return false;
        BuildBounds b = bounds(level, builder);
        if (b == null) return false;
        if (hasRedZone(level, builder)) {
            builder.stopBuild(false);
            return false;
        }
        boolean openTop = builder.isOpenTop();
        return switch (builder.getBuildStage()) {
            case STAGE_FRAME -> tickFrame(level, builder, b, openTop);
            case STAGE_CLOSURE_DECK -> tickClosureDeck(level, builder, b);
            case STAGE_ROD_CONTROLLERS -> tickRodControllers(level, builder, b);
            case STAGE_RODS -> tickRods(level, builder, b);
            case STAGE_BLADES -> tickBlades(level, builder, b);
            case STAGE_COILS -> tickCoils(level, builder, b);
            default -> {
                builder.setBuildStage(STAGE_DONE);
                yield false;
            }
        };
    }

    /** Frame shell: bottom Y layer first, then upward (same spirit as {@link net.unfamily.colossal_reactors.reactor.ReactorBuildLogic#tickRods}). */
    private static boolean tickFrame(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b, boolean openTop) {
        int x = builder.getBuildFrameX();
        int y = builder.getBuildFrameY();
        int z = builder.getBuildFrameZ();
        if (y == Integer.MIN_VALUE) {
            y = b.minY;
            x = b.minX;
            z = b.minZ;
        }
        for (int yy = y; yy <= b.maxY; yy++) {
            int xx0 = (yy == y) ? x : b.minX;
            for (int xx = xx0; xx <= b.maxX; xx++) {
                int zz0 = (yy == y && xx == xx0) ? z : b.minZ;
                for (int zz = zz0; zz <= b.maxZ; zz++) {
                    if (yy == b.maxY && openTop) {
                        builder.setBuildFrameCursor(xx, yy, zz + 1);
                        continue;
                    }
                    boolean onBorder = (xx == b.minX || xx == b.maxX || yy == b.minY || yy == b.maxY || zz == b.minZ || zz == b.maxZ);
                    if (!onBorder) {
                        builder.setBuildFrameCursor(xx, yy, zz + 1);
                        continue;
                    }
                    BlockPos pos = new BlockPos(xx, yy, zz);
                    BlockState existing = level.getBlockState(pos);
                    if (!canReplace(existing)) {
                        builder.setBuildFrameCursor(xx, yy, zz + 1);
                        continue;
                    }
                    boolean edgeOrCorner = isEdgeOrCorner(xx, yy, zz, b.minX, b.minY, b.minZ, b.maxX, b.maxY, b.maxZ);
                    boolean topOrBottomFace = (yy == b.minY || yy == b.maxY);
                    boolean preferCasing = edgeOrCorner || topOrBottomFace;
                    if (resolveFrameStack(builder, preferCasing).isEmpty()) {
                        builder.setBuildFrameCursor(xx, yy, zz);
                        return true;
                    }
                    if (tryPlaceFrame(level, builder, pos, preferCasing)) {
                        builder.setBuildFrameCursor(xx, yy, zz + 1);
                        return true;
                    }
                    builder.setBuildFrameCursor(xx, yy, zz + 1);
                }
                builder.setBuildFrameCursor(xx + 1, yy, b.minZ);
            }
            builder.setBuildFrameCursor(b.minX, yy + 1, b.minZ);
        }
        builder.setBuildFrameCursor(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        builder.setBuildStage(STAGE_CLOSURE_DECK);
        return true;
    }

    /** Casing deck separating rotor from coil zone; rod-controller cells stay air. */
    private static boolean tickClosureDeck(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        TurbineRotorLayout layout = b.layout;
        TurbineRodControllerLayout.Center center = layout.primaryCenter();
        int closure = layout.closureCoord();
        int x0 = builder.getBuildFrameX();
        int y0 = builder.getBuildFrameY();
        int z0 = builder.getBuildFrameZ();
        if (x0 == Integer.MIN_VALUE) {
            x0 = b.minX + 1;
            y0 = b.minY + 1;
            z0 = b.minZ + 1;
        }
        return switch (layout.growthAxis().getAxis()) {
            case Y -> tickClosureDeckY(level, builder, b, layout, center, closure, x0, z0);
            case Z -> tickClosureDeckZ(level, builder, b, layout, center, closure, x0, y0);
            case X -> tickClosureDeckX(level, builder, b, layout, center, closure, y0, z0);
            default -> {
                builder.setBuildStage(STAGE_ROD_CONTROLLERS);
                yield true;
            }
        };
    }

    private static boolean tickClosureDeckY(
            ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b,
            TurbineRotorLayout layout, TurbineRodControllerLayout.Center center, int closure,
            int x0, int z0) {
        for (int xx = x0; xx < b.maxX; xx++) {
            int zz0 = (xx == x0) ? z0 : b.minZ + 1;
            for (int zz = zz0; zz < b.maxZ; zz++) {
                builder.setBuildFrameCursor(xx, closure, zz + 1);
                if (layout.isRodControllerAt(xx, closure, zz, center)) {
                    continue;
                }
                if (tryPlaceClosureCasing(level, builder, new BlockPos(xx, closure, zz))) {
                    return true;
                }
            }
            builder.setBuildFrameCursor(xx + 1, closure, b.minZ + 1);
        }
        finishClosureDeck(builder);
        return true;
    }

    private static boolean tickClosureDeckZ(
            ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b,
            TurbineRotorLayout layout, TurbineRodControllerLayout.Center center, int closure,
            int x0, int y0) {
        for (int xx = x0; xx < b.maxX; xx++) {
            int yy0 = (xx == x0) ? y0 : b.minY + 1;
            for (int yy = yy0; yy < b.maxY; yy++) {
                builder.setBuildFrameCursor(xx, yy, closure + 1);
                if (layout.isRodControllerAt(xx, yy, closure, center)) {
                    continue;
                }
                if (tryPlaceClosureCasing(level, builder, new BlockPos(xx, yy, closure))) {
                    return true;
                }
            }
            builder.setBuildFrameCursor(xx + 1, b.minY + 1, closure);
        }
        finishClosureDeck(builder);
        return true;
    }

    private static boolean tickClosureDeckX(
            ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b,
            TurbineRotorLayout layout, TurbineRodControllerLayout.Center center, int closure,
            int y0, int z0) {
        for (int yy = y0; yy < b.maxY; yy++) {
            int zz0 = (yy == y0) ? z0 : b.minZ + 1;
            for (int zz = zz0; zz < b.maxZ; zz++) {
                builder.setBuildFrameCursor(closure, yy, zz + 1);
                if (layout.isRodControllerAt(closure, yy, zz, center)) {
                    continue;
                }
                if (tryPlaceClosureCasing(level, builder, new BlockPos(closure, yy, zz))) {
                    return true;
                }
            }
            builder.setBuildFrameCursor(closure, yy + 1, b.minZ + 1);
        }
        finishClosureDeck(builder);
        return true;
    }

    private static boolean tryPlaceClosureCasing(ServerLevel level, TurbineBuilderBlockEntity builder, BlockPos pos) {
        BlockState st = level.getBlockState(pos);
        if (st.is(ModBlocks.TURBINE_CASING.get()) || !canReplace(st)) {
            return false;
        }
        if (resolveFrameStack(builder, true).isEmpty()) {
            builder.setBuildFrameCursor(pos.getX(), pos.getY(), pos.getZ());
            return true;
        }
        return tryPlaceFrame(level, builder, pos, true);
    }

    private static void finishClosureDeck(TurbineBuilderBlockEntity builder) {
        builder.setBuildFrameCursor(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        builder.setBuildStage(STAGE_ROD_CONTROLLERS);
    }

    private static boolean tickRodControllers(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        TurbineRotorLayout layout = b.layout;
        if (layout.crossSizeA() <= 0 || layout.crossSizeB() <= 0) {
            builder.setBuildStage(STAGE_RODS);
            return true;
        }
        TurbineRodControllerLayout.Center center = layout.primaryCenter();
        BlockPos pos = layout.controllerPos(center);
        Direction axis = layout.growthAxis();
        BlockState desired = ModBlocks.TURBINE_ROD_CONTROLLER.get().defaultBlockState()
                .setValue(TurbineRodControllerBlock.FACING, axis);
        BlockState current = level.getBlockState(pos);
        if (current.is(ModBlocks.TURBINE_ROD_CONTROLLER.get())
                && current.hasProperty(TurbineRodControllerBlock.FACING)
                && current.getValue(TurbineRodControllerBlock.FACING) == axis) {
            builder.setBuildRodCtrlCursor(center.rx(), center.rz());
            builder.setBuildStage(STAGE_RODS);
            return true;
        }
        PlaceResult result = tryPlaceState(level, builder, pos, desired);
        if (result == PlaceResult.WAIT) {
            return true;
        }
        if (result == PlaceResult.PLACED) {
            builder.setBuildRodCtrlCursor(center.rx(), center.rz());
            builder.setBuildStage(STAGE_RODS);
            return true;
        }
        builder.setBuildStage(STAGE_RODS);
        return true;
    }

    /** Rods along placement axis, then cross-section scan (matches reactor rod tick order). */
    private static boolean tickRods(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        TurbineRotorLayout layout = b.layout;
        Direction axis = layout.growthAxis();
        int t0 = builder.getBuildRodLy();
        int ca0 = builder.getBuildRodLx();
        int cb0 = builder.getBuildRodLz();
        if (t0 == Integer.MIN_VALUE) {
            t0 = 0;
            ca0 = 0;
            cb0 = 0;
        }
        for (int t = t0; t < layout.rodExtent(); t++) {
            int caStart = (t == t0) ? ca0 : 0;
            for (int ca = caStart; ca < layout.crossSizeA(); ca++) {
                int cbStart = (t == t0 && ca == caStart) ? cb0 : 0;
                for (int cb = cbStart; cb < layout.crossSizeB(); cb++) {
                    builder.setBuildRodCursor(ca, t, cb + 1);
                    if (!TurbineRodPatternLogic.isRodColumn(ca, cb, layout.crossSizeA(), layout.crossSizeB(), builder.getRodPattern())) {
                        continue;
                    }
                    BlockPos pos = layout.rodPos(t, ca, cb);
                    BlockState existing = level.getBlockState(pos);
                    if (existing.is(ModBlocks.TURBINE_ROD.get())
                            && existing.hasProperty(TurbineRodBlock.FACING)
                            && existing.getValue(TurbineRodBlock.FACING) == axis) {
                        continue;
                    }
                    BlockState rod = ModBlocks.TURBINE_ROD.get().defaultBlockState()
                            .setValue(TurbineRodBlock.FACING, axis);
                    PlaceResult result = tryPlaceState(level, builder, pos, rod);
                    if (result == PlaceResult.WAIT) {
                        builder.setBuildRodCursor(ca, t, cb);
                        return true;
                    }
                    if (result == PlaceResult.PLACED) {
                        return true;
                    }
                }
                builder.setBuildRodCursor(ca + 1, t, 0);
            }
            builder.setBuildRodCursor(0, t + 1, 0);
        }
        builder.setBuildRodCursor(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        builder.setBuildStage(STAGE_BLADES);
        return true;
    }

    private static boolean tickBlades(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        TurbineRotorLayout layout = b.layout;
        int t0 = builder.getBuildRodLy();
        int ca0 = builder.getBuildRodLx();
        int cb0 = builder.getBuildRodLz();
        if (t0 == Integer.MIN_VALUE) {
            t0 = 0;
            ca0 = 0;
            cb0 = 0;
        }
        for (int t = t0; t < layout.rodExtent(); t++) {
            int caStart = (t == t0) ? ca0 : 0;
            for (int ca = caStart; ca < layout.crossSizeA(); ca++) {
                int cbStart = (t == t0 && ca == caStart) ? cb0 : 0;
                for (int cb = cbStart; cb < layout.crossSizeB(); cb++) {
                    builder.setBuildRodCursor(ca, t, cb + 1);
                    if (!TurbineRodPatternLogic.isRodColumn(ca, cb, layout.crossSizeA(), layout.crossSizeB(), builder.getRodPattern())) {
                        continue;
                    }
                    BlockPos rodPos = layout.rodPos(t, ca, cb);
                    BlockState rodState = level.getBlockState(rodPos);
                    if (!rodState.is(ModBlocks.TURBINE_ROD.get())) {
                        continue;
                    }
                    Direction axis = rodState.getValue(TurbineRodBlock.FACING);
                    int targetRing = TurbineRodPatternLogic.targetBladeRingForLayer(t, layout.rodExtent(), builder.getRodPattern());
                    if (placeBladesToRing(level, builder, rodPos, rodState, axis, targetRing)) {
                        builder.setBuildRodCursor(ca, t, cb);
                        return true;
                    }
                }
                builder.setBuildRodCursor(ca + 1, t, 0);
            }
            builder.setBuildRodCursor(0, t + 1, 0);
        }
        builder.setBuildRodCursor(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE);
        builder.setBuildStage(STAGE_COILS);
        return true;
    }

    private static boolean placeBladesToRing(ServerLevel level, TurbineBuilderBlockEntity builder,
                                             BlockPos rodPos, BlockState rodState, Direction axis, int targetRing) {
        while (TurbineBladePlacement.currentRing(level, rodPos, axis) <= targetRing
                && TurbineBladePlacement.placeNextBlade(level, rodPos, rodState)) {
            if (!consumeItem(level, builder, ModItems.TURBINE_BLADE.get())) {
                return true;
            }
        }
        return false;
    }

    /** Coils: last build stage — fill entire coil zone from buffer (supports tag selectors). */
    private static boolean tickCoils(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        TurbineRotorLayout layout = b.layout;
        int idx = builder.getSelectedCoilIndex();
        if (ElecCoilLoader.shouldSkipSolidCoilAutoPlacement(idx)) {
            builder.setBuildStage(STAGE_DONE);
            return false;
        }
        BlockState coilState = ElecCoilLoader.placementStateForOption(idx, level.registryAccess());
        if (coilState == null) {
            builder.setBuildStage(STAGE_DONE);
            return false;
        }
        int axisStart = layout.coilLoopStart();
        int axisEnd = layout.coilLoopEndExclusive();
        int axisStep = layout.coilLoopStep();
        int axisLayer0 = builder.getBuildRodLy();
        int x0 = builder.getBuildRodLx();
        int y0 = builder.getBuildRodLz();
        int z0 = builder.getBuildFrameZ();
        if (axisLayer0 == Integer.MIN_VALUE) {
            axisLayer0 = axisStart;
            x0 = b.minX + 1;
            y0 = b.minY + 1;
            z0 = b.minZ + 1;
        }
        for (int axisLayer = axisLayer0; axisStep > 0 ? axisLayer < axisEnd : axisLayer > axisEnd; axisLayer += axisStep) {
            int xx0 = (axisLayer == axisLayer0) ? x0 : b.minX + 1;
            for (int xx = xx0; xx < b.maxX; xx++) {
                int yy0 = (axisLayer == axisLayer0 && xx == xx0) ? y0 : b.minY + 1;
                for (int yy = yy0; yy < b.maxY; yy++) {
                    int zz0 = (axisLayer == axisLayer0 && xx == xx0 && yy == yy0) ? z0 : b.minZ + 1;
                    for (int zz = zz0; zz < b.maxZ; zz++) {
                        builder.setBuildRodCursor(xx, axisLayer, zz + 1);
                        builder.setBuildFrameCursor(xx, yy, zz);
                        if (layout.worldAxisCoord(xx, yy, zz) != axisLayer) {
                            continue;
                        }
                        if (!layout.isCoilZoneWorld(xx, yy, zz)) {
                            continue;
                        }
                        BlockPos pos = new BlockPos(xx, yy, zz);
                        BlockState existing = level.getBlockState(pos);
                        if (ElecCoilLoader.isBlockMatchingSelectedCoil(existing, idx, level.registryAccess())) {
                            continue;
                        }
                        if (!canReplace(existing)) {
                            continue;
                        }
                        if (!consumeCoilFromBuffer(builder, idx, level)) {
                            builder.setBuildRodCursor(xx, axisLayer, zz);
                            builder.setBuildFrameCursor(xx, yy, zz);
                            return true;
                        }
                        level.setBlock(pos, coilState, net.minecraft.world.level.block.Block.UPDATE_ALL);
                        return true;
                    }
                    builder.setBuildRodCursor(xx, axisLayer, b.minZ + 1);
                    builder.setBuildFrameCursor(xx, yy, b.minZ + 1);
                }
                builder.setBuildRodCursor(b.minX + 1, axisLayer, b.minZ + 1);
            }
            builder.setBuildRodCursor(b.minX + 1, axisLayer + axisStep, b.minZ + 1);
            builder.setBuildFrameCursor(b.minX + 1, b.minY + 1, b.minZ + 1);
        }
        builder.setBuildStage(STAGE_DONE);
        return false;
    }

    private static boolean consumeCoilFromBuffer(
            TurbineBuilderBlockEntity builder, int coilIndex, ServerLevel level) {
        var handler = builder.getBufferHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack stack = handler.getStackInSlot(i);
            if (!stack.isEmpty()
                    && ElecCoilLoader.itemMatchesSelectedCoil(stack, coilIndex, level.registryAccess())) {
                handler.extractItem(i, 1, false);
                return true;
            }
        }
        return false;
    }

    private static boolean isOnClosurePlane(int x, int y, int z, TurbineRotorLayout layout) {
        return switch (layout.growthAxis().getAxis()) {
            case Y -> y == layout.closureCoord();
            case Z -> z == layout.closureCoord();
            case X -> x == layout.closureCoord();
            default -> false;
        };
    }

    private static boolean isEdgeOrCorner(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int onBoundary = 0;
        if (x == minX || x == maxX) onBoundary++;
        if (y == minY || y == maxY) onBoundary++;
        if (z == minZ || z == maxZ) onBoundary++;
        return onBoundary >= 2;
    }

    private static ItemStack findCasingItem(TurbineBuilderBlockEntity builder) {
        Block casing = ModBlocks.TURBINE_CASING.get();
        for (int i = 0; i < builder.getBufferHandler().getSlots(); i++) {
            ItemStack stack = builder.getBufferHandler().getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(casing.asItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findGlassItem(TurbineBuilderBlockEntity builder) {
        Block glass = ModBlocks.TURBINE_GLASS.get();
        for (int i = 0; i < builder.getBufferHandler().getSlots(); i++) {
            ItemStack stack = builder.getBufferHandler().getStackInSlot(i);
            if (!stack.isEmpty() && stack.is(glass.asItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack resolveFrameStack(TurbineBuilderBlockEntity builder, boolean preferCasing) {
        ItemStack primary = preferCasing ? findCasingItem(builder) : findGlassItem(builder);
        if (!primary.isEmpty()) {
            return primary;
        }
        return preferCasing ? findGlassItem(builder) : findCasingItem(builder);
    }

    /**
     * Consumes casing or glass from buffer (alias), but places the block required for this face.
     */
    private static boolean tryPlaceFrame(ServerLevel level, TurbineBuilderBlockEntity builder, BlockPos pos, boolean preferCasing) {
        ItemStack stack = resolveFrameStack(builder, preferCasing);
        if (stack.isEmpty() || !consumeOne(builder, stack.getItem())) {
            return false;
        }
        Block placed = preferCasing ? ModBlocks.TURBINE_CASING.get() : ModBlocks.TURBINE_GLASS.get();
        return level.setBlock(pos, placed.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
    }

    private static PlaceResult tryPlaceState(ServerLevel level, TurbineBuilderBlockEntity builder, BlockPos pos, BlockState state) {
        BlockState existing = level.getBlockState(pos);
        if (!canReplace(existing)) {
            return PlaceResult.SKIP;
        }
        if (!consumeForBlock(builder, state.getBlock())) {
            return PlaceResult.WAIT;
        }
        level.setBlock(pos, state, net.minecraft.world.level.block.Block.UPDATE_ALL);
        return PlaceResult.PLACED;
    }

    private static boolean canReplace(BlockState existing) {
        return existing.canBeReplaced() || existing.isAir();
    }

    private static boolean consumeForBlock(TurbineBuilderBlockEntity builder, Block block) {
        ServerLevel level = (ServerLevel) builder.getLevel();
        if (level == null) return false;
        if (block == ModBlocks.TURBINE_BLADE.get()) {
            return consumeItem(level, builder, ModItems.TURBINE_BLADE.get());
        }
        ItemStack stack = new ItemStack(block);
        return consumeItem(level, builder, stack.getItem());
    }

    private static boolean consumeOne(TurbineBuilderBlockEntity builder, net.minecraft.world.item.Item item) {
        ServerLevel level = (ServerLevel) builder.getLevel();
        if (level == null) return false;
        return consumeItem(level, builder, item);
    }

    private static boolean consumeItem(ServerLevel level, TurbineBuilderBlockEntity builder, net.minecraft.world.item.Item item) {
        var handler = builder.getBufferHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (!s.isEmpty() && s.is(item)) {
                handler.extractItem(i, 1, false);
                return true;
            }
        }
        return false;
    }

    private static BuildBounds bounds(ServerLevel level, TurbineBuilderBlockEntity builder) {
        BlockState st = level.getBlockState(builder.getBlockPos());
        if (!(st.getBlock() instanceof TurbineBuilderBlock)) return null;
        Direction facing = st.getValue(TurbineBuilderBlock.FACING);
        var aabb = TurbineBuilderBlockEntity.getTurbineVolumeAABB(
                builder.getBlockPos(), facing,
                builder.getSizeLeft(), builder.getSizeRight(),
                builder.getSizeHeight(), builder.getSizeDepth());
        int minX = (int) Math.floor(aabb.minX);
        int minY = (int) Math.floor(aabb.minY);
        int minZ = (int) Math.floor(aabb.minZ);
        int maxX = (int) Math.floor(aabb.maxX - 1e-6);
        int maxY = (int) Math.floor(aabb.maxY - 1e-6);
        int maxZ = (int) Math.floor(aabb.maxZ - 1e-6);
        int w = maxX - minX + 1;
        int h = maxY - minY + 1;
        int d = maxZ - minZ + 1;
        int interiorH = TurbineRodSpaceLayout.interiorHeight(h);
        int coilLayers = builder.getCoilLayerCount();
        Direction growthAxis = builder.getPlacementAxis();
        TurbineRotorLayout layout = TurbineRotorLayout.from(
                minX, minY, minZ, maxX, maxY, maxZ, w, h, d, coilLayers, growthAxis);
        int closureWorldY = growthAxis.getAxis() == Direction.Axis.Y
                ? layout.closureCoord()
                : TurbineRodControllerLayout.closureWorldY(minY, interiorH, coilLayers);
        return new BuildBounds(minX, minY, minZ, maxX, maxY, maxZ, w, h, d,
                TurbineRodPatternLogic.rodSpaceWidth(w),
                TurbineRodPatternLogic.rodSpaceHeight(h, coilLayers),
                TurbineRodPatternLogic.rodSpaceDepth(d),
                interiorH, coilLayers, closureWorldY, layout);
    }

    private record BuildBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                               int w, int h, int d, int rw, int rh, int rd, int interiorH,
                               int coilLayers, int closureWorldY, TurbineRotorLayout layout) {}
}
