package net.unfamily.colossal_reactors.transfer;

import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import java.util.Objects;

/**
 * Presents legacy {@link IEnergyStorage} as {@link EnergyHandler} for NeoForge 26 capabilities.
 */
public final class LegacyEnergyStorageEnergyHandler implements EnergyHandler {

    private final IEnergyStorage delegate;

    public LegacyEnergyStorageEnergyHandler(IEnergyStorage delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public long getAmountAsLong() {
        return delegate.getEnergyStored();
    }

    @Override
    public long getCapacityAsLong() {
        return delegate.getMaxEnergyStored();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || !delegate.canReceive()) return 0;
        try (Transaction sub = Transaction.open(transaction)) {
            int sim = delegate.receiveEnergy(amount, true);
            if (sim <= 0) return 0;
            int actual = delegate.receiveEnergy(sim, false);
            sub.commit();
            return actual;
        }
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || !delegate.canExtract()) return 0;
        try (Transaction sub = Transaction.open(transaction)) {
            int sim = delegate.extractEnergy(amount, true);
            if (sim <= 0) return 0;
            int actual = delegate.extractEnergy(sim, false);
            sub.commit();
            return actual;
        }
    }
}
