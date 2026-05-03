package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.colossal_reactors.transfer.FluxNetworksLongEnergyBridge;
import net.unfamily.colossal_reactors.transfer.LongBackedForgeEnergyStorage;

/**
 * BlockEntity for Power Port. Large energy buffer; reactor pushes in via {@link #receiveEnergyFromReactor(long)}.
 * Each tick the port pushes energy out to adjacent blocks that can receive (cables, machines).
 * Capacity and max extract per tick are read from Config (ports.power). Internal storage is {@code long};
 * {@link IEnergyStorage} calls are still {@code int} per Forge (clamp above {@link Integer#MAX_VALUE}).
 */
public class PowerPortBlockEntity extends BlockEntity {

    private static final String TAG_ENERGY_LONG = "EnergyL";
    private static final String TAG_ENERGY_LEGACY = "Energy";

    private final long maxExtractPerTick;
    private final LongBackedForgeEnergyStorage energyStorage;

    public PowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_PORT_BE.get(), pos, state);
        long capacity = net.unfamily.colossal_reactors.Config.POWER_PORT_CAPACITY.getAsLong();
        long maxExtractCfg = net.unfamily.colossal_reactors.Config.POWER_PORT_MAX_EXTRACT.getAsLong();
        this.maxExtractPerTick = Math.min(capacity, maxExtractCfg);
        this.energyStorage = new LongBackedForgeEnergyStorage(capacity, 0L, capacity, 0L);
    }

    /**
     * Server tick: push energy to adjacent blocks that can receive (cables, machines).
     */
    public void tick() {
        if (level == null || level.isClientSide()) return;
        long budget = Math.min(maxExtractPerTick, energyStorage.getEnergyStoredLong());
        if (budget <= 0) return;
        for (Direction direction : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighborPos = worldPosition.relative(direction);
            Direction intoNeighbor = direction.getOpposite();
            long stored = energyStorage.getEnergyStoredLong();
            long offer = Math.min(budget, stored);
            if (offer <= 0) continue;

            long fluxMoved = FluxNetworksLongEnergyBridge.tryReceiveEnergyLong(level, neighborPos, intoNeighbor, offer);
            if (fluxMoved > 0) {
                energyStorage.extractEnergyLong(fluxMoved, false);
                budget -= fluxMoved;
                setChanged();
                continue;
            }

            IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, intoNeighbor);
            if (neighbor != null && neighbor.canReceive()) {
                int chunk = (int) Math.min(offer, Integer.MAX_VALUE);
                int toSend = (int) Math.min(chunk, energyStorage.getEnergyStoredLong());
                int received = neighbor.receiveEnergy(toSend, false);
                if (received > 0) {
                    energyStorage.extractEnergy(received, false);
                    budget -= received;
                    setChanged();
                }
            }
        }
    }

    /** Internal storage (used for reactor push and for the output wrapper). */
    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /**
     * Output-only view for capability (cables/consumers can only extract).
     * The reactor must use {@link #receiveEnergyFromReactor(long)} to push energy.
     */
    public IEnergyStorage getEnergyStorageForCapability() {
        return new OutputOnlyEnergyWrapper(energyStorage);
    }

    /**
     * Called by the reactor controller to push produced energy into this port.
     *
     * @param maxAmount max RF to accept this call
     * @return amount actually accepted
     */
    public long receiveEnergyFromReactor(long maxAmount) {
        long received = energyStorage.addEnergy(maxAmount);
        if (received > 0) {
            setChanged();
        }
        return received;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong(TAG_ENERGY_LONG, energyStorage.getEnergyStoredLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        long loaded = tag.contains(TAG_ENERGY_LONG, Tag.TAG_LONG)
                ? tag.getLong(TAG_ENERGY_LONG)
                : tag.getInt(TAG_ENERGY_LEGACY);
        energyStorage.setEnergy(Math.max(0L, Math.min(energyStorage.getMaxEnergyStoredLong(), loaded)));
    }

    /** Wrapper that only allows extraction (output-only port). */
    private static final class OutputOnlyEnergyWrapper implements IEnergyStorage {
        private final LongBackedForgeEnergyStorage delegate;

        OutputOnlyEnergyWrapper(LongBackedForgeEnergyStorage delegate) {
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
