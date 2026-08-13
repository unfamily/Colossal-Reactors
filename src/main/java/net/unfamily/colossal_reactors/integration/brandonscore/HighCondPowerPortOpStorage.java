package net.unfamily.colossal_reactors.integration.brandonscore;

import com.brandon3055.brandonscore.api.power.IOPStorage;
import net.unfamily.colossal_reactors.transfer.LongBackedEnergyHandler;

/**
 * Output-only {@link IOPStorage} for high-conduction power ports (Brandon's Core OP / Draconic).
 * 1 OP equals 1 FE; uses long transfers when neighbors support {@link IOPStorage}.
 * Extract limit follows live storage after {@code resize()}.
 */
public final class HighCondPowerPortOpStorage implements IOPStorage {

    private final LongBackedEnergyHandler storage;

    public HighCondPowerPortOpStorage(LongBackedEnergyHandler storage) {
        this.storage = storage;
    }

    @Override
    public long extractOP(long maxExtract, boolean simulate) {
        if (!canExtract() || maxExtract <= 0) return 0L;
        long limit = Math.min(maxExtract, Math.min(storage.getMaxExtract(), storage.getAmountAsLong()));
        if (simulate) {
            return limit;
        }
        return storage.extractEnergyLong(limit);
    }

    @Override
    public long receiveOP(long maxReceive, boolean simulate) {
        return 0L;
    }

    @Override
    public long getOPStored() {
        return storage.getAmountAsLong();
    }

    @Override
    public long getMaxOPStored() {
        return storage.getCapacityAsLong();
    }

    @Override
    public long modifyEnergyStored(long amount) {
        if (amount > 0) {
            return storage.addEnergy(amount);
        }
        if (amount < 0) {
            return storage.extractEnergyLong(-amount);
        }
        return 0L;
    }

    @Override
    public boolean canExtract() {
        return storage.getMaxExtract() > 0;
    }

    @Override
    public boolean canReceive() {
        return false;
    }
}
