package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.transfer.IntBackedForgeEnergyStorage;

public class TurbinePowerPortBlockEntity extends BlockEntity implements TurbinePowerPort {

    private static final String TAG_ENERGY = "Energy";

    private final int maxExtractPerTick;
    private final IntBackedForgeEnergyStorage energyStorage;

    public TurbinePowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_POWER_PORT_BE.get(), pos, state);
        int capacity = Config.TURBINE_POWER_PORT_CAPACITY.get();
        int maxExtractCfg = Config.TURBINE_POWER_PORT_MAX_EXTRACT.get();
        this.maxExtractPerTick = Math.min(capacity, maxExtractCfg);
        this.energyStorage = new IntBackedForgeEnergyStorage(capacity, 0, capacity, 0);
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
    public long receiveEnergyFromTurbine(long maxAmount) {
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
