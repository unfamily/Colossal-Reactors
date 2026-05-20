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
        int coilStart = TurbineRodSpaceLayout.coilZoneStartY(b.interiorH, builder.getCoilLayerCount());
        for (int x = b.minX; x <= b.maxX; x++) {
            for (int y = b.minY; y <= b.maxY; y++) {
                for (int z = b.minZ; z <= b.maxZ; z++) {
                    BlockState st = level.getBlockState(new BlockPos(x, y, z));
                    boolean border = x == b.minX || x == b.maxX || y == b.minY || y == b.maxY || z == b.minZ || z == b.maxZ;
                    if (border) {
                        if (y == b.maxY && builder.isOpenTop() && st.isAir()) {
                            continue;
                        }
                        if (!st.isAir() && !TurbineValidation.isShellBlock(st) && !st.is(ModBlocks.TURBINE_ROD_CONTROLLER.get())) {
                            return true;
                        }
                    } else {
                        if (y == b.closureWorldY && st.is(ModBlocks.TURBINE_ROD_CONTROLLER.get())) {
                            continue;
                        }
                        if (y - b.minY - 1 >= coilStart) {
                            if (!st.isAir() && !ElecCoilLoader.isCoilBlock(st, level.registryAccess())) {
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

    private static boolean tickFrame(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b, boolean openTop) {
        int x = builder.getBuildFrameX();
        int y = builder.getBuildFrameY();
        int z = builder.getBuildFrameZ();
        if (x == Integer.MIN_VALUE) {
            x = b.minX;
            y = b.minY;
            z = b.minZ;
        }
        for (int xx = x; xx <= b.maxX; xx++) {
            int yy0 = (xx == x) ? y : b.minY;
            for (int yy = yy0; yy <= b.maxY; yy++) {
                int zz0 = (xx == x && yy == yy0) ? z : b.minZ;
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
                    ItemStack frame = resolveFrameStack(builder, preferCasing);
                    if (frame.isEmpty()) {
                        builder.setBuildFrameCursor(xx, yy, zz);
                        return true;
                    }
                    if (tryPlaceFrame(level, builder, pos, frame)) {
                        builder.setBuildFrameCursor(xx, yy, zz + 1);
                        return true;
                    }
                    builder.setBuildFrameCursor(xx, yy, zz + 1);
                }
                builder.setBuildFrameCursor(xx, yy + 1, b.minZ);
            }
            builder.setBuildFrameCursor(xx + 1, b.minY, b.minZ);
        }
        builder.setBuildStage(STAGE_CLOSURE_DECK);
        return true;
    }

    /** Casing deck separating rotor from coil zone; rod-controller cells stay air. */
    private static boolean tickClosureDeck(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        int y = b.closureWorldY;
        TurbineRodControllerLayout.Center center = TurbineRodControllerLayout.bestPrimaryCenter(b.rw, b.rd);
        for (int x = b.minX + 1; x < b.maxX; x++) {
            for (int z = b.minZ + 1; z < b.maxZ; z++) {
                if (TurbineRodControllerLayout.isRodControllerWorldCell(x, z, b.minX, b.minZ, center)) {
                    continue;
                }
                BlockPos pos = new BlockPos(x, y, z);
                BlockState st = level.getBlockState(pos);
                if (st.is(ModBlocks.TURBINE_CASING.get())) {
                    continue;
                }
                if (!canReplace(st)) {
                    continue;
                }
                ItemStack casing = resolveFrameStack(builder, true);
                if (casing.isEmpty()) {
                    return true;
                }
                if (tryPlaceFrame(level, builder, pos, casing)) {
                    return true;
                }
            }
        }
        builder.setBuildStage(STAGE_ROD_CONTROLLERS);
        return true;
    }

    private static boolean tickRodControllers(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        if (b.rw <= 0 || b.rd <= 0) {
            builder.setBuildStage(STAGE_RODS);
            return true;
        }
        TurbineRodControllerLayout.Center center = TurbineRodControllerLayout.bestPrimaryCenter(b.rw, b.rd);
        BlockPos pos = new BlockPos(
                TurbineRodControllerLayout.closureWorldX(b.minX, center.rx()),
                b.closureWorldY,
                TurbineRodControllerLayout.closureWorldZ(b.minZ, center.rz()));
        Direction axis = builder.getPlacementAxis();
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

    private static boolean tickRods(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        int inset = TurbineRodSpaceLayout.rodSpaceInset();
        Direction axis = builder.getPlacementAxis();
        int x0 = b.minX + 1 + inset;
        int x1 = b.minX + inset + b.rw;
        int z0 = b.minZ + 1 + inset;
        int z1 = b.minZ + inset + b.rd;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int rx = x - b.minX - 1 - inset;
                int rz = z - b.minZ - 1 - inset;
                if (!TurbineRodPatternLogic.isRodColumn(rx, rz, b.rw, b.rd, builder.getRodPattern())) {
                    continue;
                }
                for (int y = b.minY + 1; y < b.closureWorldY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState existing = level.getBlockState(pos);
                    if (existing.is(ModBlocks.TURBINE_ROD.get())
                            && existing.hasProperty(TurbineRodBlock.FACING)
                            && existing.getValue(TurbineRodBlock.FACING) == axis) {
                        continue;
                    }
                    BlockState rod = ModBlocks.TURBINE_ROD.get().defaultBlockState()
                            .setValue(TurbineRodBlock.FACING, axis);
                    PlaceResult result = tryPlaceState(level, builder, pos, rod);
                    if (result == PlaceResult.PLACED || result == PlaceResult.WAIT) {
                        return true;
                    }
                }
            }
        }
        builder.setBuildStage(STAGE_BLADES);
        return true;
    }

    private static boolean tickBlades(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        int inset = TurbineRodSpaceLayout.rodSpaceInset();
        int x0 = b.minX + 1 + inset;
        int x1 = b.minX + inset + b.rw;
        int z0 = b.minZ + 1 + inset;
        int z1 = b.minZ + inset + b.rd;
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                int rx = x - b.minX - 1 - inset;
                int rz = z - b.minZ - 1 - inset;
                if (!TurbineRodPatternLogic.isRodColumn(rx, rz, b.rw, b.rd, builder.getRodPattern())) {
                    continue;
                }
                for (int y = b.minY + 1; y < b.closureWorldY; y++) {
                    int iy = y - b.minY - 1;
                    BlockPos rodPos = new BlockPos(x, y, z);
                    BlockState rodState = level.getBlockState(rodPos);
                    if (!rodState.is(ModBlocks.TURBINE_ROD.get())) continue;
                    Direction axis = rodState.getValue(TurbineRodBlock.FACING);
                    int targetRing = TurbineRodPatternLogic.targetBladeRingForLayer(iy, b.rh, builder.getRodPattern());
                    if (placeBladesToRing(level, builder, rodPos, rodState, axis, targetRing)) {
                        return true;
                    }
                }
            }
        }
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

    private static boolean tickCoils(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        int coilStart = TurbineRodSpaceLayout.coilZoneStartY(b.interiorH, builder.getCoilLayerCount());
        int idx = builder.getSelectedCoilIndex();
        if (ElecCoilLoader.shouldSkipSolidCoilAutoPlacement(idx)) {
            builder.setBuildStage(STAGE_DONE);
            return false;
        }
        for (int x = b.minX + 1; x < b.maxX; x++) {
            for (int z = b.minZ + 1; z < b.maxZ; z++) {
                for (int y = b.minY + 1 + coilStart; y < b.maxY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState st = level.getBlockState(pos);
                    if (ElecCoilLoader.isBlockMatchingSelectedCoil(st, idx, level.registryAccess())) {
                        continue;
                    }
                    Block block = coilBlockFor(idx, level);
                    if (block == null) {
                        continue;
                    }
                    PlaceResult result = tryPlaceState(level, builder, pos, block.defaultBlockState());
                    if (result == PlaceResult.PLACED || result == PlaceResult.WAIT) {
                        return true;
                    }
                }
            }
        }
        builder.setBuildStage(STAGE_DONE);
        return false;
    }

    private static Block coilBlockFor(int idx, ServerLevel level) {
        if (idx < 0 || idx >= ElecCoilLoader.getAllDefinitions().size()) return null;
        var def = ElecCoilLoader.getAllDefinitions().get(idx);
        if (def.validBlocks().isEmpty()) return null;
        String sel = def.validBlocks().getFirst();
        if (sel.startsWith("#")) return null;
        var id = net.minecraft.resources.Identifier.tryParse(sel);
        if (id == null) return null;
        return level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK).getValue(id);
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

    private static boolean tryPlaceFrame(ServerLevel level, TurbineBuilderBlockEntity builder, BlockPos pos, ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        if (block != ModBlocks.TURBINE_CASING.get() && block != ModBlocks.TURBINE_GLASS.get()) {
            return false;
        }
        if (!consumeOne(builder, stack.getItem())) {
            return false;
        }
        return level.setBlock(pos, block.defaultBlockState(), net.minecraft.world.level.block.Block.UPDATE_ALL);
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
        int closureWorldY = TurbineRodControllerLayout.closureWorldY(minY, interiorH, coilLayers);
        return new BuildBounds(minX, minY, minZ, maxX, maxY, maxZ, w, h, d,
                TurbineRodPatternLogic.rodSpaceWidth(w),
                TurbineRodPatternLogic.rodSpaceHeight(h, coilLayers),
                TurbineRodPatternLogic.rodSpaceDepth(d),
                interiorH, coilLayers, closureWorldY);
    }

    private record BuildBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                               int w, int h, int d, int rw, int rh, int rd, int interiorH,
                               int coilLayers, int closureWorldY) {}
}
