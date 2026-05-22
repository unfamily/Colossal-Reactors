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

    /**
     * Lateral connector arms on {@code turbine_rod.json} extend into the adjacent block by this much
     * (see {@code north_con} / {@code east_con} {@code to} coords 4.15). Ring-1 blades render in the
     * next block; shift them outward so the blade mesh does not overlap the connector volume.
     */
    private static final float ROD_CONNECTOR_DEPTH_INTO_NEIGHBOR = 4.15f / 16f;

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
            applyBladeClearRodConnectorOffset(poseStack, bladeState, bladePos, rodPos);
            poseStack.translate(-0.5, -0.5, -0.5);
            blockRenderer.render(bladeState, poseStack, bladePos);
            poseStack.popPose();
        }

        poseStack.translate(-0.5, -0.5, -0.5);
        TurbineRodRenderScope.run(level, rodPos, rodState, () -> blockRenderer.render(rodState, poseStack, rodPos));
        poseStack.popPose();
    }

    /**
     * Ring-1 blades sit in the block next to the rod; push the model away from the rod so it stays
     * in that block and clears the connector geometry protruding from the full 16³ rod cube.
     */
    private static void applyBladeClearRodConnectorOffset(
            PoseStack poseStack, BlockState bladeState, BlockPos bladePos, BlockPos rodPos) {
        if (!bladeState.hasProperty(TurbineBladeBlock.FACING)) {
            return;
        }
        if (ringDistanceFromRod(bladePos, rodPos) != 1) {
            return;
        }
        Direction awayFromRod = bladeState.getValue(TurbineBladeBlock.FACING);
        float d = ROD_CONNECTOR_DEPTH_INTO_NEIGHBOR;
        poseStack.translate(
                awayFromRod.getStepX() * d,
                awayFromRod.getStepY() * d,
                awayFromRod.getStepZ() * d);
    }

    private static int ringDistanceFromRod(BlockPos bladePos, BlockPos rodPos) {
        int dx = Math.abs(bladePos.getX() - rodPos.getX());
        int dy = Math.abs(bladePos.getY() - rodPos.getY());
        int dz = Math.abs(bladePos.getZ() - rodPos.getZ());
        return Math.max(dx, Math.max(dy, dz));
    }
}
