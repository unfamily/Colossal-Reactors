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
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;

/**
 * Pulls steam from INSERT resource ports into the controller steam input buffer (one tick of consumption).
 */
public final class TurbineFiller {

    private TurbineFiller() {}

    public static void tickFill(ServerLevel level, TurbineControllerBlockEntity controller) {
        var result = controller.getCachedResult();
        if (!result.valid()) return;

        TurbineGenerationDefinition gen = TurbineGenerationLoader.getDefault();
        Fluid steamFluid = TurbineSimulation.resolveInputSteamFluid(level.registryAccess());

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

            int drained = 0;
            if (steamFluid != null && steamFluid != Fluids.EMPTY) {
                drained = port.takeFluidForReactor(steamFluid, budget);
                if (drained > 0) {
                    int added = controller.addSteamInput(steamFluid, drained);
                    int leftover = drained - added;
                    if (leftover > 0) {
                        port.getFluidHandler().fill(new FluidStack(steamFluid, leftover), IFluidHandler.FluidAction.EXECUTE);
                    }
                    budget -= drained;
                    continue;
                }
            }
            if (MekChemicalHelper.isLoaded() && gen != null) {
                for (String input : gen.inputs()) {
                    if (!MaterialSelector.isChemicalPrefix(input)) continue;
                    Object template = MekChemicalHelper.createStack(
                            net.minecraft.resources.ResourceLocation.tryParse(input.substring(1)), 1);
                    if (template == null) continue;
                    drained = port.takeGasForReactor(template, budget);
                    if (drained > 0 && steamFluid != null && steamFluid != Fluids.EMPTY) {
                        int added = controller.addSteamInput(steamFluid, drained);
                        budget -= added;
                    }
                    break;
                }
            }
        }
    }
}
