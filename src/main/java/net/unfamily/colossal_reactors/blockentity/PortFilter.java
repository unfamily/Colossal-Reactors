package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.network.chat.Component;

/**
 * What the port outputs: in EXTRACT mode = waste + liquid; in EJECT mode = fuel + coolant (input back out).
 */
public enum PortFilter {
    BOTH(0),
    ONLY_SOLID_FUEL(1),
    ONLY_COOLANT_LIQUID(2);

    private static final String LANG_INSERT_PREFIX = "gui.colossal_reactors.resource_port.insert.";
    private static final String LANG_OUTPUT_PREFIX = "gui.colossal_reactors.resource_port.output.";
    private static final String LANG_EJECT_PREFIX = "gui.colossal_reactors.resource_port.eject.";
    private static final String LANG_FILTER_BTN_PREFIX = "gui.colossal_reactors.resource_port.filter_btn.";

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

    /** Fuel channel: solid fuel insert/eject and solid waste extract. */
    public boolean acceptsFuelRole() {
        return this == BOTH || this == ONLY_SOLID_FUEL;
    }

    /** Coolant channel: liquid coolant insert/eject and liquid/gas exhaust extract. */
    public boolean acceptsCoolantRole() {
        return this == BOTH || this == ONLY_COOLANT_LIQUID;
    }

    /** Short label for the Fuel/Coolant role button (depends on port mode). */
    public Component getFilterButtonLabel(PortMode mode) {
        if (mode == PortMode.EXTRACT) {
            return switch (this) {
                case ONLY_COOLANT_LIQUID -> Component.translatable(LANG_FILTER_BTN_PREFIX + "coolant");
                default -> Component.translatable(LANG_FILTER_BTN_PREFIX + "waste");
            };
        }
        return switch (this) {
            case ONLY_COOLANT_LIQUID -> Component.translatable(LANG_FILTER_BTN_PREFIX + "coolant");
            default -> Component.translatable(LANG_FILTER_BTN_PREFIX + "fuel");
        };
    }

    /** Display name depending on port mode (INSERT / EXTRACT / EJECT). */
    public Component getDisplayName(PortMode mode) {
        String key = switch (mode) {
            case INSERT -> LANG_INSERT_PREFIX + getInsertLangSuffix();
            case EJECT -> LANG_EJECT_PREFIX + getEjectLangSuffix();
            default -> LANG_OUTPUT_PREFIX + getOutputLangSuffix();
        };
        return Component.translatable(key);
    }

    /** Tooltip key for extract mode. */
    public String getTooltipKey() {
        return LANG_OUTPUT_PREFIX + getOutputLangSuffix() + ".tooltip";
    }

    /** Tooltip key depending on port mode. */
    public String getTooltipKey(PortMode mode) {
        String suffix = switch (mode) {
            case INSERT -> getInsertLangSuffix();
            case EJECT -> getEjectLangSuffix();
            default -> getOutputLangSuffix();
        };
        String prefix = switch (mode) {
            case INSERT -> LANG_INSERT_PREFIX;
            case EJECT -> LANG_EJECT_PREFIX;
            default -> LANG_OUTPUT_PREFIX;
        };
        return prefix + suffix + ".tooltip";
    }

    private String getInsertLangSuffix() {
        return switch (this) {
            case BOTH -> "both";
            case ONLY_SOLID_FUEL -> "fuel_only";
            case ONLY_COOLANT_LIQUID -> "coolant_only";
        };
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
