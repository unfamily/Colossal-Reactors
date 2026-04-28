package net.unfamily.colossal_reactors.heatingcoil;

import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Definition of a heating coil from datapack (load/*.json).
 * id: registry id for this coil (used for block names id_off, id_on).
 * duration: ticks the coil stays ON; every duration ticks it consumes substain.
 * consume: list of alternative "options"; satisfying any one option (all its requirements) activates the coil.
 * no_item, no_fluid, no_energy: when true, this coil does not accept that type (pipes/cables cannot connect).
 * all_sides: when true, accepts inputs from any side; otherwise only the front face.
 */
public record HeatingCoilDefinition(
        Identifier id,
        int duration,
        List<ConsumeOption> consume,
        boolean allSides,
        boolean noItem,
        boolean noFluid,
        boolean noEnergy
) {
    public HeatingCoilDefinition(Identifier id, int duration, List<ConsumeOption> consume) {
        this(id, duration, consume, false, false, false, false);
    }
}
