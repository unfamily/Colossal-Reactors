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
}
