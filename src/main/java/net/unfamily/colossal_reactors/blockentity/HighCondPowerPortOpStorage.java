package net.unfamily.colossal_reactors.blockentity;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import net.unfamily.colossal_reactors.transfer.LongBackedForgeEnergyStorage;

/**
 * Output-only {@link IOPStorage} for high-conduction power ports (Brandon's Core OP / Draconic).
 * 1 OP equals 1 FE; uses long transfers when neighbors support {@link IOPStorage}.
 */
final class HighCondPowerPortOpStorage implements IOPStorage {

    private final LongBackedForgeEnergyStorage storage;
    private final long maxExtractPerTick;

    HighCondPowerPortOpStorage(LongBackedForgeEnergyStorage storage, long maxExtractPerTick) {
        this.storage = storage;
        this.maxExtractPerTick = maxExtractPerTick;
    }

    @Override
    public long extractOP(long maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) return 0L;
        long limit = Math.min(maxExtract, Math.min(maxExtractPerTick, storage.getEnergyStoredLong()));
        return storage.extractEnergyLong(limit, simulate);
    }

    @Override
    public long receiveOP(long maxReceive, boolean simulate) {
        return 0L;
    }

    @Override
    public long getOPStored() {
        return storage.getEnergyStoredLong();
    }

    @Override
    public long getMaxOPStored() {
        return storage.getMaxEnergyStoredLong();
    }

    @Override
    public long modifyEnergyStored(long amount) {
        if (amount > 0) {
            return storage.addEnergy(amount);
        }
        if (amount < 0) {
            return storage.extractEnergyLong(-amount, false);
        }
        return 0L;
    }

    @Override
    public boolean canExtract() {
        return maxExtractPerTick > 0;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
