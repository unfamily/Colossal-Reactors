package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.nbt.CompoundTag;

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

    public void writeToNbt(CompoundTag tag) {
        tag.putInt(KEY_MEDIUM, medium.getId());
    }

    public void readFromNbt(CompoundTag tag) {
        if (tag.contains(KEY_MEDIUM)) {
            medium = PortMedium.fromId(tag.getInt(KEY_MEDIUM));
            return;
        }
        if (tag.contains(KEY_SOLID)) {
            boolean allowSolid = tag.getBoolean(KEY_SOLID);
            boolean allowLiquid = tag.getBoolean(KEY_LIQUID);
            boolean allowGas = tag.getBoolean(KEY_GAS);
            medium = PortMedium.fromLegacyBooleans(allowSolid, allowLiquid, allowGas);
        }
    }
}
