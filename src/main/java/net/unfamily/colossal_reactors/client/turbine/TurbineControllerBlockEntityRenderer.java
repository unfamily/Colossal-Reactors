package net.unfamily.colossal_reactors.client.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.unfamily.colossal_reactors.ClientConfig;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;

/**
 * Renders spinning turbine rods and blades from the controller block entity.
 */
public class TurbineControllerBlockEntityRenderer implements BlockEntityRenderer<TurbineControllerBlockEntity> {

    public TurbineControllerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            TurbineControllerBlockEntity be,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            return;
        }
        Level level = be.getLevel();
        if (level == null) {
            return;
        }

        TurbineRotorClientRegistry.ensureAssemblyState(be);

        if (!TurbineRotorClientRegistry.shouldRunBer(be)) {
            return;
        }

        TurbineRotorAnimationManager.pollController(be, partialTick);

        TurbineRotorAnimationManager.RotorState state = TurbineRotorAnimationManager.getState(be.getBlockPos());
        if (state == null) {
            return;
        }

        float angleDegrees = state.getAngleDegrees(partialTick);
        long[] rods = state.rodPositions();

        if (rods.length == 0) {
            return;
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockPos controllerPos = be.getBlockPos();
        float angleRad = (float) Math.toRadians(angleDegrees);

        for (int i = 0; i < rods.length; i++) {
            BlockPos rodPos = BlockPos.of(rods[i]);
            Direction axis = state.rodFacing(i);
            TurbineRotorRenderHelper.renderRodAssembly(
                    level,
                    controllerPos,
                    rodPos,
                    axis,
                    angleRad,
                    poseStack,
                    (blockState, stack, lightAt) -> {
                        int light = net.minecraft.client.renderer.LevelRenderer.getLightColor(level, lightAt);
                        dispatcher.renderSingleBlock(blockState, stack, bufferSource, light, packedOverlay);
                    });
        }
    }

    @Override
    public AABB getRenderBoundingBox(TurbineControllerBlockEntity be) {
        AABB bounds = TurbineRotorClientRegistry.getRenderBounds(be);
        return bounds != null ? bounds : new AABB(be.getBlockPos());
    }

    @Override
    public int getViewDistance() {
        return ClientConfig.getTurbineRotorRenderDistanceBlocks();
    }

    @Override
    public boolean shouldRenderOffScreen(TurbineControllerBlockEntity be) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            return false;
        }
        return ClientConfig.TURBINE_ROTOR_RENDER_OFFSCREEN.get();
    }
}
