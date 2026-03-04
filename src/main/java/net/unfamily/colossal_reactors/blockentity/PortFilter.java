package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.network.chat.Component;

/**
 * What the port outputs: in EXTRACT mode = waste + liquid; in EJECT mode = fuel + coolant (input back out).
 */
public enum PortFilter {
    BOTH(0),
    ONLY_SOLID_FUEL(1),
    ONLY_COOLANT_LIQUID(2);

    private static final String LANG_OUTPUT_PREFIX = "gui.colossal_reactors.resource_port.output.";
    private static final String LANG_EJECT_PREFIX = "gui.colossal_reactors.resource_port.eject.";

    private final int id;

    PortFilter(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    /** Display name for extract mode (waste / liquid output). */
    public Component getDisplayName() {
        return Component.translatable(LANG_OUTPUT_PREFIX + getOutputLangSuffix());
    }

    /** Display name depending on port mode: EJECT uses eject.* keys, EXTRACT uses output.* keys. */
    public Component getDisplayName(PortMode mode) {
        String key = (mode == PortMode.EJECT) ? LANG_EJECT_PREFIX + getEjectLangSuffix() : LANG_OUTPUT_PREFIX + getOutputLangSuffix();
        return Component.translatable(key);
    }

    /** Tooltip key for extract mode. */
    public String getTooltipKey() {
        return LANG_OUTPUT_PREFIX + getOutputLangSuffix() + ".tooltip";
    }

    /** Tooltip key depending on port mode. */
    public String getTooltipKey(PortMode mode) {
        String suffix = (mode == PortMode.EJECT) ? getEjectLangSuffix() : getOutputLangSuffix();
        String prefix = (mode == PortMode.EJECT) ? LANG_EJECT_PREFIX : LANG_OUTPUT_PREFIX;
        return prefix + suffix + ".tooltip";
    }

    private String getOutputLangSuffix() {
        return switch (this) {
            case BOTH -> "both";
            case ONLY_SOLID_FUEL -> "waste_only";
            case ONLY_COOLANT_LIQUID -> "liquid_only";
        };
    }

    private String getEjectLangSuffix() {
        return switch (this) {
            case BOTH -> "both";
            case ONLY_SOLID_FUEL -> "fuel_only";
            case ONLY_COOLANT_LIQUID -> "coolant_only";
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
