package net.unfamily.colossal_reactors.gas;

import net.neoforged.neoforge.fluids.BaseFlowingFluid;

/** Logical gas fluid for stacks and ports; no {@link net.minecraft.world.level.block.LiquidBlock} in world. */
public final class GasFlowingFluid {

    private GasFlowingFluid() {}

    public static final class Source extends BaseFlowingFluid.Source {
        public Source(Properties properties) {
            super(properties);
        }
    }

    public static final class Flowing extends BaseFlowingFluid.Flowing {
        public Flowing(Properties properties) {
            super(properties);
        }
    }
}
