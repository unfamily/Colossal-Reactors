package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.network.chat.Component;

/**
 * Redstone mode. Same semantics as iskandert_utilities: NONE, LOW, HIGH, PULSE, DISABLED.
 * Used by Redstone Port (cycle skips PULSE), Melter and Heating Coil (full cycle including PULSE).
 */
public enum RedstoneMode {
    NONE(0),
    LOW(1),
    HIGH(2),
    PULSE(3),
    DISABLED(4);

    private final int id;

    RedstoneMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable("gui.colossal_reactors.redstone_port.mode." + name().toLowerCase());
    }

    public static RedstoneMode fromId(int id) {
        return switch (id) {
            case 0 -> NONE;
            case 1 -> LOW;
            case 2 -> HIGH;
            case 3 -> PULSE;
            case 4 -> DISABLED;
            default -> NONE;
        };
    }

    /** Full cycle including PULSE (for Melter, Heating Coil). */
    public RedstoneMode next() {
        return switch (this) {
            case NONE -> LOW;
            case LOW -> HIGH;
            case HIGH -> PULSE;
            case PULSE -> DISABLED;
            case DISABLED -> NONE;
        };
    }

    /** Cycle without PULSE (for Redstone Port). */
    public RedstoneMode nextNoPulse() {
        RedstoneMode n = next();
        return n == PULSE ? next() : n;
    }
}
