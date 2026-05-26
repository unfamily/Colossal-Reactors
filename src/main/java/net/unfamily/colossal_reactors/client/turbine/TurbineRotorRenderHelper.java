package net.unfamily.colossal_reactors.client.turbine;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.turbine.TurbineBladePlacement;
import org.joml.Quaternionf;

import java.util.List;

/**
 * Shared rotor assembly transforms. BER pose stack starts at the controller block origin (min corner).
 *
 * <p><b>DO NOT change blade placement in this class.</b> Rod and blade models are authored for a strict
 * 16×16×16 block grid:
 * <ul>
 *   <li>The rod is one full block; lateral connectors are already inside {@code turbine_rod.json}.</li>
 *   <li>Ring-1 blades live in the block <em>adjacent</em> to the rod (see {@link TurbineBladePlacement}).</li>
 *   <li>Further rings are the next blocks along the lateral axis — one block per ring.</li>
 * </ul>
 * Rendering must use those {@link BlockPos} values only (plus the standard {@code -0.5} pivot to block origin).
 * Do not add sub-block offsets to “align” hub/connector geometry; fix the block models instead.
 */
public final class TurbineRotorRenderHelper {

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
        // Rod rotation pivot: center of the rod block (16³).
        poseStack.translate(
                rodPos.getX() - controllerPos.getX() + 0.5,
                rodPos.getY() - controllerPos.getY() + 0.5,
                rodPos.getZ() - controllerPos.getZ() + 0.5);
        poseStack.mulPose(new Quaternionf().rotateAxis(
                angleRad, rodAxis.getStepX(), rodAxis.getStepY(), rodAxis.getStepZ()));

        // Blades: integer block offsets from rod center only — DO NOT add per-ring sub-block nudges here.
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
            poseStack.translate(-0.5, -0.5, -0.5);
            blockRenderer.render(bladeState, poseStack, bladePos);
            poseStack.popPose();
        }

        poseStack.translate(-0.5, -0.5, -0.5);
        TurbineRodRenderScope.run(level, rodPos, rodState, () -> blockRenderer.render(rodState, poseStack, rodPos));
        poseStack.popPose();
    }
}
