package net.unfamily.colossal_reactors.transfer;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * {@link IEnergyStorage} backed by {@code long} capacity and stored energy. Per-call Forge API remains {@code int};
 * values above {@link Integer#MAX_VALUE} are clamped for {@link #getEnergyStored()} / {@link #getMaxEnergyStored()}.
 */
public class LongBackedForgeEnergyStorage implements IEnergyStorage {

    private long energy;
    private final long capacity;
    private final long maxReceivePerOp;
    private final long maxExtractPerOp;

    public LongBackedForgeEnergyStorage(long capacity, long maxReceivePerOp, long maxExtractPerOp) {
        this(capacity, maxReceivePerOp, maxExtractPerOp, 0L);
    }

    public LongBackedForgeEnergyStorage(long capacity, long maxReceivePerOp, long maxExtractPerOp, long initialEnergy) {
        if (capacity < 0 || maxReceivePerOp < 0 || maxExtractPerOp < 0 || initialEnergy < 0) {
            throw new IllegalArgumentException("Energy storage arguments must be non-negative");
        }
        this.capacity = capacity;
        this.maxReceivePerOp = maxReceivePerOp;
        this.maxExtractPerOp = maxExtractPerOp;
        this.energy = Math.min(capacity, initialEnergy);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (!canReceive() || maxReceive <= 0) return 0;
        long received = Math.min(capacity - energy, Math.min((long) maxReceive, maxReceivePerOp));
        int out = received > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) received;
        if (!simulate) {
            energy += out;
        }
        return out;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) return 0;
        long extracted = Math.min(energy, Math.min((long) maxExtract, maxExtractPerOp));
        int out = extracted > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) extracted;
        if (!simulate) {
            energy -= out;
        }
        return out;
    }

    /**
     * Same as {@link #extractEnergy(int, boolean)} but without {@code int} truncation on the removed amount.
     */
    public long extractEnergyLong(long maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) return 0L;
        long extracted = Math.min(energy, Math.min(maxExtract, maxExtractPerOp));
        if (!simulate) {
            energy -= extracted;
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return (int) Math.min(energy, Integer.MAX_VALUE);
    }

    @Override
    public int getMaxEnergyStored() {
        return (int) Math.min(capacity, Integer.MAX_VALUE);
    }

    @Override
    public boolean canExtract() {
        return maxExtractPerOp > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceivePerOp > 0;
    }

    public long getEnergyStoredLong() {
        return energy;
    }

    public long getMaxEnergyStoredLong() {
        return capacity;
    }

    public void setEnergy(long amount) {
        energy = Math.max(0L, Math.min(capacity, amount));
    }

    /**
     * Adds energy up to capacity (e.g. reactor push). Returns amount accepted.
     */
    public long addEnergy(long maxReceive) {
        if (maxReceive <= 0) return 0;
        long space = capacity - energy;
        long received = Math.min(space, maxReceive);
        if (received > 0) {
            energy += received;
        }
        return received;
    }
}
