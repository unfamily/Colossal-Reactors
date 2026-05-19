package net.unfamily.colossal_reactors.transfer;

import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * {@link IEnergyStorage} with {@code int} capacity and stored energy (standard FE port).
 */
public class IntBackedForgeEnergyStorage implements IEnergyStorage {

    private int energy;
    private final int capacity;
    private final int maxReceivePerOp;
    private final int maxExtractPerOp;

    public IntBackedForgeEnergyStorage(int capacity, int maxReceivePerOp, int maxExtractPerOp) {
        this(capacity, maxReceivePerOp, maxExtractPerOp, 0);
    }

    public IntBackedForgeEnergyStorage(int capacity, int maxReceivePerOp, int maxExtractPerOp, int initialEnergy) {
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
        int received = (int) Math.min((long) capacity - energy, Math.min(maxReceive, maxReceivePerOp));
        if (!simulate) {
            energy += received;
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) return 0;
        int extracted = Math.min(energy, Math.min(maxExtract, maxExtractPerOp));
        if (!simulate) {
            energy -= extracted;
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return maxExtractPerOp > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceivePerOp > 0;
    }

    public void setEnergy(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }

    /**
     * Adds energy up to capacity (e.g. reactor push). Returns amount accepted.
     */
    public int addEnergy(int maxReceive) {
        if (maxReceive <= 0) return 0;
        int space = capacity - energy;
        int received = Math.min(space, maxReceive);
        if (received > 0) {
            energy += received;
        }
        return received;
    }

    /** Accepts reactor output; clamps to {@link Integer#MAX_VALUE} per tick. */
    public long addEnergyFromReactor(long maxAmount) {
        if (maxAmount <= 0) return 0;
        int chunk = maxAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxAmount;
        return addEnergy(chunk);
    }
}
