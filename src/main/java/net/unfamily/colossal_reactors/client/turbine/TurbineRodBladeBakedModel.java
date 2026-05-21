package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

/** Hides static rod/blade quads while the controller BER draws the spinning rotor. */
public class TurbineRodBladeBakedModel extends BakedModelWrapper<BakedModel> {

    /** Set during {@link #getModelData} for the same-thread chunk mesh build. */
    private static final ThreadLocal<BlockPos> CURRENT_POS = new ThreadLocal<>();

    public TurbineRodBladeBakedModel(BakedModel original) {
        super(original);
    }

    @Override
    public ModelData getModelData(
            net.minecraft.world.level.BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            ModelData renderData) {
        CURRENT_POS.set(pos);
        return renderData.derive().with(TurbineRotorModelData.RENDER_POS, pos).build();
    }

    @Override
    public List<BakedQuad> getQuads(
            @Nullable BlockState state,
            @Nullable Direction side,
            RandomSource rand,
            ModelData extraData,
            @Nullable RenderType renderType) {
        BlockPos pos = extraData.get(TurbineRotorModelData.RENDER_POS);
        if (pos == null) {
            pos = CURRENT_POS.get();
        }
        if (pos != null && TurbineRotorAnimationManager.shouldHideStatic(pos)) {
            return Collections.emptyList();
        }
        return originalModel.getQuads(state, side, rand, extraData, renderType);
    }
}
