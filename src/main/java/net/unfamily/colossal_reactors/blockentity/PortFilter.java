package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.network.chat.Component;

/**
 * What the port outputs in extract/eject mode: both waste and liquid, waste only, or liquid only.
 */
public enum PortFilter {
    BOTH(0),
    ONLY_SOLID_FUEL(1),
    ONLY_COOLANT_LIQUID(2);

    private static final String LANG_PREFIX = "gui.colossal_reactors.resource_port.output.";

    private final int id;

    PortFilter(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable(LANG_PREFIX + getLangSuffix());
    }

    /** Tooltip key (single line). */
    public String getTooltipKey() {
        return LANG_PREFIX + getLangSuffix() + ".tooltip";
    }

    private String getLangSuffix() {
        return switch (this) {
            case BOTH -> "both";
            case ONLY_SOLID_FUEL -> "waste_only";
            case ONLY_COOLANT_LIQUID -> "liquid_only";
        };
    }

    public static PortFilter fromId(int id) {
        return switch (id) {
            case 0 -> BOTH;
            case 1 -> ONLY_SOLID_FUEL;
            case 2 -> ONLY_COOLANT_LIQUID;
            default -> BOTH;
        };
    }
}
