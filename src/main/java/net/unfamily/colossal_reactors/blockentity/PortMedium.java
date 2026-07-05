package net.unfamily.colossal_reactors.blockentity;

/**
 * Exclusive transport medium for a resource port (one of solid, liquid, or gas).
 */
public enum PortMedium {
    SOLID(0),
    LIQUID(1),
    GAS(2);

    private final int id;

    PortMedium(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isSolid() {
        return this == SOLID;
    }

    public boolean isLiquid() {
        return this == LIQUID;
    }

    public boolean isGas() {
        return this == GAS;
    }

    public PortMedium nextForReactor() {
        return nextForReactor(true);
    }

    public PortMedium nextForReactor(boolean gasAvailable) {
        return switch (this) {
            case SOLID -> LIQUID;
            case LIQUID -> gasAvailable ? GAS : SOLID;
            case GAS -> SOLID;
        };
    }

    public PortMedium nextForTurbine() {
        return nextForTurbine(true);
    }

    public PortMedium nextForTurbine(boolean gasAvailable) {
        if (!gasAvailable) {
            return LIQUID;
        }
        return this == LIQUID ? GAS : LIQUID;
    }

    public PortMedium prevForReactor() {
        return prevForReactor(true);
    }

    public PortMedium prevForReactor(boolean gasAvailable) {
        return switch (this) {
            case SOLID -> gasAvailable ? GAS : LIQUID;
            case LIQUID -> SOLID;
            case GAS -> LIQUID;
        };
    }

    public PortMedium prevForTurbine() {
        return prevForTurbine(true);
    }

    public PortMedium prevForTurbine(boolean gasAvailable) {
        if (!gasAvailable) {
            return LIQUID;
        }
        return this == LIQUID ? GAS : LIQUID;
    }

    /** Normalize legacy save data where multiple booleans could be true. */
    public static PortMedium fromLegacyBooleans(boolean allowSolid, boolean allowLiquid, boolean allowGas) {
        if (allowGas) {
            return GAS;
        }
        if (allowLiquid && !allowSolid) {
            return LIQUID;
        }
        return SOLID;
    }

    public static PortMedium fromId(int id) {
        return switch (id) {
            case 1 -> LIQUID;
            case 2 -> GAS;
            default -> SOLID;
        };
    }
}
