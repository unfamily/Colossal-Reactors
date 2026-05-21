package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;

import java.util.ArrayList;
import java.util.Map;

public final class TurbineRotorClientRegistration {

    private TurbineRotorClientRegistration() {}

    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.TURBINE_CONTROLLER_BE.get(),
                TurbineControllerBlockEntityRenderer::new);
    }

    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        Map<ModelResourceLocation, BakedModel> models = event.getModels();
        wrapRodBladeModels(models);
    }

    public static void registerRenderLayers() {
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.TURBINE_ROD.get(), RenderType.cutout());
        ItemBlockRenderTypes.setRenderLayer(ModBlocks.TURBINE_BLADE.get(), RenderType.cutout());
    }

    /**
     * Wraps every baked model used by turbine rod/blade blockstates.
     * Path-only matching ({@code turbine/turbine_rod}) misses variant keys like {@code block/turbine_rod#facing=north}.
     */
    private static void wrapRodBladeModels(Map<ModelResourceLocation, BakedModel> models) {
        for (BlockState state : ModBlocks.TURBINE_ROD.get().getStateDefinition().getPossibleStates()) {
            wrapModel(models, BlockModelShaper.stateToModelLocation(state));
        }
        for (BlockState state : ModBlocks.TURBINE_BLADE.get().getStateDefinition().getPossibleStates()) {
            wrapModel(models, BlockModelShaper.stateToModelLocation(state));
        }
        for (var entry : new ArrayList<>(models.entrySet())) {
            ResourceLocation id = entry.getKey().id();
            if (!ColossalReactors.MODID.equals(id.getNamespace())) {
                continue;
            }
            String path = id.getPath();
            if ((path.contains("turbine_rod") || path.contains("turbine_blade"))
                    && !(entry.getValue() instanceof TurbineRodBladeBakedModel)) {
                models.put(entry.getKey(), new TurbineRodBladeBakedModel(entry.getValue()));
            }
        }
    }

    private static void wrapModel(Map<ModelResourceLocation, BakedModel> models, ModelResourceLocation location) {
        BakedModel model = models.get(location);
        if (model != null && !(model instanceof TurbineRodBladeBakedModel)) {
            models.put(location, new TurbineRodBladeBakedModel(model));
        }
    }
}
