package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;

/** Turbine controller: validate multiblock on click, open GUI when valid. */
public class TurbineControllerBlock extends BaseEntityBlock {

    public static final net.minecraft.world.level.block.state.properties.DirectionProperty FACING =
            HorizontalDirectionalBlock.FACING;
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
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineControllerBlockEntity(pos, state);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof TurbineControllerBlockEntity controllerBe)) {
            return;
        }
        Direction into = state.getValue(FACING).getOpposite();
        BlockPos startPos = pos.relative(into);
        TurbineVisualState current = state.getValue(VISUAL);

        if (current == TurbineVisualState.VALIDATING) {
            TurbineValidation.Result result = TurbineValidation.validateWithRodAlignment(level, startPos, into, -1);
            controllerBe.setCachedResult(result);
            TurbineVisualState next = result.valid() ? TurbineVisualState.ON : TurbineVisualState.OFF;
            level.setBlock(pos, state.setValue(VISUAL, next), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
            controllerBe.setChanged();
            controllerBe.notifyValidationResult();
            if (next == TurbineVisualState.ON) {
                controllerBe.tickSimulation(level);
                level.scheduleTick(pos, this, 1);
            }
            return;
        }

        if (current == TurbineVisualState.ON) {
            TurbineValidation.Result result = controllerBe.getCachedResult();
            boolean revalidate = (level.getGameTime() % Config.TURBINE_VALIDATION_INTERVAL_TICKS.get()) == 0;
            if (revalidate) {
                result = TurbineValidation.validateWithRodAlignment(level, startPos, into, -1);
                if (!result.valid()) {
                    controllerBe.setCachedResult(result);
                    level.setBlock(pos, state.setValue(VISUAL, TurbineVisualState.OFF), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
                    controllerBe.setChanged();
                    return;
                }
                controllerBe.setCachedResult(result);
                controllerBe.setChanged();
            }
            if (result != null && result.valid()) {
                controllerBe.tickSimulation(level);
            }
            level.scheduleTick(pos, this, 1);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof TurbineControllerBlockEntity controllerBe)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(VISUAL) == TurbineVisualState.ON) {
            if (controllerBe.getCachedResult().valid()) {
                controllerBe.tickSimulation((ServerLevel) level);
                if (player instanceof ServerPlayer sp) {
                    sp.openMenu(controllerBe, pos);
                }
            }
            return InteractionResult.CONSUME;
        }
        controllerBe.setLastInteractingPlayer(player);
        level.setBlock(pos, state.setValue(VISUAL, TurbineVisualState.VALIDATING), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(pos, this, 1);
        }
        return InteractionResult.SUCCESS;
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
