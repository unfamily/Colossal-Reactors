package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.ReactorBuilderBlock;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.reactor.RodPatternLogic;

/**
 * Server-side reactor build: checks red zones and places blocks from builder inventory/tank.
 * Order: frame (except top if openTop), rod controllers, interior rods, interior liquids (1000mb each), interior heat sink blocks.
 * Cannot replace existing blocks. When no materials available, waits. On red zone or stop, returns to Build state.
 */
public final class ReactorBuildLogic {

    private static final int MB_PER_LIQUID_BLOCK = 1000;

    private ReactorBuildLogic() {}

    /**
     * Returns true if the reactor volume has any "red zone" (block that doesn't belong there).
     * Same logic as preview red markers: frame must be shell or rod controller on top; interior rod = rod block; interior else = air or heat sink.
     */
    public static boolean hasRedZone(ServerLevel level, ReactorBuilderBlockEntity builder) {
        if (level == null || builder == null || builder.getLevel() != level) return true;
        BlockState builderState = level.getBlockState(builder.getBlockPos());
        if (!(builderState.getBlock() instanceof ReactorBuilderBlock)) return true;
        var facing = builderState.getValue(ReactorBuilderBlock.FACING);
        var aabb = ReactorBuilderBlockEntity.getReactorVolumeAABB(
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
        int rw = RodPatternLogic.rodSpaceWidth(w, builder.getPatternMode());
        int rd = RodPatternLogic.rodSpaceDepth(d, builder.getPatternMode());
        int insetXZ = RodPatternLogic.rodSpaceInsetXZ(builder.getPatternMode());
        int pattern = builder.getRodPattern();
        boolean expansionRodAtCenter = (pattern == RodPatternLogic.PATTERN_EXPANSION)
                ? RodPatternLogic.getExpansionRodAtCenterForPreview(rw, rd)
                : false;
        var registryAccess = level.registryAccess();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockState blockState = level.getBlockState(new BlockPos(x, y, z));
                    int lx = x - minX, ly = y - minY, lz = z - minZ;
                    boolean onBorder = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                    boolean hasBlock = !blockState.isAir() && !blockState.canBeReplaced();

                    if (onBorder) {
                        boolean validFrame = ReactorValidation.isShellBlock(blockState)
                                || (blockState.is(ModBlocks.ROD_CONTROLLER.get()) && y == maxY && isRodControllerPos(x, z, minX, minZ, maxY, insetXZ, rw, rd, pattern, expansionRodAtCenter));
                        if (hasBlock && !validFrame) return true;
                    } else {
                        boolean isRodPos = (lx >= insetXZ && lx < w - insetXZ && ly >= 1 && ly < h - 1 && lz >= insetXZ && lz < d - insetXZ)
                                && RodPatternLogic.isRodForPreview(lx - insetXZ, ly - 1, lz - insetXZ,
                                rw, RodPatternLogic.rodSpaceHeight(h, builder.getPatternMode()), rd, pattern, expansionRodAtCenter);
                        if (isRodPos) {
                            if (hasBlock && !blockState.is(ModBlocks.REACTOR_ROD.get())) return true;
                        } else {
                            if (hasBlock && !HeatSinkLoader.isHeatSinkBlock(blockState, registryAccess)) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isRodControllerPos(int x, int z, int minX, int minZ, int maxY, int insetXZ, int rw, int rd, int pattern, boolean expansionRodAtCenter) {
        int rx = x - minX - insetXZ;
        int rz = z - minZ - insetXZ;
        if (rx < 0 || rx >= rw || rz < 0 || rz >= rd) return false;
        return RodPatternLogic.isRodColumnForPreview(rx, rz, rw, rd, pattern, expansionRodAtCenter);
    }

    /**
     * One build step. Returns true if building should continue, false if done or aborted (caller should call stopBuild).
     */
    public static boolean tick(ReactorBuilderBlockEntity builder) {
        if (builder.getLevel() == null || builder.getLevel().isClientSide()) return false;
        ServerLevel level = (ServerLevel) builder.getLevel();
        BlockState builderState = level.getBlockState(builder.getBlockPos());
        if (!(builderState.getBlock() instanceof ReactorBuilderBlock)) return false;
        var facing = builderState.getValue(ReactorBuilderBlock.FACING);
        var aabb = ReactorBuilderBlockEntity.getReactorVolumeAABB(
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
        int rw = RodPatternLogic.rodSpaceWidth(w, builder.getPatternMode());
        int rh = RodPatternLogic.rodSpaceHeight(h, builder.getPatternMode());
        int rd = RodPatternLogic.rodSpaceDepth(d, builder.getPatternMode());
        int insetXZ = RodPatternLogic.rodSpaceInsetXZ(builder.getPatternMode());
        int pattern = builder.getRodPattern();
        boolean expansionRodAtCenter = (pattern == RodPatternLogic.PATTERN_EXPANSION)
                ? RodPatternLogic.getExpansionRodAtCenterForPreview(rw, rd)
                : false;
        boolean openTop = builder.isOpenTop();

        if (hasRedZone(level, builder)) return false;

        // Try to place one block per tick in order: frame (skip top if openTop), rod controllers, rods, liquids, heat sink
        // Frame: all border positions except top face when openTop; reserve top-face rod controller positions (no frame there)
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (y == maxY && openTop) continue;
                    boolean onBorder = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                    if (!onBorder) continue;
                    if (y == maxY && isRodControllerPos(x, z, minX, minZ, maxY, insetXZ, rw, rd, pattern, expansionRodAtCenter)) continue; // reserve for rod controller
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!canReplace(level, pos)) continue;
                    // Cornice (edges/corners) and top/bottom faces: prefer casing. Lateral faces only: prefer glass.
                    boolean edgeOrCorner = isEdgeOrCorner(x, y, z, minX, minY, minZ, maxX, maxY, maxZ);
                    boolean topOrBottomFace = (y == minY || y == maxY);
                    boolean preferCasing = edgeOrCorner || topOrBottomFace;
                    ItemStack frame = preferCasing ? findCasingBlock(builder) : findGlassBlock(builder);
                    if (frame.isEmpty()) frame = preferCasing ? findGlassBlock(builder) : findCasingBlock(builder);
                    if (frame.isEmpty()) return true; // wait for materials
                    if (tryPlaceFrame(builder, level, pos, frame)) return true;
                }
            }
        }

        // Rod controllers on top face
        int rodControllerY = maxY;
        for (int rx = 0; rx < rw; rx++) {
            for (int rz = 0; rz < rd; rz++) {
                if (!RodPatternLogic.isRodColumnForPreview(rx, rz, rw, rd, pattern, expansionRodAtCenter)) continue;
                BlockPos pos = new BlockPos(minX + insetXZ + rx, rodControllerY, minZ + insetXZ + rz);
                if (!canReplace(level, pos)) continue;
                ItemStack rodCtrl = findRodController(builder);
                if (rodCtrl.isEmpty()) return true;
                if (tryPlaceRodController(builder, level, pos, rodCtrl)) return true;
            }
        }

        // Interior: rods
        for (int lx = insetXZ; lx < w - insetXZ; lx++) {
            for (int ly = 1; ly < h - 1; ly++) {
                for (int lz = insetXZ; lz < d - insetXZ; lz++) {
                    int rx = lx - insetXZ, ry = ly - 1, rz = lz - insetXZ;
                    if (!RodPatternLogic.isRodForPreview(rx, ry, rz, rw, rh, rd, pattern, expansionRodAtCenter)) continue;
                    BlockPos pos = new BlockPos(minX + lx, minY + ly, minZ + lz);
                    if (!canReplace(level, pos)) continue;
                    ItemStack rod = findRod(builder);
                    if (rod.isEmpty()) return true;
                    if (tryPlaceRod(builder, level, pos, rod)) return true;
                }
            }
        }

        // Interior: liquids and heat sinks use FULL interior (1..w-2, 1..h-2, 1..d-2). Rods use only rod space (insetXZ area).
        int patternMode = builder.getPatternMode();
        Fluid fluid = builder.getFluidTank().getFluid().getFluid();
        if (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY && builder.getFluidTank().getFluidAmount() >= MB_PER_LIQUID_BLOCK) {
            for (int lx = 1; lx < w - 1; lx++) {
                for (int ly = 1; ly < h - 1; ly++) {
                    for (int lz = 1; lz < d - 1; lz++) {
                        if (isInteriorCellRod(lx, ly, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) continue;
                        if (patternMode == RodPatternLogic.MODE_SUPER_ECONOMY) {
                            if (!isInRodSpace(lx, ly, lz, w, h, d, insetXZ) || !isRodSpaceCellAdjacentToRod(lx, ly, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) continue;
                        } else if (patternMode == RodPatternLogic.MODE_ECONOMY && !isInteriorCellAdjacentToRod(lx, ly, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) continue;
                        BlockPos pos = new BlockPos(minX + lx, minY + ly, minZ + lz);
                        if (!canReplace(level, pos)) continue;
                        BlockState liquidBlock = fluid.defaultFluidState().createLegacyBlock();
                        if (liquidBlock.isAir()) continue;
                        level.setBlock(pos, liquidBlock, 3);
                        builder.getFluidTank().drain(MB_PER_LIQUID_BLOCK, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                        builder.setChanged();
                        return true;
                    }
                }
            }
        }

        // Interior: heat sink blocks. Full interior; skip rod cells. Economy: only if adjacent to rod.
        for (int lx = 1; lx < w - 1; lx++) {
            for (int ly = 1; ly < h - 1; ly++) {
                for (int lz = 1; lz < d - 1; lz++) {
                    if (isInteriorCellRod(lx, ly, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) continue;
                    if (patternMode == RodPatternLogic.MODE_SUPER_ECONOMY) {
                        if (!isInRodSpace(lx, ly, lz, w, h, d, insetXZ) || !isRodSpaceCellAdjacentToRod(lx, ly, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) continue;
                    } else if (patternMode == RodPatternLogic.MODE_ECONOMY && !isInteriorCellAdjacentToRod(lx, ly, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) continue;
                    BlockPos pos = new BlockPos(minX + lx, minY + ly, minZ + lz);
                    if (!canReplace(level, pos)) continue;
                    if (builder.getSelectedHeatSinkIndex() == 0) continue; // Air: leave empty
                    ItemStack heatSink = findHeatSinkBlock(builder);
                    if (heatSink.isEmpty()) return true;
                    if (tryPlaceHeatSink(builder, level, pos, heatSink)) return true;
                }
            }
        }

        return false; // done
    }

    /** True if (lx, ly, lz) lies inside rod space (the -2 X/Z area). */
    private static boolean isInRodSpace(int lx, int ly, int lz, int w, int h, int d, int insetXZ) {
        return lx >= insetXZ && lx < w - insetXZ && ly >= 1 && ly < h - 1 && lz >= insetXZ && lz < d - insetXZ;
    }

    /** True if (lx, ly, lz) lies inside rod space and is a rod position. Used to skip rod cells when placing coolant in full interior. */
    private static boolean isInteriorCellRod(int lx, int ly, int lz, int w, int h, int d, int insetXZ, int rw, int rh, int rd, int pattern, boolean expansionRodAtCenter) {
        if (!isInRodSpace(lx, ly, lz, w, h, d, insetXZ)) return false;
        int rx = lx - insetXZ, ry = ly - 1, rz = lz - insetXZ;
        return RodPatternLogic.isRodForPreview(rx, ry, rz, rw, rh, rd, pattern, expansionRodAtCenter);
    }

    /** True if cell (lx, ly, lz) has at least one neighbor in rod space (6 directions) that is a rod. Used for Super Economy (coolant only in rod space, only adjacent to rods). */
    private static boolean isRodSpaceCellAdjacentToRod(int lx, int ly, int lz, int w, int h, int d, int insetXZ, int rw, int rh, int rd, int pattern, boolean expansionRodAtCenter) {
        for (int dx = -1; dx <= 1; dx += 2) {
            int nx = lx + dx;
            if (nx >= insetXZ && nx < w - insetXZ && isInteriorCellRod(nx, ly, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) return true;
        }
        for (int dy = -1; dy <= 1; dy += 2) {
            int ny = ly + dy;
            if (ny >= 1 && ny < h - 1 && isInteriorCellRod(lx, ny, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) return true;
        }
        for (int dz = -1; dz <= 1; dz += 2) {
            int nz = lz + dz;
            if (nz >= insetXZ && nz < d - insetXZ && isInteriorCellRod(lx, ly, nz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) return true;
        }
        return false;
    }

    /** True if interior cell (lx, ly, lz) has at least one neighbor (6 directions) that is a rod. Used for Economy mode (coolant only on sides of rods). Neighbors evaluated in full interior. */
    private static boolean isInteriorCellAdjacentToRod(int lx, int ly, int lz, int w, int h, int d, int insetXZ, int rw, int rh, int rd, int pattern, boolean expansionRodAtCenter) {
        for (int dx = -1; dx <= 1; dx += 2) {
            int nx = lx + dx;
            if (nx >= 1 && nx < w - 1 && isInteriorCellRod(nx, ly, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) return true;
        }
        for (int dy = -1; dy <= 1; dy += 2) {
            int ny = ly + dy;
            if (ny >= 1 && ny < h - 1 && isInteriorCellRod(lx, ny, lz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) return true;
        }
        for (int dz = -1; dz <= 1; dz += 2) {
            int nz = lz + dz;
            if (nz >= 1 && nz < d - 1 && isInteriorCellRod(lx, ly, nz, w, h, d, insetXZ, rw, rh, rd, pattern, expansionRodAtCenter)) return true;
        }
        return false;
    }

    private static boolean canReplace(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    /** True if position is on frame/cornice (edge or corner: at least 2 dimensions on boundary). */
    private static boolean isEdgeOrCorner(int x, int y, int z, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        int onBoundary = 0;
        if (x == minX || x == maxX) onBoundary++;
        if (y == minY || y == maxY) onBoundary++;
        if (z == minZ || z == maxZ) onBoundary++;
        return onBoundary >= 2;
    }

    private static ItemStack findCasingBlock(ReactorBuilderBlockEntity builder) {
        Block casing = ModBlocks.REACTOR_CASING.get();
        for (int i = 0; i < builder.getBufferHandler().getSlots(); i++) {
            ItemStack stack = builder.getBufferHandler().getStackInSlot(i);
            if (!stack.isEmpty() && Block.byItem(stack.getItem()) == casing) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static ItemStack findGlassBlock(ReactorBuilderBlockEntity builder) {
        Block glass = ModBlocks.REACTOR_GLASS.get();
        for (int i = 0; i < builder.getBufferHandler().getSlots(); i++) {
            ItemStack stack = builder.getBufferHandler().getStackInSlot(i);
            if (!stack.isEmpty() && Block.byItem(stack.getItem()) == glass) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryPlaceFrame(ReactorBuilderBlockEntity builder, ServerLevel level, BlockPos pos, ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR || !ReactorValidation.isShellBlock(block.defaultBlockState())) return false;
        level.setBlock(pos, block.defaultBlockState(), 3);
        consumeOne(builder, stack);
        return true;
    }

    private static ItemStack findRodController(ReactorBuilderBlockEntity builder) {
        for (int i = 0; i < builder.getBufferHandler().getSlots(); i++) {
            ItemStack stack = builder.getBufferHandler().getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == ModBlocks.ROD_CONTROLLER.get().asItem()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryPlaceRodController(ReactorBuilderBlockEntity builder, ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.getItem() != ModBlocks.ROD_CONTROLLER.get().asItem()) return false;
        level.setBlock(pos, ModBlocks.ROD_CONTROLLER.get().defaultBlockState(), 3);
        consumeOne(builder, stack);
        return true;
    }

    private static ItemStack findRod(ReactorBuilderBlockEntity builder) {
        for (int i = 0; i < builder.getBufferHandler().getSlots(); i++) {
            ItemStack stack = builder.getBufferHandler().getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() == ModBlocks.REACTOR_ROD.get().asItem()) return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryPlaceRod(ReactorBuilderBlockEntity builder, ServerLevel level, BlockPos pos, ItemStack stack) {
        if (stack.getItem() != ModBlocks.REACTOR_ROD.get().asItem()) return false;
        level.setBlock(pos, ModBlocks.REACTOR_ROD.get().defaultBlockState(), 3);
        consumeOne(builder, stack);
        return true;
    }

    private static ItemStack findHeatSinkBlock(ReactorBuilderBlockEntity builder) {
        int idx = builder.getSelectedHeatSinkIndex();
        if (idx <= 0) return ItemStack.EMPTY;
        if (builder.getLevel() == null) return ItemStack.EMPTY;
        var defs = HeatSinkLoader.getAllDefinitions();
        int defIdx = idx - 1;
        if (defIdx >= defs.size()) return ItemStack.EMPTY;
        for (int i = 0; i < builder.getBufferHandler().getSlots(); i++) {
            ItemStack stack = builder.getBufferHandler().getStackInSlot(i);
            if (stack.isEmpty()) continue;
            Block block = Block.byItem(stack.getItem());
            if (block != Blocks.AIR && HeatSinkLoader.isHeatSinkBlock(block.defaultBlockState(), builder.getLevel().registryAccess()))
                return stack;
        }
        return ItemStack.EMPTY;
    }

    private static boolean tryPlaceHeatSink(ReactorBuilderBlockEntity builder, ServerLevel level, BlockPos pos, ItemStack stack) {
        Block block = Block.byItem(stack.getItem());
        if (block == Blocks.AIR) return false;
        if (!HeatSinkLoader.isHeatSinkBlock(block.defaultBlockState(), level.registryAccess())) return false;
        level.setBlock(pos, block.defaultBlockState(), 3);
        consumeOne(builder, stack);
        return true;
    }

    private static void consumeOne(ReactorBuilderBlockEntity builder, ItemStack stack) {
        stack.shrink(1);
        builder.setChanged();
    }
}
