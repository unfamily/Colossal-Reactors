package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.colossal_reactors.multiblock.PortCapacityPolicy;
import net.unfamily.colossal_reactors.multiblock.PortScalingConstants;
import net.unfamily.colossal_reactors.transfer.IntBackedForgeEnergyStorage;

/**
 * Standard power port: {@code int} buffer and {@code int} RF/t extraction (Forge FE only).
 */
public class PowerPortBlockEntity extends BlockEntity implements ReactorPowerPort {

    private static final String TAG_ENERGY = "Energy";

    private int maxExtractPerTick;
    private IntBackedForgeEnergyStorage energyStorage;

    public PowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_PORT_BE.get(), pos, state);
        applyEnergyCapacity((int) PortScalingConstants.MIN_ENERGY_BUFFER_RF);
    }

    public void applyEnergyCapacity(int targetCapacity) {
        int cap = (int) Math.min(PortScalingConstants.INT_ENERGY_CAP,
                Math.max(PortScalingConstants.MIN_ENERGY_BUFFER_RF, targetCapacity));
        if (energyStorage != null) {
            cap = (int) PortCapacityPolicy.resolveEnergyCapacity(cap,
                    energyStorage.getMaxEnergyStored(), energyStorage.getEnergyStored());
        }
        if (energyStorage != null && energyStorage.getMaxEnergyStored() == cap && maxExtractPerTick == cap) {
            return;
        }
        maxExtractPerTick = cap;
        if (energyStorage == null) {
            energyStorage = new IntBackedForgeEnergyStorage(cap, cap, cap, 0);
        } else {
            energyStorage.resize(cap, cap, cap);
        }
        setChanged();
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        int budget = Math.min(maxExtractPerTick, energyStorage.getEnergyStored());
        if (budget <= 0) return;
        for (Direction direction : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighborPos = worldPosition.relative(direction);
            Direction intoNeighbor = direction.getOpposite();
            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, intoNeighbor);
            if (neighbor != null && neighbor.canReceive()) {
                int toSend = Math.min(budget, energyStorage.getEnergyStored());
                int received = neighbor.receiveEnergy(toSend, false);
                if (received > 0) {
                    energyStorage.extractEnergy(received, false);
                    budget -= received;
                    setChanged();
                }
            }
        }
    }

    public IEnergyStorage getEnergyStorageForCapability() {
        return new OutputOnlyEnergyWrapper(energyStorage);
    }

    @Override
    public long getStoredEnergyLong() {
        return energyStorage.getEnergyStored();
    }

    @Override
    public long getMaxEnergyLong() {
        return energyStorage.getMaxEnergyStored();
    }

    @Override
    public long receiveEnergyFromReactor(long maxAmount) {
        long received = energyStorage.addEnergyFromReactor(maxAmount);
        if (received > 0) {
            setChanged();
        }
        return received;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_ENERGY, energyStorage.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energyStorage.setEnergy(Math.max(0, Math.min(energyStorage.getMaxEnergyStored(), tag.getInt(TAG_ENERGY))));
    }

    private static final class OutputOnlyEnergyWrapper implements IEnergyStorage {
        private final IntBackedForgeEnergyStorage delegate;

        OutputOnlyEnergyWrapper(IntBackedForgeEnergyStorage delegate) {
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
