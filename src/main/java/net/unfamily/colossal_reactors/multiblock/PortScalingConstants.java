package net.unfamily.colossal_reactors.multiblock;

/**
 * Internal constants for automatic resource and power port scaling on multiblock rebuild.
 */
public final class PortScalingConstants {

    public static final int MIN_FLUID_TANK_MB = 16000;
    public static final int DEMAND_MULTIPLIER = 10;
    public static final long MIN_ENERGY_BUFFER_RF = 10_000L;
    public static final int INT_ENERGY_CAP = Integer.MAX_VALUE;
    public static final long LONG_ENERGY_CAP = Long.MAX_VALUE;

    private PortScalingConstants() {}
}
