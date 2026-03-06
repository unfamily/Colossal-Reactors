package net.unfamily.colossal_reactors.heatingcoil;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Definition of a heating coil from datapack (load/*.json).
 * id: registry id for this coil (used for block names id_off, id_on).
 * duration: ticks the coil stays ON; every duration ticks it consumes substain.
 * consume: list of alternative "options"; satisfying any one option (all its requirements) activates the coil.
 * no_item, no_fluid, no_energy: when true, this coil does not accept that type (pipes/cables cannot connect).
 */
public record HeatingCoilDefinition(
        ResourceLocation id,
        int duration,
        List<ConsumeOption> consume,
        boolean noItem,
        boolean noFluid,
        boolean noEnergy
) {
    public HeatingCoilDefinition(ResourceLocation id, int duration, List<ConsumeOption> consume) {
        this(id, duration, consume, false, false, false);
    }
}
