package net.unfamily.colossal_reactors.multiblock;

/**
 * Grow port buffers immediately; shrink only when stored content fits the new target.
 */
public final class PortCapacityPolicy {

    private PortCapacityPolicy() {}

    /** Grow immediately; defer shrink while {@code storedMb > targetMb}. */
    public static long resolveFluidCapacity(long targetMb, long currentCapMb, long storedMb) {
        if (targetMb <= 0) {
            return Math.max(0L, currentCapMb);
        }
        if (targetMb >= currentCapMb) {
            return targetMb;
        }
        return storedMb > targetMb ? currentCapMb : targetMb;
    }

    /** Grow immediately; defer shrink while {@code storedRf > targetRf}. */
    public static long resolveEnergyCapacity(long targetRf, long currentCapRf, long storedRf) {
        if (targetRf <= 0) {
            return Math.max(0L, currentCapRf);
        }
        if (targetRf >= currentCapRf) {
            return targetRf;
        }
        return storedRf > targetRf ? currentCapRf : targetRf;
    }
}
