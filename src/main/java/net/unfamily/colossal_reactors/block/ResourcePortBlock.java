package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;

public class ResourcePortBlock extends BaseEntityBlock {

    public static final MapCodec<ResourcePortBlock> CODEC = simpleCodec(ResourcePortBlock::new);

    public ResourcePortBlock(Properties properties) {
        super(properties);
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
        return new ResourcePortBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof ResourcePortBlockEntity resourcePort) {
                resourcePort.dropAllContents();
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ResourcePortBlockEntity resourcePort)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // Work on a copy with count 1 so the capability modifies that copy; then we update player hand explicitly (avoids duplication / missing bucket fill).
        ItemStack singleStack = stack.copyWithCount(1);
        IFluidHandlerItem fluidHandler = singleStack.getCapability(Capabilities.FluidHandler.ITEM);
        if (fluidHandler == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (resourcePort.interactWithItemFluidHandler(fluidHandler, player)) {
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
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof ResourcePortBlockEntity resourcePort) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.openMenu(resourcePort, pos);
            }
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
