package net.unfamily.colossal_reactors.blockentity;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import com.brandon3055.brandonscore.capability.CapabilityOP;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.transfer.FluxNetworksLongEnergyBridge;
import net.unfamily.colossal_reactors.transfer.LongBackedForgeEnergyStorage;

public class TurbineHighCondPowerPortBlockEntity extends BlockEntity implements TurbinePowerPort {

    private static final String TAG_ENERGY_LONG = "EnergyL";
    private static final String TAG_ENERGY_LEGACY = "Energy";

    private final long maxExtractPerTick;
    private final LongBackedForgeEnergyStorage energyStorage;
    private final HighCondPowerPortOpStorage opOutput;

    public TurbineHighCondPowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_HIGH_COND_POWER_PORT_BE.get(), pos, state);
        long capacity = Config.TURBINE_HIGH_COND_POWER_PORT_CAPACITY.getAsLong();
        long maxExtractCfg = Config.TURBINE_HIGH_COND_POWER_PORT_MAX_EXTRACT.getAsLong();
        this.maxExtractPerTick = Math.min(capacity, maxExtractCfg);
        this.energyStorage = new LongBackedForgeEnergyStorage(capacity, 0L, capacity, 0L);
        this.opOutput = new HighCondPowerPortOpStorage(energyStorage, maxExtractPerTick);
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
        if (ModList.get().isLoaded("brandonscore")) {
            IOPStorage nativeOp = level.getCapability(CapabilityOP.BLOCK, neighborPos, intoNeighbor);
            if (nativeOp != null) {
                if (nativeOp.canReceive()) {
                    return nativeOp.receiveOP(offer, false);
                }
                return 0L;
            }
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
        return opOutput;
    }

    public IOPStorage getOpStorageForCapability() {
        return opOutput;
    }

    @Override
    public long receiveEnergyFromTurbine(long maxAmount) {
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
