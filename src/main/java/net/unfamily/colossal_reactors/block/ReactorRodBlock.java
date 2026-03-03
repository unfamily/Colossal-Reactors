package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;

/**
 * Reactor rod block. When full (fuelUnits >= max) uses full model with inner cube; otherwise empty frame model.
 */
public class ReactorRodBlock extends BaseEntityBlock {

    public static final BooleanProperty FULL = BooleanProperty.create("full");

    public static final MapCodec<ReactorRodBlock> CODEC = simpleCodec(ReactorRodBlock::new);

    public ReactorRodBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FULL, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FULL);
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
        return new ReactorRodBlockEntity(pos, state);
    }
}
