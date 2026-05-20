package net.unfamily.colossal_reactors.block;

import net.minecraft.util.StringRepresentable;

/** Visual-only controller states for turbine block models (no gameplay logic yet). */
public enum TurbineVisualState implements StringRepresentable {
    OFF("off"),
    VALIDATING("validating"),
    ON("on");

    private final String name;

    TurbineVisualState(String name) {
        this.name = name;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
