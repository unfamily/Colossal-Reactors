package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Per-port medium toggles (row 2 of resource port GUI). Liquid and gas are mutually exclusive.
 */
public final class PortMediumFlags {

    public static final String KEY_SOLID = "PortAllowSolid";
    public static final String KEY_LIQUID = "PortAllowLiquid";
    public static final String KEY_GAS = "PortAllowGas";

    private boolean allowSolid = true;
    private boolean allowLiquid = true;
    private boolean allowGas;

    public static PortMediumFlags fromLegacyFilter(PortFilter filter) {
        return switch (filter) {
            case BOTH -> new PortMediumFlags(true, true, false);
            case ONLY_SOLID_FUEL -> new PortMediumFlags(true, false, false);
            case ONLY_COOLANT_LIQUID -> new PortMediumFlags(false, true, false);
        };
    }

    public PortMediumFlags() {}

    public PortMediumFlags(boolean allowSolid, boolean allowLiquid, boolean allowGas) {
        this.allowSolid = allowSolid;
        this.allowLiquid = allowLiquid;
        this.allowGas = allowGas;
        enforceLiquidXorGas();
    }

    public boolean isAllowSolid() {
        return allowSolid;
    }

    public boolean isAllowLiquid() {
        return allowLiquid;
    }

    public boolean isAllowGas() {
        return allowGas;
    }

    public void setAllowSolid(boolean allowSolid) {
        this.allowSolid = allowSolid;
    }

    public void setAllowLiquid(boolean allowLiquid) {
        this.allowLiquid = allowLiquid;
        if (allowLiquid && allowGas) allowGas = false;
    }

    public void setAllowGas(boolean allowGas) {
        this.allowGas = allowGas;
        if (allowGas && allowLiquid) allowLiquid = false;
    }

    public void enforceLiquidXorGas() {
        if (allowLiquid && allowGas) allowGas = false;
    }

    public PortFilter toLegacyFilter() {
        if (allowSolid && allowLiquid && !allowGas) return PortFilter.BOTH;
        if (allowSolid && !allowLiquid && !allowGas) return PortFilter.ONLY_SOLID_FUEL;
        if (!allowSolid && allowLiquid && !allowGas) return PortFilter.ONLY_COOLANT_LIQUID;
        return PortFilter.BOTH;
    }

    public void write(ValueOutput output) {
        output.putBoolean(KEY_SOLID, allowSolid);
        output.putBoolean(KEY_LIQUID, allowLiquid);
        output.putBoolean(KEY_GAS, allowGas);
    }

    public void read(ValueInput input) {
        allowSolid = input.getBooleanOr(KEY_SOLID, true);
        allowLiquid = input.getBooleanOr(KEY_LIQUID, true);
        allowGas = input.getBooleanOr(KEY_GAS, false);
        enforceLiquidXorGas();
    }
}
