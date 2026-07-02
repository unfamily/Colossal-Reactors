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
import net.neoforged.fml.ModList;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.integration.brandonscore.BrandonScoreIntegration;
import net.unfamily.colossal_reactors.transfer.FluxNetworksLongEnergyBridge;
import net.unfamily.colossal_reactors.transfer.LongBackedForgeEnergyStorage;

/**
 * High-conduction power port: {@code long} buffer and transfer rates.
 * Pushes to native OP when Brandon's Core is present, otherwise Flux long API, then standard FE.
 */
public class HighCondPowerPortBlockEntity extends BlockEntity implements ReactorPowerPort {

    private static final String TAG_ENERGY_LONG = "EnergyL";
    private static final String TAG_ENERGY_LEGACY = "Energy";

    private final long maxExtractPerTick;
    private final LongBackedForgeEnergyStorage energyStorage;
    private Object opOutput;

    public HighCondPowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIGH_COND_POWER_PORT_BE.get(), pos, state);
        long capacity = Config.HIGH_COND_POWER_PORT_CAPACITY.getAsLong();
        long maxExtractCfg = Config.HIGH_COND_POWER_PORT_MAX_EXTRACT.getAsLong();
        this.maxExtractPerTick = Math.min(capacity, maxExtractCfg);
        this.energyStorage = new LongBackedForgeEnergyStorage(capacity, 0L, capacity, 0L);
    }

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

            long moved = tryPushToNeighbor(neighborPos, intoNeighbor, offer);
            if (moved > 0) {
                energyStorage.extractEnergyLong(moved, false);
                budget -= moved;
                setChanged();
            }
        }
    }

    private long tryPushToNeighbor(BlockPos neighborPos, Direction intoNeighbor, long offer) {
        long opMoved = BrandonScoreIntegration.tryPushToNeighbor(level, neighborPos, intoNeighbor, offer);
        if (opMoved > 0) {
            return opMoved;
        }

        long fluxMoved = FluxNetworksLongEnergyBridge.tryReceiveEnergyLong(level, neighborPos, intoNeighbor, offer);
        if (fluxMoved > 0) {
            return fluxMoved;
        }

        IEnergyStorage neighbor = level.getCapability(Capabilities.EnergyStorage.BLOCK, neighborPos, intoNeighbor);
        if (neighbor != null && neighbor.canReceive()) {
            int chunk = (int) Math.min(offer, Integer.MAX_VALUE);
            return neighbor.receiveEnergy(chunk, false);
        }
        return 0L;
    }

    public IEnergyStorage getEnergyStorageForCapability() {
        return energyStorage;
    }

    /** OP capability when Brandon's Core is loaded. */
    public Object getOpStorageForCapability() {
        if (!ModList.get().isLoaded("brandonscore")) {
            return null;
        }
        if (opOutput == null) {
            opOutput = BrandonScoreIntegration.createOpStorage(energyStorage, maxExtractPerTick);
        }
        return opOutput;
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
}
