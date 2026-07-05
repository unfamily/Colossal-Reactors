package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Exclusive port medium persisted on the block entity. */
public final class PortMediumFlags {

    public static final String KEY_MEDIUM = "PortMedium";
    /** Legacy keys — read only. */
    public static final String KEY_SOLID = "PortAllowSolid";
    public static final String KEY_LIQUID = "PortAllowLiquid";
    public static final String KEY_GAS = "PortAllowGas";

    private PortMedium medium = PortMedium.SOLID;

    public PortMedium getMedium() {
        return medium;
    }

    public void setMedium(PortMedium medium) {
        this.medium = medium != null ? medium : PortMedium.SOLID;
    }

    public void cycleReactor() {
        cycleReactor(true);
    }

    public void cycleReactor(boolean gasAvailable) {
        medium = medium.nextForReactor(gasAvailable);
    }

    public void cycleTurbine() {
        cycleTurbine(true);
    }

    public void cycleTurbine(boolean gasAvailable) {
        medium = medium.nextForTurbine(gasAvailable);
    }

    public void cycleReactorBack() {
        cycleReactorBack(true);
    }

    public void cycleReactorBack(boolean gasAvailable) {
        medium = medium.prevForReactor(gasAvailable);
    }

    public void cycleTurbineBack() {
        cycleTurbineBack(true);
    }

    public void cycleTurbineBack(boolean gasAvailable) {
        medium = medium.prevForTurbine(gasAvailable);
    }

    public boolean isAllowSolid() {
        return medium.isSolid();
    }

    public boolean isAllowLiquid() {
        return medium.isLiquid();
    }

    public boolean isAllowGas() {
        return medium.isGas();
    }

    public void write(ValueOutput output) {
        output.putInt(KEY_MEDIUM, medium.getId());
    }

    public void read(ValueInput input) {
        if (input.getInt(KEY_MEDIUM).isPresent()) {
            medium = PortMedium.fromId(input.getIntOr(KEY_MEDIUM, PortMedium.SOLID.getId()));
            return;
        }
        boolean allowSolid = input.getBooleanOr(KEY_SOLID, true);
        boolean allowLiquid = input.getBooleanOr(KEY_LIQUID, true);
        boolean allowGas = input.getBooleanOr(KEY_GAS, false);
        medium = PortMedium.fromLegacyBooleans(allowSolid, allowLiquid, allowGas);
    }
}
