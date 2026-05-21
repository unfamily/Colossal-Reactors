package net.unfamily.colossal_reactors.client.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineBladeBlock;
import net.unfamily.colossal_reactors.turbine.TurbineBladePlacement;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Shared rotor assembly transforms. BER pose stack starts at the controller block origin (min corner).
 */
public final class TurbineRotorRenderHelper {

    /** Extra toward-rod nudge (model hub is already at z=14); 2/16 attaches to connector without sinking inside. */
    private static final float BLADE_HUB_TOWARD_CONNECTOR = 2f / 16f;

    @FunctionalInterface
    public interface BlockRenderCallback {
        void render(BlockState state, PoseStack poseStack, BlockPos lightAt);
    }

    private TurbineRotorRenderHelper() {}

    public static void renderRodAssembly(
            Level level,
            BlockPos controllerPos,
            BlockPos rodPos,
            Direction rodAxis,
            float angleRad,
            PoseStack poseStack,
            BlockRenderCallback blockRenderer) {
        BlockState rodState = level.getBlockState(rodPos);
        if (!rodState.is(ModBlocks.TURBINE_ROD.get())) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(
                rodPos.getX() - controllerPos.getX() + 0.5,
                rodPos.getY() - controllerPos.getY() + 0.5,
                rodPos.getZ() - controllerPos.getZ() + 0.5);
        poseStack.mulPose(new Quaternionf().rotateAxis(
                angleRad, rodAxis.getStepX(), rodAxis.getStepY(), rodAxis.getStepZ()));

        List<BlockPos> blades = TurbineBladePlacement.collectBladePositions(level, rodPos, rodAxis);
        for (BlockPos bladePos : blades) {
            BlockState bladeState = level.getBlockState(bladePos);
            if (!bladeState.is(ModBlocks.TURBINE_BLADE.get())) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(
                    bladePos.getX() - rodPos.getX(),
                    bladePos.getY() - rodPos.getY(),
                    bladePos.getZ() - rodPos.getZ());
            applyBladeHubOffset(poseStack, bladeState);
            poseStack.translate(-0.5, -0.5, -0.5);
            blockRenderer.render(bladeState, poseStack, bladePos);
            poseStack.popPose();
        }

        poseStack.translate(-0.5, -0.5, -0.5);
        TurbineRodRenderScope.run(level, rodPos, rodState, () -> blockRenderer.render(rodState, poseStack, rodPos));
        poseStack.popPose();
    }

    private static void applyBladeHubOffset(PoseStack poseStack, BlockState bladeState) {
        if (!bladeState.hasProperty(TurbineBladeBlock.FACING)) {
            return;
        }
        Direction towardRod = bladeState.getValue(TurbineBladeBlock.FACING).getOpposite();
        poseStack.translate(
                towardRod.getStepX() * BLADE_HUB_TOWARD_CONNECTOR,
                towardRod.getStepY() * BLADE_HUB_TOWARD_CONNECTOR,
                towardRod.getStepZ() * BLADE_HUB_TOWARD_CONNECTOR);
    }
}
