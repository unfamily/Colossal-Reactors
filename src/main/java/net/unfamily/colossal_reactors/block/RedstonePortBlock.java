package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.unfamily.colossal_reactors.blockentity.RedstonePortBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Redstone input face for reactor/turbine multiblocks. Connects to dust/repeaters ({@link #canConnectRedstone})
 * but does not conduct power through the block volume ({@code isRedstoneConductor=false} on block properties).
 * No item/fluid/energy NeoForge capabilities (same as iskandert machines that only react to redstone).
 */
public class RedstonePortBlock extends BaseEntityBlock {

    public static final MapCodec<RedstonePortBlock> CODEC = simpleCodec(RedstonePortBlock::new);

    public RedstonePortBlock(Properties properties) {
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
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction direction) {
        return direction != null;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, net.minecraft.world.level.block.Block block,
                                @Nullable Orientation orientation, boolean isMoving) {
        if (!level.isClientSide()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof RedstonePortBlockEntity port) {
                port.onRedstoneNeighborChanged();
            }
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstonePortBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof RedstonePortBlockEntity redstonePort && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(redstonePort, pos);
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }
}
