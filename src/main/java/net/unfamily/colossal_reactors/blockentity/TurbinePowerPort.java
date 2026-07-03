package net.unfamily.colossal_reactors.blockentity;

/**
 * Turbine shell block that accepts RF pushed from the turbine controller simulation.
 */
public interface TurbinePowerPort {

    /**
     * @param maxAmount max RF to accept this call
     * @return amount actually accepted
     */
    long receiveEnergyFromTurbine(long maxAmount);

    long getStoredEnergyLong();

    long getMaxEnergyLong();

    default long availableSpaceLong() {
        return Math.max(0L, getMaxEnergyLong() - getStoredEnergyLong());
    }

    default boolean canAcceptMoreFromTurbine() {
        return availableSpaceLong() > 0L;
    }
}
