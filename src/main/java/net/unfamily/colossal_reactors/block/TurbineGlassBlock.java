package net.unfamily.colossal_reactors.block;

import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/** Turbine glass with Fusion connecting textures. */
public class TurbineGlassBlock extends TransparentBlock {

    public TurbineGlassBlock() {
        super(BlockBehaviour.Properties.of()
                .sound(net.minecraft.world.level.block.SoundType.GLASS)
                .instrument(NoteBlockInstrument.HAT)
                .strength(0.3f)
                .noOcclusion()
                .isValidSpawn((s, l, p, e) -> false)
                .isRedstoneConductor((s, l, p) -> false)
                .isSuffocating((s, l, p) -> false)
                .isViewBlocking((s, l, p) -> false));
    }
}
