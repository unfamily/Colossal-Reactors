package net.unfamily.colossal_reactors.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Turbine rod shell (custom model), no functionality yet. */
public class TurbineRodBlock extends Block {

    public static final MapCodec<TurbineRodBlock> CODEC = simpleCodec(TurbineRodBlock::new);

    public TurbineRodBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }
}
