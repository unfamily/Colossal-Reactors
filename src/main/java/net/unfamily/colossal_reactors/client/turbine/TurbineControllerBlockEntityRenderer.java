package net.unfamily.colossal_reactors.client.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.unfamily.colossal_reactors.ClientConfig;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders spinning turbine rods and blades from the controller block entity (MC 26 render pipeline).
 */
public class TurbineControllerBlockEntityRenderer
        implements BlockEntityRenderer<TurbineControllerBlockEntity, TurbineControllerBlockEntityRenderer.RotorRenderState> {

    private static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();

    private final BlockModelResolver blockModelResolver;
    private final BlockModelRenderState blockModelRenderState = new BlockModelRenderState();

    public TurbineControllerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        this.blockModelResolver = context.blockModelResolver();
    }

    public static final class RotorRenderState extends BlockEntityRenderState {
        boolean renderAssembly;
        float angleDegrees;
        BlockPos controllerPos = BlockPos.ZERO;
        final List<RodEntry> rods = new ArrayList<>();
    }

    public record RodEntry(BlockPos pos, Direction axis) {}

    @Override
    public RotorRenderState createRenderState() {
        return new RotorRenderState();
    }

    @Override
    public void extractRenderState(
            TurbineControllerBlockEntity be,
            RotorRenderState state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.CrumblingOverlay breakProgress) {
        BlockEntityRenderState.extractBase(be, state, breakProgress);
        state.rods.clear();
        state.controllerPos = be.getBlockPos();
        TurbineRotorClientRegistry.ensureAssemblyState(be);
        TurbineRotorClientRegistry.ensureGloballyRendered(be);
        if (!TurbineRotorClientRegistry.shouldRunBer(be)) {
            state.renderAssembly = false;
            return;
        }
        TurbineRotorAnimationManager.pollController(be, partialTicks);
        TurbineRotorAnimationManager.RotorState anim = TurbineRotorAnimationManager.getState(be.getBlockPos());
        if (anim == null) {
            state.renderAssembly = false;
            return;
        }
        state.renderAssembly = true;
        state.angleDegrees = anim.getAngleDegrees(partialTicks);
        long[] rodPositions = anim.rodPositions();
        for (int i = 0; i < rodPositions.length; i++) {
            state.rods.add(new RodEntry(BlockPos.of(rodPositions[i]), anim.rodFacing(i)));
        }
    }

    @Override
    public void submit(
            RotorRenderState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get() || !state.renderAssembly) {
            return;
        }
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }

        float angleRad = (float) Math.toRadians(state.angleDegrees);
        BlockPos ctrl = state.controllerPos;

        for (RodEntry rod : state.rods) {
            TurbineRotorRenderHelper.renderRodAssembly(
                    level,
                    ctrl,
                    rod.pos(),
                    rod.axis(),
                    angleRad,
                    poseStack,
                    (blockState, stack, lightAt) -> submitBlockModel(
                            blockState, stack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY));
        }
    }

    private void submitBlockModel(
            BlockState state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            int light,
            int overlay) {
        blockModelResolver.update(blockModelRenderState, state, BLOCK_DISPLAY_CONTEXT);
        if (blockModelRenderState.isEmpty()) {
            return;
        }
        // Transforms live on poseStack (see TurbineRotorRenderHelper); update() already filled modelParts.
        blockModelRenderState.submit(poseStack, submitNodeCollector, light, overlay, 0);
    }

    @Override
    public AABB getRenderBoundingBox(TurbineControllerBlockEntity be) {
        AABB bounds = TurbineRotorClientRegistry.getRenderBounds(be);
        return bounds != null ? bounds : BlockEntityRenderer.super.getRenderBoundingBox(be);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()
                && ClientConfig.TURBINE_ROTOR_RENDER_OFFSCREEN.get();
    }

    @Override
    public int getViewDistance() {
        return ClientConfig.getTurbineRotorRenderDistanceBlocks();
    }

}
