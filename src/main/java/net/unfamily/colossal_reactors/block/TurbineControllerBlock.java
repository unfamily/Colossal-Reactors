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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Turbine controller shell — collision matches Blockbench model (same layout as {@link ReactorControllerBlock}).
 */
public class TurbineControllerBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<TurbineControllerBlock> CODEC = simpleCodec(TurbineControllerBlock::new);
    public static final EnumProperty<TurbineVisualState> VISUAL =
            EnumProperty.create("visual", TurbineVisualState.class);

    private static final VoxelShape SCREEN_NORTH = Shapes.or(
            Block.box(0, 0, 8, 16, 4, 10),
            Block.box(0, 4, 8, 16, 8, 12),
            Block.box(0, 8, 8, 16, 12, 14),
            Block.box(0, 12, 8, 16, 16, 16));
    private static final VoxelShape SHAPE_NORTH = Shapes.or(
            SCREEN_NORTH,
            Block.box(2, 1, 10, 4, 3, 16),
            Block.box(12, 1, 10, 14, 3, 16),
            Block.box(1, 12, 15, 15, 14, 16));

    private static final VoxelShape SCREEN_SOUTH = Shapes.or(
            Block.box(0, 0, 6, 16, 4, 8),
            Block.box(0, 4, 4, 16, 8, 8),
            Block.box(0, 8, 2, 16, 12, 8),
            Block.box(0, 12, 0, 16, 16, 8));
    private static final VoxelShape SHAPE_SOUTH = Shapes.or(
            SCREEN_SOUTH,
            Block.box(12, 1, 0, 14, 3, 6),
            Block.box(2, 1, 0, 4, 3, 6),
            Block.box(1, 12, 0, 15, 14, 1));

    private static final VoxelShape SCREEN_EAST = Shapes.or(
            Block.box(6, 0, 0, 8, 4, 16),
            Block.box(4, 4, 0, 8, 8, 16),
            Block.box(2, 8, 0, 8, 12, 16),
            Block.box(0, 12, 0, 8, 16, 16));
    private static final VoxelShape SHAPE_EAST = Shapes.or(
            SCREEN_EAST,
            Block.box(0, 1, 2, 6, 3, 4),
            Block.box(0, 1, 12, 6, 3, 14),
            Block.box(0, 12, 1, 1, 14, 15));

    private static final VoxelShape SCREEN_WEST = Shapes.or(
            Block.box(8, 0, 0, 10, 4, 16),
            Block.box(8, 4, 0, 12, 8, 16),
            Block.box(8, 8, 0, 14, 12, 16),
            Block.box(8, 12, 0, 16, 16, 16));
    private static final VoxelShape SHAPE_WEST = Shapes.or(
            SCREEN_WEST,
            Block.box(10, 1, 12, 16, 3, 14),
            Block.box(10, 1, 2, 16, 3, 4),
            Block.box(15, 12, 1, 16, 14, 15));

    public TurbineControllerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(VISUAL, TurbineVisualState.OFF));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, VISUAL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapeFor(state.getValue(FACING));
    }
}
