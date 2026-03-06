package net.unfamily.colossal_reactors.block;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Ender goo liquid block. Teleport-on-contact for entities is handled in
 * {@link net.unfamily.colossal_reactors.fluid.SpecialFluidEffects}.
 */
public class EnderGooBlock extends LiquidBlock {

    public EnderGooBlock(FlowingFluid fluid, Properties properties) {
        super(fluid, properties);
    }
}
