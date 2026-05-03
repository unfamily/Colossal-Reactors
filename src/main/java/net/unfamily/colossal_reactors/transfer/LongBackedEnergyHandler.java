package net.unfamily.colossal_reactors.transfer;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * {@link EnergyHandler} with {@code long} capacity and stored amount (NeoForge {@code insert}/{@code extract} remain per-call {@code int}).
 * Serializes {@code energyL}; falls back to legacy int {@code energy} (old {@link net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler} saves).
 */
public class LongBackedEnergyHandler implements EnergyHandler, ValueIOSerializable {

    private static final String TAG_ENERGY_LONG = "energyL";
    /** Legacy key from {@code SimpleEnergyHandler} on power ports before long migration. */
    private static final String TAG_ENERGY_LEGACY = "energy";

    protected long energy;
    protected long capacity;
    protected long maxInsert;
    protected long maxExtract;

    private final EnergyJournal energyJournal = new EnergyJournal();
    private final Runnable onChange;

    public LongBackedEnergyHandler(long capacity, long maxInsert, long maxExtract, long initialEnergy, Runnable onChange) {
        if (capacity < 0 || maxInsert < 0 || maxExtract < 0 || initialEnergy < 0) {
            throw new IllegalArgumentException("Energy handler arguments must be non-negative");
        }
        this.capacity = capacity;
        this.maxInsert = maxInsert;
        this.maxExtract = maxExtract;
        this.energy = Math.min(capacity, initialEnergy);
        this.onChange = onChange != null ? onChange : () -> {};
    }

    @Override
    public long getAmountAsLong() {
        return energy;
    }

    @Override
    public long getCapacityAsLong() {
        return capacity;
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0) return 0;
        long space = capacity - energy;
        if (space <= 0) return 0;
        long allowed = Math.min((long) amount, Math.min(space, maxInsert));
        int inserting = allowed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) allowed;
        if (inserting <= 0) return 0;
        energyJournal.updateSnapshots(transaction);
        energy += inserting;
        return inserting;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0) return 0;
        if (energy <= 0) return 0;
        long allowed = Math.min((long) amount, Math.min(energy, maxExtract));
        int extracting = allowed > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) allowed;
        if (extracting <= 0) return 0;
        energyJournal.updateSnapshots(transaction);
        energy -= extracting;
        return extracting;
    }

    /**
     * Sets stored energy, clamped to {@code [0, capacity]}.
     */
    public void setEnergy(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("Energy amount must be non-negative");
        }
        long clamped = Math.min(amount, capacity);
        if (this.energy != clamped) {
            long previous = this.energy;
            this.energy = clamped;
            onEnergyChangedDirect(previous);
        }
    }

    /**
     * Adds energy up to capacity; returns amount actually added.
     */
    public long addEnergy(long delta) {
        if (delta <= 0) return 0;
        long previous = energy;
        long space = capacity - energy;
        long add = Math.min(delta, space);
        if (add > 0) {
            energy += add;
            onEnergyChangedDirect(previous);
        }
        return add;
    }

    /**
     * Removes up to {@code maxExtract} energy (e.g. Flux {@code long} push). Not routed through NeoForge {@link #extract}
     * transactions; use only for internal port output where the peer is not an {@link EnergyHandler}.
     */
    public long extractEnergyLong(long maxAmount) {
        if (maxAmount <= 0 || maxExtract <= 0) return 0L;
        long extracted = Math.min(energy, Math.min(maxAmount, maxExtract));
        if (extracted > 0) {
            long previous = energy;
            energy -= extracted;
            onEnergyChangedDirect(previous);
        }
        return extracted;
    }

    protected void onEnergyChangedDirect(long previousAmount) {
        onChange.run();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putLong(TAG_ENERGY_LONG, energy);
    }

    @Override
    public void deserialize(ValueInput input) {
        long fallback = input.getIntOr(TAG_ENERGY_LEGACY, 0);
        long loaded = input.getLongOr(TAG_ENERGY_LONG, fallback);
        energy = Math.max(0L, Math.min(capacity, loaded));
    }

    private class EnergyJournal extends SnapshotJournal<Long> {
        @Override
        protected Long createSnapshot() {
            return energy;
        }

        @Override
        protected void revertToSnapshot(Long snapshot) {
            energy = snapshot;
        }

        @Override
        protected void onRootCommit(Long originalState) {
            if (energy != originalState) {
                onChange.run();
            }
        }
    }
}
