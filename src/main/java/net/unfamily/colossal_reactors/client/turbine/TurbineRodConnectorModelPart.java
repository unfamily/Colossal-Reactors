package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Wraps a {@link BlockStateModelPart} and drops connector quads on sides without blades. */
public final class TurbineRodConnectorModelPart implements BlockStateModelPart {

    private final BlockStateModelPart delegate;
    private final Direction rodAxis;
    private final int visibleMask;

    public TurbineRodConnectorModelPart(BlockStateModelPart delegate, Direction rodAxis, int visibleMask) {
        this.delegate = delegate;
        this.rodAxis = rodAxis;
        this.visibleMask = visibleMask;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable Direction direction) {
        return TurbineRodConnectorQuadFilter.filter(delegate.getQuads(direction), rodAxis, visibleMask);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
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

    public static List<BlockStateModelPart> wrapParts(
            List<BlockStateModelPart> parts, Direction rodAxis, int visibleMask) {
        List<BlockStateModelPart> out = new ArrayList<>(parts.size());
        for (BlockStateModelPart part : parts) {
            List<BakedQuad> all = part.getQuads(null);
            List<BakedQuad> filtered = TurbineRodConnectorQuadFilter.filter(all, rodAxis, visibleMask);
            if (!filtered.isEmpty()) {
                out.add(new TurbineRodConnectorModelPart(part, rodAxis, visibleMask));
            }
        }
        return out;
    }
}
