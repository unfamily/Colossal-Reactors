package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.TurbineRodBlock;
import net.unfamily.colossal_reactors.turbine.TurbineRodConnectorVisibility;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-local rod context for BER {@code renderSingleBlock}, which may not pass {@link TurbineRotorModelData}.
 */
public final class TurbineRodRenderScope {

    private static final ThreadLocal<@Nullable Context> CONTEXT = new ThreadLocal<>();

    private record Context(Level level, BlockPos rodPos, BlockState rodState, int connectorMask) {}

    private TurbineRodRenderScope() {}

    public static void run(Level level, BlockPos rodPos, BlockState rodState, Runnable action) {
        int mask = 0;
        if (rodState.is(net.unfamily.colossal_reactors.block.ModBlocks.TURBINE_ROD.get())
                && rodState.hasProperty(TurbineRodBlock.FACING)) {
            mask = TurbineRodConnectorVisibility.lateralConnectorMask(
                    level, rodPos, rodState.getValue(TurbineRodBlock.FACING));
        }
        CONTEXT.set(new Context(level, rodPos, rodState, mask));
        try {
            action.run();
        } finally {
            CONTEXT.remove();
        }
    }

    @Nullable
    public static Integer connectorMaskOrNull() {
        Context ctx = CONTEXT.get();
        return ctx != null ? ctx.connectorMask() : null;
    }
}
