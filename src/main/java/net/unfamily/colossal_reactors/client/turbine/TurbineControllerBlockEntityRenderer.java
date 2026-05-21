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
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineControllerBlock;
import net.unfamily.colossal_reactors.block.TurbineVisualState;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;

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

        TurbineRotorAnimationManager.pollController(be, partialTick);
        TurbineRotorAnimationManager.RotorState state = TurbineRotorAnimationManager.getState(be.getBlockPos());

        float angleDegrees;
        long[] rods;
        byte[] facings;

        if (state != null && state.isAssemblyReady()) {
            angleDegrees = state.getAngleDegrees(partialTick);
            rods = state.rodPositions();
            facings = rodFacingsFromState(state, rods.length);
        } else if (canRenderAssemblyFromBlockEntity(level, be)) {
            var source = TurbineRotorSimulationSource.forRendering(be);
            angleDegrees = 0f;
            TurbineValidation.Result result = source.getCachedResult();
            rods = source.getCachedRodPositions();
            facings = source.getCachedRodFacings();
            if (rods.length == 0 && result.valid()) {
                rods = scanRods(level, result);
                facings = scanFacings(level, rods);
            }
        } else {
            return;
        }

        if (rods.length == 0) {
            return;
        }

        BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
        BlockPos controllerPos = be.getBlockPos();
        float angleRad = (float) Math.toRadians(angleDegrees);

        for (int i = 0; i < rods.length; i++) {
            BlockPos rodPos = BlockPos.of(rods[i]);
            Direction axis = facings.length > i
                    ? Direction.from3DDataValue(facings[i] & 0xFF)
                    : level.getBlockState(rodPos).getValue(net.unfamily.colossal_reactors.block.TurbineRodBlock.FACING);
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

    private static boolean canRenderAssemblyFromBlockEntity(Level level, TurbineControllerBlockEntity be) {
        BlockState ctrl = level.getBlockState(be.getBlockPos());
        TurbineControllerBlockEntity source = TurbineRotorSimulationSource.forRendering(be);
        return ctrl.is(ModBlocks.TURBINE_CONTROLLER.get())
                && ctrl.getValue(TurbineControllerBlock.VISUAL) == TurbineVisualState.ON
                && source.getCachedResult().valid();
    }

    private static byte[] rodFacingsFromState(TurbineRotorAnimationManager.RotorState state, int rodCount) {
        byte[] facings = new byte[rodCount];
        for (int i = 0; i < rodCount; i++) {
            facings[i] = (byte) state.rodFacing(i).ordinal();
        }
        return facings;
    }

    private static long[] scanRods(Level level, TurbineValidation.Result result) {
        it.unimi.dsi.fastutil.longs.LongArrayList rods = new it.unimi.dsi.fastutil.longs.LongArrayList();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = result.minX(); x <= result.maxX(); x++) {
            for (int y = result.minY(); y <= result.maxY(); y++) {
                for (int z = result.minZ(); z <= result.maxZ(); z++) {
                    p.set(x, y, z);
                    if (level.getBlockState(p).is(ModBlocks.TURBINE_ROD.get())) {
                        rods.add(p.asLong());
                    }
                }
            }
        }
        return rods.toLongArray();
    }

    private static byte[] scanFacings(Level level, long[] rods) {
        byte[] facings = new byte[rods.length];
        for (int i = 0; i < rods.length; i++) {
            BlockState state = level.getBlockState(BlockPos.of(rods[i]));
            facings[i] = (byte) state.getValue(net.unfamily.colossal_reactors.block.TurbineRodBlock.FACING).ordinal();
        }
        return facings;
    }

    @Override
    public AABB getRenderBoundingBox(TurbineControllerBlockEntity be) {
        TurbineValidation.Result result = TurbineRotorSimulationSource.forRendering(be).getCachedResult();
        if (result.valid()) {
            return new AABB(
                    result.minX(), result.minY(), result.minZ(),
                    result.maxX() + 1.0, result.maxY() + 1.0, result.maxZ() + 1.0);
        }
        BlockPos p = be.getBlockPos();
        return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1.0, p.getY() + 1.0, p.getZ() + 1.0);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(TurbineControllerBlockEntity be) {
        if (!ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get()) {
            return false;
        }
        Level level = be.getLevel();
        if (level == null) {
            return false;
        }
        BlockPos pos = be.getBlockPos();
        if (TurbineRotorAnimationManager.shouldRenderAssembly(pos)) {
            return true;
        }
        return canRenderAssemblyFromBlockEntity(level, be);
    }
}
