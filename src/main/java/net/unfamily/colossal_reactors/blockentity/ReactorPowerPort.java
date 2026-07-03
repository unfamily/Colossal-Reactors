package net.unfamily.colossal_reactors.blockentity;

/**
 * Any reactor shell block that accepts RF pushed from the controller simulation.
 */
public interface ReactorPowerPort {

    /**
     * @param maxAmount max RF to accept this call
     * @return amount actually accepted
     */
    long receiveEnergyFromReactor(long maxAmount);

    long getStoredEnergyLong();

    long getMaxEnergyLong();

    default long availableSpaceLong() {
        return Math.max(0L, getMaxEnergyLong() - getStoredEnergyLong());
    }

    default boolean canAcceptMoreFromReactor() {
        return availableSpaceLong() > 0L;
    }
}
