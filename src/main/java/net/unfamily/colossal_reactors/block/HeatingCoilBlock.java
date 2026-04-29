package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.colossal_reactors.blockentity.HeatingCoilBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

/**
 * Heating coil block: 6-direction placement (front faces player, like Hellfire Igniter).
 * Only the front face accepts items/fluids/energy. Off and On are separate block types per coil id.
 */
public class HeatingCoilBlock extends DirectionalBlock implements EntityBlock {

    public static final EnumProperty<Direction> FACING = DirectionalBlock.FACING;

    private final Identifier coilId;
    private final boolean isOn;

    public HeatingCoilBlock(Properties properties, Identifier coilId, boolean isOn) {
        super(properties);
        this.coilId = coilId;
        this.isOn = isOn;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static MapCodec<HeatingCoilBlock> codec(Identifier coilId, boolean isOn) {
        return simpleCodec(properties -> new HeatingCoilBlock(properties, coilId, isOn));
    }

    @Override
    protected MapCodec<? extends DirectionalBlock> codec() {
        return codec(coilId, isOn);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getNearestLookingDirection().getOpposite();
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            facing = context.getNearestLookingDirection();
        }
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    public Identifier getCoilId() {
        return coilId;
    }

    public boolean isOn() {
        return isOn;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HeatingCoilBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, ModBlockEntities.HEATING_COIL_BE.get(), HeatingCoilBlockEntity::tick);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private static <E extends BlockEntity, A extends BlockEntity> BlockEntityTicker<A> createTickerHelper(
            BlockEntityType<A> type, BlockEntityType<E> expected, BlockEntityTicker<? super E> ticker) {
        return expected == type ? (BlockEntityTicker<A>) ticker : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HeatingCoilBlockEntity coil && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(coil, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
