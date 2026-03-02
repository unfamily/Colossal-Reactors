package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Reactor controller block. Placeable in 4 horizontal directions, does not connect to other blocks.
 * Hitbox matches the non-full model: inclined screen (22.5° around X), back legs and top bar.
 */
public class ReactorControllerBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;

    /** Inclined screen for NORTH: 22.5° tilt (origin z=8), approximated as 4 horizontal slices. */
    private static final VoxelShape SCREEN_NORTH = Shapes.or(
            Block.box(0, 0, 8, 16, 4, 10),   // bottom slice
            Block.box(0, 4, 8, 16, 8, 12),
            Block.box(0, 8, 8, 16, 12, 14),
            Block.box(0, 12, 8, 16, 16, 16)  // top slice (tilt extends to z=16)
    );
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            SCREEN_NORTH,
            Block.box(2, 1, 10, 4, 3, 16),    // back_0
            Block.box(12, 1, 10, 14, 3, 16),  // back_1
            Block.box(1, 12, 15, 15, 14, 16)  // back_top
    );

    /** Inclined screen for SOUTH (mirror Z). */
    private static final VoxelShape SCREEN_SOUTH = Shapes.or(
            Block.box(0, 0, 6, 16, 4, 8),
            Block.box(0, 4, 4, 16, 8, 8),
            Block.box(0, 8, 2, 16, 12, 8),
            Block.box(0, 12, 0, 16, 16, 8)
    );
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            SCREEN_SOUTH,
            Block.box(12, 1, 0, 14, 3, 6),
            Block.box(2, 1, 0, 4, 3, 6),
            Block.box(1, 12, 0, 15, 14, 1)
    );

    /** Inclined screen for EAST (front = +X). */
    private static final VoxelShape SCREEN_EAST = Shapes.or(
            Block.box(6, 0, 0, 8, 4, 16),
            Block.box(4, 4, 0, 8, 8, 16),
            Block.box(2, 8, 0, 8, 12, 16),
            Block.box(0, 12, 0, 8, 16, 16)
    );
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            SCREEN_EAST,
            Block.box(0, 1, 2, 6, 3, 4),
            Block.box(0, 1, 12, 6, 3, 14),
            Block.box(0, 12, 1, 1, 14, 15)
    );

    /** Inclined screen for WEST (front = -X). */
    private static final VoxelShape SCREEN_WEST = Shapes.or(
            Block.box(8, 0, 0, 10, 4, 16),
            Block.box(8, 4, 0, 12, 8, 16),
            Block.box(8, 8, 0, 14, 12, 16),
            Block.box(8, 12, 0, 16, 16, 16)
    );
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            SCREEN_WEST,
            Block.box(10, 1, 12, 16, 3, 14),
            Block.box(10, 1, 2, 16, 3, 4),
            Block.box(15, 12, 1, 16, 14, 15)
    );

    public ReactorControllerBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return simpleCodec(ReactorControllerBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    private static VoxelShape shapeFor(Direction facing) {
        return switch (facing) {
            case SOUTH -> SHAPE_SOUTH;
            case EAST -> SHAPE_EAST;
            case WEST -> SHAPE_WEST;
            default -> SHAPE_NORTH;
        };
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }
}
