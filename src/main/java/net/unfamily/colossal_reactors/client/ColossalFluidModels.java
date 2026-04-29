package net.unfamily.colossal_reactors.client;

import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSources;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.fluid.ModFluids;
import net.unfamily.colossal_reactors.fluid.ModFluids.FluidColors;

/**
 * NeoForge 26: custom fluids need {@link FluidModel.Unbaked} registration for world/JEI/GUI rendering.
 * Molten-style fluids use {@code assets/colossal_reactors/textures/block/fluid/still|flow.png}; gelid breezium uses vanilla water sprites.
 * Tints match legacy {@link FluidColors} (old {@code IClientFluidTypeExtensions#getTintColor}).
 */
public final class ColossalFluidModels {

    /** {@code textures/block/fluid/still.png} */
    private static final Material MOLTEN_STILL = new Material(Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "block/fluid/still"));
    /** {@code textures/block/fluid/flow.png} */
    private static final Material MOLTEN_FLOW = new Material(Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "block/fluid/flow"));
    private static final Material WATER_STILL = new Material(Identifier.withDefaultNamespace("block/water_still"));
    private static final Material WATER_FLOW = new Material(Identifier.withDefaultNamespace("block/water_flow"));

    private ColossalFluidModels() {}

    public static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(
                new FluidModel.Unbaked(MOLTEN_STILL, MOLTEN_FLOW, null, FluidTintSources.constant(FluidColors.MOLTEN_TOUGH_ALLOY)),
                ModFluids.MOLTEN_TOUGH_ALLOY::getSource,
                ModFluids.MOLTEN_TOUGH_ALLOY::getFlowing);

        event.register(
                new FluidModel.Unbaked(WATER_STILL, WATER_FLOW, null, FluidTintSources.constant(FluidColors.GELID_BREEZIUM)),
                ModFluids.GELID_BREEZIUM::getSource,
                ModFluids.GELID_BREEZIUM::getFlowing);

        event.register(
                new FluidModel.Unbaked(MOLTEN_STILL, MOLTEN_FLOW, null, FluidTintSources.constant(FluidColors.ENDER_GOO)),
                ModFluids.ENDER_GOO::getSource,
                ModFluids.ENDER_GOO::getFlowing);
    }
}
