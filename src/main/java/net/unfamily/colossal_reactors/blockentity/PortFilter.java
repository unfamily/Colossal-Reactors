package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.network.chat.Component;

/**
 * What the port transfers: both solid fuel and coolant, only solid fuel, or only coolant liquid.
 */
public enum PortFilter {
    BOTH(0),
    ONLY_SOLID_FUEL(1),
    ONLY_COOLANT_LIQUID(2);

    private final int id;

    PortFilter(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable("gui.colossal_reactors.resource_port.filter." + name().toLowerCase());
    }

    /** Tooltip key (single line). */
    public String getTooltipKey() {
        return "gui.colossal_reactors.resource_port.filter." + name().toLowerCase() + ".tooltip";
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
