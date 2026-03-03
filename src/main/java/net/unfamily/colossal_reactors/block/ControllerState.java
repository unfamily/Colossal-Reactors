package net.unfamily.colossal_reactors.block;

import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

/** Screen state: OFF (invalid, player must click), VALIDATING (boot texture), ON (valid). */
public enum ControllerState implements StringRepresentable {
    OFF("off"),
    VALIDATING("validating"),
    ON("on");

    private final String name;

    ControllerState(String name) {
        this.name = name;
    }

    @Override
    @NotNull
    public String getSerializedName() {
        return name;
    }
}
