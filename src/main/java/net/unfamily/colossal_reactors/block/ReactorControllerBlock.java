package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.RedstonePortBlockEntity;
import net.unfamily.colossal_reactors.reactor.ReactorFiller;
import net.unfamily.colossal_reactors.reactor.ReactorSimulation;
import net.unfamily.colossal_reactors.reactor.ReactorValidation;

/**
 * Reactor controller block. Placeable in 4 horizontal directions, does not connect to other blocks.
 * Validates the reactor multiblock behind it (opposite to facing) and re-validates periodically.
 */
public class ReactorControllerBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<ControllerState> STATE = EnumProperty.create("state", ControllerState.class);

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
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(STATE, ControllerState.OFF));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorControllerBlockEntity(pos, state);
    }

    /**
     * No BE ticker: validation is driven by block tick only (onPlace + tick), same pattern as iskautils SacredRubberSaplingBlock.
     */
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return null;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(ReactorControllerBlock::new);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, STATE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // FACING = opposite of player look so the screen faces the player; back = getOpposite(FACING) = into reactor
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    /**
     * On place: leave OFF. No validation; player must click to start one.
     */
    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
    }

    /**
     * Block tick only when scheduled: (1) VALIDATING → run one validation → ON or OFF, schedule next only if ON;
     * (2) ON → periodic re-validation, stay ON or go OFF; never show boot during periodic (stays visually ON).
     */
    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ReactorControllerBlockEntity controllerBe)) {
            return;
        }
        Direction back = state.getValue(FACING).getOpposite();
        BlockPos startPos = pos.relative(back);
        ControllerState current = state.getValue(STATE);

        if (current == ControllerState.VALIDATING) {
            ReactorValidation.Result result = ReactorValidation.validate(level, startPos, back);
            controllerBe.setCachedResult(result);
            ControllerState next = result.valid() ? ControllerState.ON : ControllerState.OFF;
            level.setBlock(pos, state.setValue(STATE, next), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
            controllerBe.setChanged();
            controllerBe.notifyValidationResult();
            if (next == ControllerState.ON) {
                level.scheduleTick(pos, this, 1);
            }
            return;
        }

        if (current == ControllerState.ON) {
            ReactorValidation.Result result = controllerBe.getCachedResult();
            boolean revalidate = (level.getGameTime() % Config.REACTOR_VALIDATION_INTERVAL_TICKS.get()) == 0;
            if (revalidate) {
                result = ReactorValidation.validate(level, startPos, back);
                if (!result.valid()) {
                    controllerBe.setCachedResult(result);
                    level.setBlock(pos, state.setValue(STATE, ControllerState.OFF), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
                    controllerBe.setChanged();
                    return;
                }
                controllerBe.setCachedResult(result);
                controllerBe.setChanged();
            }
            if (result != null && result.valid()) {
                if (isRedstoneGateSatisfied(level, result)) {
                    ReactorFiller.tickFill(level, controllerBe);
                    ReactorSimulation.tick(level, controllerBe);
                }
            }
            level.scheduleTick(pos, this, 1);
        }
    }

    /**
     * On click: if ON open GUI; if OFF start one validation (boot then ON/OFF).
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof ReactorControllerBlockEntity be)) {
            return InteractionResult.PASS;
        }
        if (state.getValue(STATE) == ControllerState.ON) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(be, pos);
            }
            return InteractionResult.CONSUME;
        }
        be.setLastInteractingPlayer(player);
        level.setBlock(pos, state.setValue(STATE, ControllerState.VALIDATING), Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS);
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.scheduleTick(pos, this, 1);
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * If the reactor has at least one redstone port, the reactor runs only when at least one port is active (signal + mode).
     * If there are no redstone ports, the gate is satisfied (reactor always runs when ON).
     */
    public static boolean isRedstoneGateSatisfied(ServerLevel level, ReactorValidation.Result result) {
        int minX = result.minX();
        int minY = result.minY();
        int minZ = result.minZ();
        int maxX = result.maxX();
        int maxY = result.maxY();
        int maxZ = result.maxZ();
        boolean hasAnyPort = false;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (!level.getBlockState(new BlockPos(x, y, z)).is(ModBlocks.REDSTONE_PORT.get())) continue;
                    hasAnyPort = true;
                    BlockEntity be = level.getBlockEntity(new BlockPos(x, y, z));
                    if (be instanceof RedstonePortBlockEntity port && port.isRedstoneActive(level)) {
                        return true;
                    }
                }
            }
        }
        return !hasAnyPort;
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
