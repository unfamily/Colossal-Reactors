package net.unfamily.colossal_reactors.transfer;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

/**
 * {@link EnergyHandler} with {@code int} capacity and stored amount (standard FE power port).
 */
public class IntBackedEnergyHandler implements EnergyHandler, ValueIOSerializable {

    private static final String TAG_ENERGY = "energy";

    protected int energy;
    protected final int capacity;
    protected final int maxInsert;
    protected final int maxExtract;

    private final EnergyJournal energyJournal = new EnergyJournal();
    private final Runnable onChange;

    public IntBackedEnergyHandler(int capacity, int maxInsert, int maxExtract, int initialEnergy, Runnable onChange) {
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
        if (amount == 0 || maxInsert == 0) return 0;
        int space = capacity - energy;
        if (space <= 0) return 0;
        int inserting = Math.min(amount, Math.min(space, maxInsert));
        if (inserting <= 0) return 0;
        energyJournal.updateSnapshots(transaction);
        energy += inserting;
        return inserting;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount == 0 || maxExtract == 0) return 0;
        if (energy <= 0) return 0;
        int extracting = Math.min(amount, Math.min(energy, maxExtract));
        if (extracting <= 0) return 0;
        energyJournal.updateSnapshots(transaction);
        energy -= extracting;
        return extracting;
    }

    public void setEnergy(int amount) {
        energy = Math.max(0, Math.min(capacity, amount));
    }

    public int getEnergyStored() {
        return energy;
    }

    public int getCapacity() {
        return capacity;
    }

    /** Adds energy up to capacity; returns amount actually added. */
    public long addEnergyFromReactor(long maxAmount) {
        if (maxAmount <= 0) return 0;
        int chunk = maxAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) maxAmount;
        int space = capacity - energy;
        int add = Math.min(chunk, space);
        if (add > 0) {
            energy += add;
            onChange.run();
        }
        return add;
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putInt(TAG_ENERGY, energy);
    }

    @Override
    public void deserialize(ValueInput input) {
        energy = Math.max(0, Math.min(capacity, input.getIntOr(TAG_ENERGY, 0)));
    }

    private final class EnergyJournal extends SnapshotJournal<Integer> {
        @Override
        protected Integer createSnapshot() {
            return energy;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            energy = snapshot;
        }

        @Override
        protected void onRootCommit(Integer originalState) {
            if (!originalState.equals(energy)) {
                onChange.run();
            }
        }
    }
}
