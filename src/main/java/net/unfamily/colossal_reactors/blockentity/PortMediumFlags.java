package net.unfamily.colossal_reactors.blockentity;

/**
 * Per-port medium toggles (row 2 of resource port GUI). Liquid and gas are mutually exclusive.
 */
public final class PortMediumFlags {

    private boolean allowSolid = true;
    private boolean allowLiquid = true;
    private boolean allowGas;

    public PortMediumFlags() {}

    public PortMediumFlags(boolean allowSolid, boolean allowLiquid, boolean allowGas) {
        this.allowSolid = allowSolid;
        this.allowLiquid = allowLiquid;
        this.allowGas = allowGas;
        enforceLiquidXorGas();
    }

    public static PortMediumFlags fromLegacyFilter(PortFilter filter) {
        return switch (filter) {
            case BOTH -> new PortMediumFlags(true, true, false);
            case ONLY_SOLID_FUEL -> new PortMediumFlags(true, false, false);
            case ONLY_COOLANT_LIQUID -> new PortMediumFlags(false, true, false);
        };
    }

    public PortFilter toLegacyFilter() {
        if (allowSolid && allowLiquid && !allowGas) return PortFilter.BOTH;
        if (allowSolid && !allowLiquid && !allowGas) return PortFilter.ONLY_SOLID_FUEL;
        if (!allowSolid && allowLiquid && !allowGas) return PortFilter.ONLY_COOLANT_LIQUID;
        if (allowSolid && !allowLiquid && allowGas) return PortFilter.ONLY_SOLID_FUEL;
        if (!allowSolid && !allowLiquid && allowGas) return PortFilter.ONLY_COOLANT_LIQUID;
        return PortFilter.BOTH;
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
        if (allowLiquid && allowGas) {
            allowGas = false;
        }
    }

    public void setAllowGas(boolean allowGas) {
        this.allowGas = allowGas;
        if (allowGas && allowLiquid) {
            allowLiquid = false;
        }
    }

    public void enforceLiquidXorGas() {
        if (allowLiquid && allowGas) {
            allowGas = false;
        }
    }

    public void writeToNbt(net.minecraft.nbt.CompoundTag tag) {
        tag.putBoolean("PortAllowSolid", allowSolid);
        tag.putBoolean("PortAllowLiquid", allowLiquid);
        tag.putBoolean("PortAllowGas", allowGas);
    }

    public void readFromNbt(net.minecraft.nbt.CompoundTag tag) {
        if (tag.contains("PortAllowSolid")) {
            allowSolid = tag.getBoolean("PortAllowSolid");
            allowLiquid = tag.getBoolean("PortAllowLiquid");
            allowGas = tag.getBoolean("PortAllowGas");
            enforceLiquidXorGas();
        }
    }
}
