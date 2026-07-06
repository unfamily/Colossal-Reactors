package net.unfamily.colossal_reactors.heatingcoil;

import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;
import java.util.List;

/**
 * One alternative way to feed the coil. Can have fluid, item, energy, burnable (any combination);
 * all present requirements must be satisfied for this option to count as "activated".
 * activation = total needed to turn ON; substain = consumed every duration ticks while ON.
 */
public record ConsumeOption(
        @Nullable FluidRequirement fluid,
        @Nullable ChemicalRequirement chemical,
        @Nullable ItemRequirement item,
        @Nullable EnergyRequirement energy,
        @Nullable BurnableRequirement burnable
) {
    public boolean isEmpty() {
        return fluid == null && chemical == null && item == null && energy == null && burnable == null;
    }

    /** tagOrId: fluid id or tag (when isTag true, e.g. #c:water). */
    public record FluidRequirement(Identifier tagOrId, boolean isTag, int activation, int substain) {}

    /** selector: Mek chemical id with {@code %} prefix (e.g. {@code %mekanism:steam}). */
    public record ChemicalRequirement(String selector, int activation, int substain) {}
    /** tagOrId: item id or tag (when isTag true, e.g. #c:ingots/uranium). */
    public record ItemRequirement(Identifier tagOrId, boolean isTag, int activation, int substain) {}
    public record EnergyRequirement(int activation, int substain) {}
    /** activation/substain in burn-time ticks (e.g. from furnace fuel). */
    public record BurnableRequirement(int activation, int substain) {}
}
