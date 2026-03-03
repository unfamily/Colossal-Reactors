package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.network.chat.Component;

/**
 * Port mode: insert (reactor accepts uranium ingots + coolant), extract (reactor outputs waste + spent coolant), eject (reactor ejects uranium ingots inside).
 */
public enum PortMode {
    INSERT(0),
    EXTRACT(1),
    EJECT(2);

    private final int id;

    PortMode(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public Component getDisplayName() {
        return Component.translatable("gui.colossal_reactors.resource_port.mode." + name().toLowerCase());
    }

    /** Tooltip key (single line). */
    public String getTooltipKey() {
        return "gui.colossal_reactors.resource_port.mode." + name().toLowerCase() + ".tooltip";
    }

    public static PortMode fromId(int id) {
        return switch (id) {
            case 0 -> INSERT;
            case 1 -> EXTRACT;
            case 2 -> EJECT;
            default -> INSERT;
        };
    }
}
