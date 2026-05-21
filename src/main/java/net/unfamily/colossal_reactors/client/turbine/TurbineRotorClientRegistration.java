package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.ModBlockEntities;

@EventBusSubscriber(modid = ColossalReactors.MODID)
public final class TurbineRotorClientRegistration {

    private TurbineRotorClientRegistration() {}

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
                ModBlockEntities.TURBINE_CONTROLLER_BE.get(),
                TurbineControllerBlockEntityRenderer::new);
    }

    @SubscribeEvent
    public static void onModifyBakingResult(ModelEvent.ModifyBakingResult event) {
        var blockModels = event.getBakingResult().blockStateModels();
        for (BlockState state : blockModels.keySet().toArray(new BlockState[0])) {
            BlockStateModel base = blockModels.get(state);
            if (state.is(ModBlocks.TURBINE_ROD.get())) {
                base = new TurbineRodConnectorBlockStateModel(base);
            }
            if (state.is(ModBlocks.TURBINE_ROD.get()) || state.is(ModBlocks.TURBINE_BLADE.get())) {
                blockModels.put(state, new TurbineRodBladeHidingBlockStateModel(base));
            }
        }
    }
}
