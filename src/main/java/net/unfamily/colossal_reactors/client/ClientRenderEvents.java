package net.unfamily.colossal_reactors.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.unfamily.colossal_reactors.ColossalReactors;

@EventBusSubscriber(modid = ColossalReactors.MODID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientRenderEvents {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            PoseStack poseStack = event.getPoseStack();
            PreviewMarkRenderer.getInstance().render(poseStack, 0.0f);
        }
    }
}
