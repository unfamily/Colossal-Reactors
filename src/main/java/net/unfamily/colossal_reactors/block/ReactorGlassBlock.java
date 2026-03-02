package net.unfamily.colossal_reactors.block;

import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;

/**
 * Glass block with connected textures (Fusion). Transparent, no occlusion, like Connected Glass.
 */
public class ReactorGlassBlock extends TransparentBlock {

    public ReactorGlassBlock() {
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
