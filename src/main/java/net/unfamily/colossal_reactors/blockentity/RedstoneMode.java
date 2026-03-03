package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.network.chat.Component;

/**
 * Redstone mode for Redstone Port. Same semantics as iskandert_utilities (NONE, LOW, HIGH, DISABLED).
 * PULSE is not used.
 */
public enum RedstoneMode {
    NONE(0),
    LOW(1),
    HIGH(2),
    DISABLED(3);

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
            case 3 -> DISABLED;
            default -> NONE;
        };
    }

    /** Cycle to next mode (NONE -> LOW -> HIGH -> DISABLED -> NONE). */
    public RedstoneMode next() {
        return switch (this) {
            case NONE -> LOW;
            case LOW -> HIGH;
            case HIGH -> DISABLED;
            case DISABLED -> NONE;
        };
    }
}
