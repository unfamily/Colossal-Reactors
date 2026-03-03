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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
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
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ResourcePortBlockEntity resourcePort)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        IFluidHandler blockHandler = resourcePort.getFluidHandler();
        IFluidHandler itemHandler = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (itemHandler == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        // Try fill block from item (e.g. water bucket -> tank)
        FluidStack inItem = itemHandler.getFluidInTank(0);
        if (!inItem.isEmpty()) {
            int filled = blockHandler.fill(inItem, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                return ItemInteractionResult.SUCCESS;
            }
        }
        // Try drain block to item (e.g. tank -> empty bucket)
        FluidStack inBlock = blockHandler.getFluidInTank(0);
        if (!inBlock.isEmpty()) {
            int drained = itemHandler.fill(inBlock, IFluidHandler.FluidAction.EXECUTE);
            if (drained > 0) {
                blockHandler.drain(drained, IFluidHandler.FluidAction.EXECUTE);
                return ItemInteractionResult.SUCCESS;
            }
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
