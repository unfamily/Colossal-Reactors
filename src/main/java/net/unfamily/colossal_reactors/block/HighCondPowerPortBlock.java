package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.blockentity.HighCondPowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;

public class HighCondPowerPortBlock extends BaseEntityBlock {

    public static final MapCodec<HighCondPowerPortBlock> CODEC = simpleCodec(HighCondPowerPortBlock::new);

    public HighCondPowerPortBlock(Properties properties) {
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
        return new HighCondPowerPortBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return type == ModBlockEntities.HIGH_COND_POWER_PORT_BE.get()
                ? (l, pos, st, be) -> ((HighCondPowerPortBlockEntity) be).tick()
                : null;
    }
}
