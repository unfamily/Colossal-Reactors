package net.unfamily.colossal_reactors.block;

import net.minecraft.world.level.block.TransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Glass block with connected textures (Fusion). Transparent, no occlusion, like Connected Glass.
 */
public class ReactorGlassBlock extends TransparentBlock {

    public ReactorGlassBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }
}
