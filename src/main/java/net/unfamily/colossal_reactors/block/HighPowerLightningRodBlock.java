package net.unfamily.colossal_reactors.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * Lightning rod block placed on top of the Lightning Generator.
 * When present, the generator creates lightning by itself when it rains (15–30s) or thunders (3–5s).
 */
public class HighPowerLightningRodBlock extends Block {

    public HighPowerLightningRodBlock(Properties properties) {
        super(properties);
    }
}
