package net.unfamily.colossal_reactors.turbine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import net.unfamily.colossal_reactors.util.FluidInputMatcher;

import java.util.List;

/**
 * Pulls steam from INSERT resource ports into the controller steam input buffer (one tick of consumption).
 */
public final class TurbineFiller {

    private TurbineFiller() {}

    public static void tickFill(ServerLevel level, TurbineControllerBlockEntity controller) {
        var result = controller.getCachedResult();
        if (!result.valid()) return;

        TurbineGenerationDefinition gen = TurbineGenerationLoader.getDefault();
        if (gen == null) return;

        List<String> inputs = gen.inputs();
        if (inputs.isEmpty()) return;

        int space = Math.max(0, controller.getSteamInputCapacityMb() - controller.getTotalSteamInputMb());
        if (space <= 0) return;

        long[] resourcePorts = controller.getCachedResourcePortPositions();
        if (resourcePorts.length == 0) {
            controller.rebuildPartCaches(level, result);
            resourcePorts = controller.getCachedResourcePortPositions();
        }

        int budget = space;
        for (long lp : resourcePorts) {
            if (budget <= 0) break;
            if (!(level.getBlockEntity(BlockPos.of(lp)) instanceof ResourcePortBlockEntity port)) continue;
            if (port.getPortMode() != PortMode.INSERT) continue;

            FluidStack stored = port.getStoredFluid();
            if (stored.isEmpty() || stored.getFluid() == Fluids.EMPTY) {
                continue;
            }
            if (!FluidInputMatcher.matchesAnyFluidInput(stored.getFluid(), inputs)) {
                continue;
            }
            int drained = port.takeFluidForReactor(stored.getFluid(), budget);
            if (drained <= 0) continue;
            int added = controller.addSteamInput(stored.getFluid(), drained);
            int leftover = drained - added;
            if (leftover > 0) {
                port.getFluidHandler().fill(new FluidStack(stored.getFluid(), leftover), IFluidHandler.FluidAction.EXECUTE);
            }
            budget -= added;
        }
    }
}
