package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * BlockEntity for Power Port. Large energy buffer, output-only to the world.
 * The reactor will push energy via {@link #receiveEnergyFromReactor(int)}.
 * Capacity and max extract are sized for large reactor output (see big_reactor formulas).
 */
public class PowerPortBlockEntity extends BlockEntity {

    /** Buffer capacity: ~20s at 1M FE/t. From CSV, reactors can do 8k–128k+ RF/t. */
    public static final int CAPACITY = 50_000_000;
    /** Max transfer per tick (cables pull this much). 1M FE/t for large reactors. */
    public static final int MAX_EXTRACT = 1_000_000;

    private final PowerPortEnergyStorage energyStorage;

    public PowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_PORT_BE.get(), pos, state);
        this.energyStorage = new PowerPortEnergyStorage(CAPACITY, MAX_EXTRACT);
    }

    /** Internal storage (used for reactor push and for the output wrapper). */
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /**
     * Output-only view for capability (cables/consumers can only extract).
     * The reactor must use {@link #receiveEnergyFromReactor(int)} to push energy.
     */
    public IEnergyStorage getEnergyStorageForCapability() {
        return new OutputOnlyEnergyWrapper(energyStorage);
    }

    /**
     * Called by the reactor controller to push produced energy into this port.
     *
     * @param maxAmount max FE to accept this call
     * @return amount actually accepted
     */
    public int receiveEnergyFromReactor(int maxAmount) {
        int received = energyStorage.receiveEnergyInternal(maxAmount);
        if (received > 0) {
            setChanged();
        }
        return received;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(tag.getInt("Energy"));
    }

    /** Internal storage with settable energy and unlimited receive for reactor. */
    private static final class PowerPortEnergyStorage extends EnergyStorage {
        public PowerPortEnergyStorage(int capacity, int maxExtract) {
            super(capacity, 0, maxExtract);
        }

        public void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }

        /** Bypass receive limit for reactor push. */
        int receiveEnergyInternal(int maxReceive) {
            int received = Math.min(capacity - energy, Math.max(0, maxReceive));
            energy += received;
            return received;
        }
    }

    /** Wrapper that only allows extraction (output-only port). */
    private static final class OutputOnlyEnergyWrapper implements IEnergyStorage {
        private final PowerPortEnergyStorage delegate;

        OutputOnlyEnergyWrapper(PowerPortEnergyStorage delegate) {
            this.delegate = delegate;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return delegate.extractEnergy(maxExtract, simulate);
        }

        @Override
        public int getEnergyStored() {
            return delegate.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {
            return delegate.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
