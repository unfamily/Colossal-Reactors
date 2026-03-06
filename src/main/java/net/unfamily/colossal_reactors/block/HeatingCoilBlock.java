package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.unfamily.colossal_reactors.blockentity.HeatingCoilBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Heating coil block: 6-direction placement (front faces player, like Hellfire Igniter).
 * Only the front face accepts items/fluids/energy. Off and On are separate block types per coil id.
 */
public class HeatingCoilBlock extends DirectionalBlock implements EntityBlock {

    public static final DirectionProperty FACING = DirectionalBlock.FACING;

    private final ResourceLocation coilId;
    private final boolean isOn;

    public HeatingCoilBlock(Properties properties, ResourceLocation coilId, boolean isOn) {
        super(properties);
        this.coilId = coilId;
        this.isOn = isOn;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public static MapCodec<HeatingCoilBlock> codec(ResourceLocation coilId, boolean isOn) {
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

    public ResourceLocation getCoilId() {
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

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof HeatingCoilBlockEntity coil) {
                coil.dropAllContents();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) return ItemInteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof HeatingCoilBlockEntity coil)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (!coil.acceptsFluidCapability()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        ItemStack singleStack = stack.copyWithCount(1);
        IFluidHandlerItem fluidHandler = singleStack.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler == null) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (coil.interactWithItemFluidHandler(fluidHandler, player)) {
            stack.shrink(1);
            if (stack.isEmpty()) {
                player.setItemInHand(hand, fluidHandler.getContainer());
            } else {
                player.setItemInHand(hand, stack);
                if (!player.getInventory().add(fluidHandler.getContainer())) {
                    player.drop(fluidHandler.getContainer(), false);
                }
            }
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
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
