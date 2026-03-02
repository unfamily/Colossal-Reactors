package net.unfamily.colossal_reactors.data;

import com.supermartijn642.fusion.api.model.DefaultModelTypes;
import com.supermartijn642.fusion.api.model.ModelInstance;
import com.supermartijn642.fusion.api.model.data.ConnectingModelDataBuilder;
import com.supermartijn642.fusion.api.predicate.DefaultConnectionPredicates;
import com.supermartijn642.fusion.api.provider.FusionModelProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * Registers Fusion connecting models for reactor_glass and reactor_casing (same pattern as Connected Glass / Rechiseled).
 * Run the "data" run config to generate. Fusion will then use these models at runtime.
 */
public class ColossalReactorsFusionModelProvider extends FusionModelProvider {

    public ColossalReactorsFusionModelProvider(PackOutput output) {
        super(ColossalReactors.MODID, output);
    }

    @Override
    public void generate() {
        // Reactor glass - cube_all, connecting to same block (like Connected Glass clear_glass)
        this.addModel(
                ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "block/reactor_glass"),
                ModelInstance.of(
                        DefaultModelTypes.CONNECTING,
                        ConnectingModelDataBuilder.builder()
                                .parent(ResourceLocation.withDefaultNamespace("block/cube_all"))
                                .texture("all", ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "block/reactor_glass"))
                                .connections("all", DefaultConnectionPredicates.isSameBlock())
                                .build()
                )
        );

        // Reactor casing - cube_all, connecting to same block
        this.addModel(
                ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "block/reactor_casing"),
                ModelInstance.of(
                        DefaultModelTypes.CONNECTING,
                        ConnectingModelDataBuilder.builder()
                                .parent(ResourceLocation.withDefaultNamespace("block/cube_all"))
                                .texture("all", ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "block/reactor_casing"))
                                .connections("all", DefaultConnectionPredicates.isSameBlock())
                                .build()
                )
        );
    }
}
