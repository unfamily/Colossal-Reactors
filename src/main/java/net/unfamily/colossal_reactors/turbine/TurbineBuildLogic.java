package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineBladeBlock;
import net.unfamily.colossal_reactors.block.TurbineBuilderBlock;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;
import net.unfamily.colossal_reactors.item.ModItems;

/**
 * Server-side turbine build: frame, rod controllers, rods, blades, coil blocks.
 */
public final class TurbineBuildLogic {

    private static final int STAGE_FRAME = 0;
    private static final int STAGE_ROD_CONTROLLERS = 1;
    private static final int STAGE_RODS = 2;
    private static final int STAGE_BLADES = 3;
    private static final int STAGE_COILS = 4;
    private static final int STAGE_DONE = 5;

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
                        if (!st.isAir() && !TurbineValidation.isShellBlock(st) && !st.is(ModBlocks.TURBINE_ROD_CONTROLLER.get())) {
                            return true;
                        }
                    } else {
                        int iy = y - b.minY - 1;
                        if (iy >= coilStart) {
                            if (!st.isAir() && !ElecCoilLoader.isCoilBlock(st, level.registryAccess())) return true;
                        } else if (!st.isAir() && !st.is(ModBlocks.TURBINE_ROD.get())
                                && !st.is(ModBlocks.TURBINE_BLADE.get())) {
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
            if (!tickOnce(builder)) return false;
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
        return switch (builder.getBuildStage()) {
            case STAGE_FRAME -> tickFrame(level, builder, b);
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

    private static boolean tickFrame(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        for (int x = b.minX; x <= b.maxX; x++) {
            for (int y = b.minY; y <= b.maxY; y++) {
                for (int z = b.minZ; z <= b.maxZ; z++) {
                    boolean border = x == b.minX || x == b.maxX || y == b.minY || y == b.maxY || z == b.minZ || z == b.maxZ;
                    if (!border) continue;
                    BlockPos pos = new BlockPos(x, y, z);
                    if (y == b.maxY && isRodControllerColumn(x, z, b)) continue;
                    if (tryPlace(level, builder, pos, ModBlocks.TURBINE_CASING.get())) {
                        return true;
                    }
                }
            }
        }
        builder.setBuildStage(STAGE_ROD_CONTROLLERS);
        return true;
    }

    private static boolean tickRodControllers(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        for (int x = b.minX + 1; x < b.maxX; x++) {
            for (int z = b.minZ + 1; z < b.maxZ; z++) {
                if (!isRodControllerColumn(x, z, b)) continue;
                BlockPos pos = new BlockPos(x, b.maxY, z);
                if (tryPlace(level, builder, pos, ModBlocks.TURBINE_ROD_CONTROLLER.get())) {
                    return true;
                }
            }
        }
        builder.setBuildStage(STAGE_RODS);
        return true;
    }

    private static boolean tickRods(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        int coilStart = TurbineRodSpaceLayout.coilZoneStartY(b.interiorH, builder.getCoilLayerCount());
        for (int x = b.minX + 1; x < b.maxX; x++) {
            for (int z = b.minZ + 1; z < b.maxZ; z++) {
                int rx = x - b.minX - 1;
                int rz = z - b.minZ - 1;
                if (!TurbineRodPatternLogic.isRodColumn(rx, rz, b.rw, b.rd, builder.getRodPattern())) continue;
                for (int y = b.minY + 1; y < b.maxY; y++) {
                    int iy = y - b.minY - 1;
                    if (iy >= coilStart) continue;
                    BlockPos pos = new BlockPos(x, y, z);
                    BlockState existing = level.getBlockState(pos);
                    if (existing.is(ModBlocks.TURBINE_ROD.get())) continue;
                    BlockState rod = ModBlocks.TURBINE_ROD.get().defaultBlockState()
                            .setValue(TurbineRodBlock.FACING, Direction.UP);
                    if (tryPlaceState(level, builder, pos, rod)) return true;
                }
            }
        }
        builder.setBuildStage(STAGE_BLADES);
        return true;
    }

    private static boolean tickBlades(ServerLevel level, TurbineBuilderBlockEntity builder, BuildBounds b) {
        int coilStart = TurbineRodSpaceLayout.coilZoneStartY(b.interiorH, builder.getCoilLayerCount());
        for (int x = b.minX + 1; x < b.maxX; x++) {
            for (int z = b.minZ + 1; z < b.maxZ; z++) {
                int rx = x - b.minX - 1;
                int rz = z - b.minZ - 1;
                if (!TurbineRodPatternLogic.isRodColumn(rx, rz, b.rw, b.rd, builder.getRodPattern())) continue;
                for (int y = b.minY + 1; y < b.maxY; y++) {
                    int iy = y - b.minY - 1;
                    if (iy >= coilStart) continue;
                    BlockPos rodPos = new BlockPos(x, y, z);
                    BlockState rodState = level.getBlockState(rodPos);
                    if (!rodState.is(ModBlocks.TURBINE_ROD.get())) continue;
                    Direction axis = rodState.getValue(TurbineRodBlock.FACING);
                    int targetRing = TurbineRodPatternLogic.targetBladeRingForLayer(iy, coilStart, builder.getRodPattern());
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
            if (!consumeItem(level, builder, ModItems.TURBINE_BLADE.get())) return false;
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
                    if (ElecCoilLoader.isBlockMatchingSelectedCoil(st, idx, level.registryAccess())) continue;
                    Block block = coilBlockFor(idx, level);
                    if (block != null && tryPlace(level, builder, pos, block)) return true;
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
        var id = net.minecraft.resources.ResourceLocation.tryParse(sel);
        if (id == null) return null;
        return level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.BLOCK).get(id);
    }

    private static boolean tryPlace(ServerLevel level, TurbineBuilderBlockEntity builder, BlockPos pos, Block block) {
        return tryPlaceState(level, builder, pos, block.defaultBlockState());
    }

    private static boolean tryPlaceState(ServerLevel level, TurbineBuilderBlockEntity builder, BlockPos pos, BlockState state) {
        BlockState existing = level.getBlockState(pos);
        if (!existing.canBeReplaced() && !existing.isAir()) return false;
        if (!consumeForBlock(builder, state.getBlock())) return false;
        return level.setBlock(pos, state, Block.UPDATE_ALL);
    }

    private static boolean consumeForBlock(TurbineBuilderBlockEntity builder, Block block) {
        if (block == ModBlocks.TURBINE_BLADE.get()) {
            return consumeItem((ServerLevel) builder.getLevel(), builder, ModItems.TURBINE_BLADE.get());
        }
        ItemStack stack = new ItemStack(block);
        return consumeItem((ServerLevel) builder.getLevel(), builder, stack.getItem());
    }

    private static boolean consumeItem(ServerLevel level, TurbineBuilderBlockEntity builder, net.minecraft.world.item.Item item) {
        var handler = builder.getBufferHandler();
        for (int i = 0; i < handler.getSlots(); i++) {
            ItemStack s = handler.getStackInSlot(i);
            if (s.is(item)) {
                s.shrink(1);
                handler.setStackInSlot(i, s);
                return true;
            }
        }
        return false;
    }

    private static boolean isRodControllerColumn(int x, int z, BuildBounds b) {
        int rx = x - b.minX - 1;
        int rz = z - b.minZ - 1;
        return rx >= 0 && rx < b.rw && rz >= 0 && rz < b.rd;
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
        return new BuildBounds(minX, minY, minZ, maxX, maxY, maxZ, w, h, d,
                TurbineRodPatternLogic.rodSpaceWidth(w),
                TurbineRodPatternLogic.rodSpaceHeight(h, builder.getCoilLayerCount()),
                TurbineRodPatternLogic.rodSpaceDepth(d),
                TurbineRodSpaceLayout.interiorHeight(h));
    }

    private record BuildBounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
                               int w, int h, int d, int rw, int rh, int rd, int interiorH) {}
}
