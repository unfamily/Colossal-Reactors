package net.unfamily.colossal_reactors.transfer;

import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

/**
 * Fluid storage that can snapshot / restore full tank state for NeoForge transfer transactions.
 */
public interface RestorableFluidStorage {

    record Snapshot(@Nullable FluidStack fluid, long amountMb) {
        public static Snapshot empty() {
            return new Snapshot(null, 0L);
        }

        public Snapshot copy() {
            if (fluid == null || fluid.isEmpty() || amountMb <= 0L) {
                return empty();
            }
            return new Snapshot(new FluidStack(fluid.getFluid(), 1), amountMb);
        }
    }

    Snapshot captureFluidState();

    void restoreFluidState(Snapshot snapshot);

    /** Called after a root transfer transaction commits a real change. */
    default void onTransactionCommitted() {}
}
