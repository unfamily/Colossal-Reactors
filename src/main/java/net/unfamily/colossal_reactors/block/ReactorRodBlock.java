package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;

/**
 * Reactor rod block. Block state FILL (0-12) selects model by fill percentage: 0=empty, 1=5%, 2=10%, ..., 12=100%.
 */
public class ReactorRodBlock extends BaseEntityBlock {

    /** Fill level for model: 0=0%, 1=5%, 2=10%, 3=20%, 4=30%, 5=40%, 6=50%, 7=60%, 8=70%, 9=80%, 10=90%, 11=95%, 12=100%. */
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, 12);

    public static final MapCodec<ReactorRodBlock> CODEC = simpleCodec(ReactorRodBlock::new);

    public ReactorRodBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILL);
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
