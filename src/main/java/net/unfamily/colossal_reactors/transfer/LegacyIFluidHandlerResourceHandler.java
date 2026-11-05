package net.unfamily.colossal_reactors.transfer;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

/**
 * Presents a legacy {@link IFluidHandler} as {@link ResourceHandler}{@code <FluidResource>} for NeoForge 26.
 * <p>
 * When the delegate (or an underlying {@link FluidTank}/{@link LongBackedFluidTank}) supports restore,
 * mutations participate in transfer transactions via {@link SnapshotJournal} (rollback-safe).
 * Without restore support, only {@link IFluidHandler.FluidAction#SIMULATE} is used so transfers
 * cannot destroy fluid.
 */
public final class LegacyIFluidHandlerResourceHandler implements ResourceHandler<FluidResource> {

    private final IFluidHandler delegate;
    private final RestorableFluidStorage restorable;
    private final FluidJournal journal;

    private LegacyIFluidHandlerResourceHandler(IFluidHandler delegate) {
        this.delegate = Objects.requireNonNull(delegate);
        this.restorable = resolveRestorable(delegate);
        this.journal = restorable != null ? new FluidJournal() : null;
    }

    public static ResourceHandler<FluidResource> wrap(IFluidHandler delegate) {
        return new LegacyIFluidHandlerResourceHandler(delegate);
    }

    private static RestorableFluidStorage resolveRestorable(IFluidHandler delegate) {
        if (delegate instanceof RestorableFluidStorage r) {
            return r;
        }
        if (delegate instanceof FluidTank tank) {
            return new FluidTankRestorable(tank);
        }
        return null;
    }

    @Override
    public int size() {
        return delegate.getTanks();
    }

    @Override
    public FluidResource getResource(int index) {
        Objects.checkIndex(index, size());
        return FluidResource.of(delegate.getFluidInTank(index));
    }

    @Override
    public long getAmountAsLong(int index) {
        Objects.checkIndex(index, size());
        if (restorable != null) {
            return restorable.captureFluidState().amountMb();
        }
        return delegate.getFluidInTank(index).getAmount();
    }

    @Override
    public long getCapacityAsLong(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        if (!resource.isEmpty() && !delegate.isFluidValid(index, resource.toStack(1))) {
            return 0;
        }
        return delegate.getTankCapacity(index);
    }

    @Override
    public boolean isValid(int index, FluidResource resource) {
        Objects.checkIndex(index, size());
        if (resource.isEmpty()) return true;
        return delegate.isFluidValid(index, resource.toStack(1));
    }

    @Override
    public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        Objects.checkIndex(index, size());
        return insert(resource, amount, transaction);
    }

    @Override
    public int insert(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        FluidStack stack = resource.toStack(amount);
        int sim = delegate.fill(stack, IFluidHandler.FluidAction.SIMULATE);
        if (sim <= 0) return 0;
        if (restorable == null || journal == null) {
            // No rollback support: never mutate so pipe transactions cannot destroy fluid.
            return sim;
        }
        journal.updateSnapshots(transaction);
        return delegate.fill(resource.toStack(sim), IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        Objects.checkIndex(index, size());
        return extract(resource, amount, transaction);
    }

    @Override
    public int extract(FluidResource resource, int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonEmptyNonNegative(resource, amount);
        FluidStack template = resource.toStack(amount);
        FluidStack sim = delegate.drain(template, IFluidHandler.FluidAction.SIMULATE);
        if (sim.isEmpty()) return 0;
        if (restorable == null || journal == null) {
            return sim.getAmount();
        }
        journal.updateSnapshots(transaction);
        FluidStack drained = delegate.drain(template, IFluidHandler.FluidAction.EXECUTE);
        return drained.getAmount();
    }

    private final class FluidJournal extends SnapshotJournal<RestorableFluidStorage.Snapshot> {
        @Override
        protected RestorableFluidStorage.Snapshot createSnapshot() {
            return restorable.captureFluidState().copy();
        }

        @Override
        protected void revertToSnapshot(RestorableFluidStorage.Snapshot snapshot) {
            restorable.restoreFluidState(snapshot);
        }

        @Override
        protected void onRootCommit(RestorableFluidStorage.Snapshot originalState) {
            RestorableFluidStorage.Snapshot now = restorable.captureFluidState();
            if (originalState.amountMb() != now.amountMb()
                    || !sameFluid(originalState.fluid(), now.fluid())) {
                restorable.onTransactionCommitted();
            }
        }

        private static boolean sameFluid(FluidStack a, FluidStack b) {
            if (a == null || a.isEmpty()) return b == null || b.isEmpty();
            if (b == null || b.isEmpty()) return false;
            return a.getFluid() == b.getFluid();
        }
    }

    /** Snapshot adapter for NeoForge {@link FluidTank}. */
    private static final class FluidTankRestorable implements RestorableFluidStorage {
        private final FluidTank tank;

        private FluidTankRestorable(FluidTank tank) {
            this.tank = tank;
        }

        @Override
        public Snapshot captureFluidState() {
            FluidStack f = tank.getFluid();
            if (f.isEmpty()) {
                return Snapshot.empty();
            }
            return new Snapshot(new FluidStack(f.getFluid(), 1), f.getAmount());
        }

        @Override
        public void restoreFluidState(Snapshot snapshot) {
            if (snapshot == null || snapshot.fluid() == null || snapshot.fluid().isEmpty() || snapshot.amountMb() <= 0L) {
                tank.setFluid(FluidStack.EMPTY);
            } else {
                int amount = (int) Math.min(Integer.MAX_VALUE, Math.max(0L, snapshot.amountMb()));
                tank.setFluid(new FluidStack(snapshot.fluid().getFluid(), amount));
            }
        }
    }
}
