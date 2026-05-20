package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineBladeBlock;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;

/**
 * Corrects manually placed turbine rods that share the controller axis but face the opposite direction.
 */
public final class TurbineRodAlignment {

    private TurbineRodAlignment() {}

    /**
     * Flips rods (and in-place blade facings) on {@code controllerAxis.getOpposite()} to {@code controllerAxis}.
     * Only runs on server levels.
     */
    public static void correctOppositeRods(
            ServerLevel level,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ,
            TurbineRotorLayout layout) {
        if (layout == null) {
            return;
        }
        Direction controllerAxis = layout.growthAxis();
        Direction opposite = controllerAxis.getOpposite();
        for (int x = minX + 1; x < maxX; x++) {
            for (int z = minZ + 1; z < maxZ; z++) {
                for (int y = minY + 1; y < maxY; y++) {
                    if (layout.interiorIndexFromWorld(x, y, z) >= layout.coilStartInterior()) {
                        continue;
                    }
                    BlockPos rodPos = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(rodPos);
                    if (!state.is(ModBlocks.TURBINE_ROD.get()) || !state.hasProperty(TurbineRodBlock.FACING)) {
                        continue;
                    }
                    Direction rodFacing = state.getValue(TurbineRodBlock.FACING);
                    if (rodFacing.getAxis() != controllerAxis.getAxis() || rodFacing != opposite) {
                        continue;
                    }
                    BlockState newRod = state.setValue(TurbineRodBlock.FACING, controllerAxis);
                    level.setBlock(rodPos, newRod, net.minecraft.world.level.block.Block.UPDATE_ALL);
                    refreshBladeFacings(level, rodPos, controllerAxis, opposite);
                }
            }
        }
    }

    private static void refreshBladeFacings(ServerLevel level, BlockPos rodPos, Direction newAxis, Direction oldAxis) {
        for (BlockPos bladePos : TurbineBladePlacement.collectBladePositions(level, rodPos, oldAxis)) {
            BlockState bladeState = level.getBlockState(bladePos);
            if (!bladeState.is(ModBlocks.TURBINE_BLADE.get()) || !bladeState.hasProperty(TurbineBladeBlock.FACING)) {
                continue;
            }
            Direction lateral = bladeState.getValue(TurbineBladeBlock.FACING);
            if (lateral.getAxis() == newAxis.getAxis()) {
                continue;
            }
            level.setBlock(bladePos, bladeState.setValue(TurbineBladeBlock.FACING, lateral), net.minecraft.world.level.block.Block.UPDATE_ALL);
        }
    }
}
