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
 * Turbine controller shell — same shape as {@link ReactorControllerBlock}, no block entity yet.
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

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SCREEN_NORTH;
            case EAST -> rotateShapeEast(SCREEN_NORTH);
            case WEST -> rotateShapeWest(SCREEN_NORTH);
            default -> SCREEN_NORTH;
        };
    }

    private static VoxelShape rotateShapeEast(VoxelShape north) {
        return Shapes.or(
                Block.box(8, 0, 0, 10, 4, 16),
                Block.box(8, 4, 0, 12, 8, 16),
                Block.box(8, 8, 0, 14, 12, 16),
                Block.box(8, 12, 0, 16, 16, 16));
    }

    private static VoxelShape rotateShapeWest(VoxelShape north) {
        return Shapes.or(
                Block.box(6, 0, 0, 8, 4, 16),
                Block.box(4, 4, 0, 8, 8, 16),
                Block.box(2, 8, 0, 8, 12, 16),
                Block.box(0, 12, 0, 8, 16, 16));
    }
}
