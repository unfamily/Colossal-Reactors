package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;
import net.unfamily.colossal_reactors.turbine.TurbineRodConnectorVisibility;

import java.util.Objects;

/** ModelData for turbine rod/blade baked model wrappers (block render position). */
public final class TurbineRotorModelData {

    public static final ModelProperty<BlockPos> RENDER_POS = new ModelProperty<>(Objects::nonNull);
    /** Lateral connector visibility mask for turbine rods ({@link TurbineRodConnectorVisibility}). */
    public static final ModelProperty<Integer> CONNECTOR_MASK = new ModelProperty<>();

    private TurbineRotorModelData() {}

    public static ModelData withPos(BlockPos pos) {
        return ModelData.builder().with(RENDER_POS, pos).build();
    }

    public static ModelData forRod(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        ModelData.Builder builder = ModelData.builder().with(RENDER_POS, pos);
        if (state.is(ModBlocks.TURBINE_ROD.get()) && state.hasProperty(TurbineRodBlock.FACING)) {
            Direction axis = state.getValue(TurbineRodBlock.FACING);
            builder.with(CONNECTOR_MASK, TurbineRodConnectorVisibility.lateralConnectorMask(level, pos, axis));
        }
        return builder.build();
    }
}
