package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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
