package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;

import java.util.Objects;

/** ModelData for turbine rod/blade baked model wrappers (block render position). */
public final class TurbineRotorModelData {

    public static final ModelProperty<BlockPos> RENDER_POS = new ModelProperty<>(Objects::nonNull);

    private TurbineRotorModelData() {}

    public static ModelData withPos(BlockPos pos) {
        return ModelData.builder().with(RENDER_POS, pos).build();
    }
}
