package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;
import net.unfamily.colossal_reactors.turbine.TurbineRodConnectorVisibility;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Hides rod connector geometry on lateral sides that have no turbine blades. */
public final class TurbineRodConnectorBlockStateModel implements BlockStateModel {

    private final BlockStateModel delegate;

    public TurbineRodConnectorBlockStateModel(BlockStateModel delegate) {
        this.delegate = delegate;
    }

    @Override
    public void collectParts(
            BlockAndTintGetter level,
            BlockPos pos,
            BlockState state,
            RandomSource random,
            List<BlockStateModelPart> output) {
        if (!state.is(ModBlocks.TURBINE_ROD.get()) || !state.hasProperty(TurbineRodBlock.FACING)) {
            delegate.collectParts(level, pos, state, random, output);
            return;
        }
        List<BlockStateModelPart> parts = new ArrayList<>();
        delegate.collectParts(level, pos, state, random, parts);
        Direction axis = state.getValue(TurbineRodBlock.FACING);
        int mask = resolveMask(level, pos, axis);
        output.addAll(TurbineRodConnectorModelPart.wrapParts(parts, axis, mask));
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
        if (!state.is(ModBlocks.TURBINE_ROD.get()) || !state.hasProperty(TurbineRodBlock.FACING)) {
            return delegate.createGeometryKey(level, pos, state, random);
        }
        return new ConnectorKey(
                delegate.createGeometryKey(level, pos, state, random),
                resolveMask(level, pos, state.getValue(TurbineRodBlock.FACING)));
    }

    private static int resolveMask(BlockAndTintGetter level, BlockPos pos, Direction axis) {
        Integer fromScope = TurbineRodRenderScope.connectorMaskOrNull();
        if (fromScope != null) {
            return fromScope;
        }
        if (level instanceof net.minecraft.world.level.Level world) {
            return TurbineRodConnectorVisibility.lateralConnectorMask(world, pos, axis);
        }
        var clientLevel = net.minecraft.client.Minecraft.getInstance().level;
        if (clientLevel != null) {
            return TurbineRodConnectorVisibility.lateralConnectorMask(clientLevel, pos, axis);
        }
        return 0;
    }

    private record ConnectorKey(@Nullable Object delegateKey, int mask) {}
}
