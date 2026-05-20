package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Solid rod core collision (8×8 px cross-section, full block length along axis). */
public final class TurbineRodShapes {

    private static final VoxelShape AXIS_Y = Block.box(4, 0, 4, 12, 16, 12);
    private static final VoxelShape AXIS_Z = Block.box(4, 4, 0, 12, 12, 16);
    private static final VoxelShape AXIS_X = Block.box(0, 4, 4, 16, 12, 12);

    private TurbineRodShapes() {}

    public static VoxelShape forFacing(Direction facing) {
        return switch (facing.getAxis()) {
            case Y -> AXIS_Y;
            case Z -> AXIS_Z;
            case X -> AXIS_X;
        };
    }
}
