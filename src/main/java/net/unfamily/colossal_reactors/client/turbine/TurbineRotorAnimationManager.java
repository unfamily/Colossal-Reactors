package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.ClientConfig;
import net.unfamily.colossal_reactors.block.TurbineVisualState;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Facade for {@link TurbineRotorClientRegistry} (legacy call sites).
 */
public final class TurbineRotorAnimationManager {

    private TurbineRotorAnimationManager() {}

    public static void pollController(TurbineControllerBlockEntity controller, float partialTick) {
        TurbineRotorClientRegistry.pollController(controller, partialTick);
    }

    public static void applySyncedRuntime(TurbineControllerBlockEntity controller) {
        TurbineRotorClientRegistry.onControllerSync(controller);
    }

    public static boolean shouldRenderRotorAssembly(TurbineControllerBlockEntity controller) {
        return TurbineRotorClientRegistry.shouldRunBer(controller);
    }

    public static float computeClientRotationFactor(TurbineControllerBlockEntity controller) {
        return TurbineRotorClientRegistry.getClientRotationFactor(controller.getBlockPos());
    }

    public static boolean isRotorAllowedToSpin(TurbineControllerBlockEntity controller) {
        return TurbineRotorClientRegistry.shouldAnimate(controller);
    }

    public static void onClientStructureChanged(Level level, BlockPos structurePos) {
        TurbineRotorClientRegistry.invalidateStructure(level, structurePos);
    }

    public static void onClientRedstoneChanged(Level level, BlockPos portPos) {
        TurbineRotorClientRegistry.onClientRedstoneChanged(level, portPos);
    }

    public static float getClientRotationFactor(BlockPos controllerPos) {
        return TurbineRotorClientRegistry.getClientRotationFactor(controllerPos);
    }

    public static void onControllerSync(TurbineControllerBlockEntity controller) {
        TurbineRotorClientRegistry.onControllerSync(controller);
    }

    public static void onControllerVisualChanged(
            TurbineControllerBlockEntity controller,
            TurbineVisualState previous,
            TurbineVisualState current) {
        TurbineRotorClientRegistry.ensureAssemblyState(controller);
    }

    public static void ensureAssemblyState(TurbineControllerBlockEntity controller) {
        TurbineRotorClientRegistry.ensureAssemblyState(controller);
    }

    public static void clientTick() {
        TurbineRotorClientRegistry.clientTick();
    }

    @Nullable
    public static RotorState getState(BlockPos controllerPos) {
        TurbineRotorClientRegistry.ClientEntry entry = TurbineRotorClientRegistry.getEntry(controllerPos);
        if (entry == null || entry.geometry == null) {
            return null;
        }
        return new RotorState(entry, controllerPos);
    }

    public static boolean shouldHideStatic(BlockPos worldPos) {
        return TurbineRotorClientRegistry.shouldHideStatic(worldPos);
    }

    public static boolean shouldSpin(BlockPos controllerPos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        var be = level.getBlockEntity(controllerPos);
        if (be instanceof TurbineControllerBlockEntity controller) {
            return TurbineRotorClientRegistry.shouldAnimate(controller);
        }
        return false;
    }

    public static boolean shouldRenderAssembly(BlockPos controllerPos) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return false;
        }
        var be = level.getBlockEntity(controllerPos);
        if (be instanceof TurbineControllerBlockEntity controller) {
            return TurbineRotorClientRegistry.hasRenderableGeometry(controller);
        }
        return false;
    }

    /** View wrapper over {@link TurbineRotorClientRegistry.ClientEntry} for BER. */
    public static final class RotorState {
        private final TurbineRotorClientRegistry.ClientEntry entry;
        private final BlockPos controllerPos;

        RotorState(TurbineRotorClientRegistry.ClientEntry entry, BlockPos controllerPos) {
            this.entry = entry;
            this.controllerPos = controllerPos;
        }

        public boolean isAssemblyReady() {
            return ClientConfig.TURBINE_ROTOR_ROTATION_ENABLED.get() && entry.hasRenderableGeometry();
        }

        public boolean shouldSpin() {
            return entry.shouldAnimate();
        }

        public float getAngleDegrees(float partialTick) {
            return entry.getAngleDegrees(partialTick, controllerPos);
        }

        public long[] rodPositions() {
            return entry.geometry != null ? entry.geometry.rodPositions() : new long[0];
        }

        public Direction rodFacing(int index) {
            return entry.rodFacing(index);
        }
    }
}
