package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

import java.util.List;

/** Hides static turbine rod/blade models while the controller BER draws the spinning rotor. */
public final class TurbineRodBladeHidingBlockStateModel implements BlockStateModel {

    private final BlockStateModel delegate;

    public TurbineRodBladeHidingBlockStateModel(BlockStateModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        if (TurbineRotorAnimationManager.shouldHideStatic(pos)) {
            return;
        }
        delegate.collectParts(level, pos, state, random, output);
    }

    @Override
    public void collectParts(RandomSource random, List<BlockStateModelPart> output) {
        delegate.collectParts(random, output);
    }

    @Override
    public Material.Baked particleMaterial() {
        return delegate.particleMaterial();
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags() {
        return delegate.materialFlags();
    }

    @Override
    public Material.Baked particleMaterial(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return delegate.particleMaterial(level, pos, state);
    }

    @Override
    @BakedQuad.MaterialFlags
    public int materialFlags(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        return delegate.materialFlags(level, pos, state);
    }

    @Override
    public @Nullable Object createGeometryKey(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random) {
        if (TurbineRotorAnimationManager.shouldHideStatic(pos)) {
            return null;
        }
        return delegate.createGeometryKey(level, pos, state, random);
    }
}
