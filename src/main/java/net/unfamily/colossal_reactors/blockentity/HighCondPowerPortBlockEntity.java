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
import net.unfamily.colossal_reactors.integration.brandonscore.BrandonScoreIntegration;
import net.unfamily.colossal_reactors.multiblock.PortCapacityPolicy;
import net.unfamily.colossal_reactors.multiblock.PortScalingConstants;
import net.unfamily.colossal_reactors.transfer.FluxNetworksLongEnergyBridge;
import net.unfamily.colossal_reactors.transfer.LongBackedForgeEnergyStorage;

/**
 * High-conduction power port: {@code long} buffer and transfer rates.
 * With Brandon's Core / Draconic: same {@code IOPStorage} instance is exposed as OP and FE (1 OP = 1 FE).
 * Without BC: Forge FE auto-push and FE capability.
 */
public class HighCondPowerPortBlockEntity extends BlockEntity implements ReactorPowerPort {

    private static final String TAG_ENERGY_LONG = "EnergyL";
    private static final String TAG_ENERGY_LEGACY = "Energy";

    private long maxExtractPerTick;
    private LongBackedForgeEnergyStorage energyStorage;
    private Object opOutput;

    public HighCondPowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIGH_COND_POWER_PORT_BE.get(), pos, state);
        applyEnergyCapacity(PortScalingConstants.MIN_ENERGY_BUFFER_RF);
    }

    public void applyEnergyCapacity(long targetCapacity) {
        long cap = Math.min(PortScalingConstants.LONG_ENERGY_CAP,
                Math.max(PortScalingConstants.MIN_ENERGY_BUFFER_RF, targetCapacity));
        if (energyStorage != null) {
            cap = PortCapacityPolicy.resolveEnergyCapacity(cap,
                    energyStorage.getMaxEnergyStoredLong(), energyStorage.getEnergyStoredLong());
        }
        if (energyStorage != null && energyStorage.getMaxEnergyStoredLong() == cap && maxExtractPerTick == cap) {
            return;
        }
        maxExtractPerTick = cap;
        if (energyStorage == null) {
            energyStorage = new LongBackedForgeEnergyStorage(cap, cap, cap, 0L);
        } else {
            energyStorage.resize(cap, cap, cap);
        }
        setChanged();
    }

    public LongBackedForgeEnergyStorage getBackingEnergyStorage() {
        return energyStorage;
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        long budget = Math.min(maxExtractPerTick, energyStorage.getEnergyStoredLong());
        if (budget <= 0) return;

        if (BrandonScoreIntegration.isBrandonScoreLoaded()) {
            Object op = getOpStorageForCapability();
            if (op == null) {
                return;
            }
            for (Direction direction : Direction.values()) {
                if (budget <= 0) break;
                BlockPos neighborPos = worldPosition.relative(direction);
                Direction intoNeighbor = direction.getOpposite();
                long offer = Math.min(budget, energyStorage.getEnergyStoredLong());
                if (offer <= 0) continue;
                long moved = BrandonScoreIntegration.tryPushOpToNeighbor(level, neighborPos, intoNeighbor, op, offer);
                if (moved > 0) {
                    budget -= moved;
                    setChanged();
                }
            }
            return;
        }

        for (Direction direction : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighborPos = worldPosition.relative(direction);
            Direction intoNeighbor = direction.getOpposite();
            long offer = Math.min(budget, energyStorage.getEnergyStoredLong());
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
                int received = neighbor.receiveEnergy(chunk, false);
                if (received > 0) {
                    energyStorage.extractEnergyLong(received, false);
                    budget -= received;
                    setChanged();
                }
            }
        }
    }

    public IEnergyStorage getEnergyStorageForCapability() {
        if (BrandonScoreIntegration.isBrandonScoreLoaded()) {
            Object op = getOpStorageForCapability();
            return op instanceof IEnergyStorage storage ? storage : null;
        }
        return new OutputOnlyEnergyWrapper(energyStorage);
    }

    /** OP capability when Brandon's Core is loaded. */
    public Object getOpStorageForCapability() {
        if (!BrandonScoreIntegration.isBrandonScoreLoaded()) {
            return null;
        }
        if (opOutput == null) {
            opOutput = BrandonScoreIntegration.createOpStorage(energyStorage);
        }
        return opOutput;
    }

    @Override
    public long getStoredEnergyLong() {
        return energyStorage.getEnergyStoredLong();
    }

    @Override
    public long getMaxEnergyLong() {
        return energyStorage.getMaxEnergyStoredLong();
    }

    @Override
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
