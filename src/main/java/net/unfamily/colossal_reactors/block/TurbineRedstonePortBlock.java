package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;
import net.unfamily.colossal_reactors.blockentity.TurbineRedstonePortBlockEntity;

public class TurbineRedstonePortBlock extends RedstonePortBlock {

    public static final MapCodec<TurbineRedstonePortBlock> CODEC = simpleCodec(TurbineRedstonePortBlock::new);

    public TurbineRedstonePortBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends RedstonePortBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TurbineRedstonePortBlockEntity(pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block,
                                Orientation orientation, boolean isMoving) {
        super.neighborChanged(state, level, pos, block, orientation, isMoving);
        TurbineControllerBlock.notifyTurbineRedstoneChanged(level, pos);
    }
}
