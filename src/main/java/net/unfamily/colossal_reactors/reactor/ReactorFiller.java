package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.multiblock.MultiblockPortScaling;

import java.util.ArrayList;
import java.util.List;

/**
 * When the reactor is ON and valid, pulls fuel/coolant from INSERT ports into the controller buffer.
 * Pull amounts respect controller free space and {@link MultiblockPortScaling} coolant budget;
 * port {@link PortFilter} restricts fuel vs coolant paths.
 */
public final class ReactorFiller {

    private ReactorFiller() {}

    public static void tickFill(ServerLevel level, ReactorControllerBlockEntity controller) {
        ReactorValidation.Result result = controller.getCachedResult();
        if (result == null || !result.valid()) return;

        List<ResourcePortBlockEntity> insertPorts = new ArrayList<>();
        long[] resourcePorts = controller.getCachedResourcePortPositions();
        if (resourcePorts.length == 0) {
            controller.rebuildPartCaches(level, result);
            resourcePorts = controller.getCachedResourcePortPositions();
        }
        for (long lp : resourcePorts) {
            BlockPos pos = BlockPos.of(lp);
            if (level.getBlockEntity(pos) instanceof ResourcePortBlockEntity port && port.getPortMode() == PortMode.INSERT) {
                insertPorts.add(port);
            }
        }

        RegistryAccess registryAccess = level.registryAccess();
        int coolantMoveBudgetMb = MultiblockPortScaling.estimateReactorCoolantMoveBudgetMb(controller, registryAccess);

        for (ResourcePortBlockEntity port : insertPorts) {
            if (portAcceptsFuel(port) && port.isAllowSolid()) {
                pullSolidFuel(port, controller, registryAccess);
            }
            if (portAcceptsFuel(port) && port.isAllowLiquid()) {
                pullLiquidFuel(port, controller, registryAccess);
            }
            if (portAcceptsCoolant(port) && port.isAllowLiquid() && coolantMoveBudgetMb > 0) {
                coolantMoveBudgetMb = pullLiquidCoolant(port, controller, registryAccess, coolantMoveBudgetMb);
            }
        }
    }

    private static boolean portAcceptsFuel(ResourcePortBlockEntity port) {
        return port.getPortFilter().acceptsFuelRole();
    }

    private static boolean portAcceptsCoolant(ResourcePortBlockEntity port) {
        return port.getPortFilter().acceptsCoolantRole();
    }

    private static void pullSolidFuel(
            ResourcePortBlockEntity port,
            ReactorControllerBlockEntity controller,
            RegistryAccess registryAccess) {
        ItemStack stack = port.getItemHandler().getStackInSlot(0);
        if (stack.isEmpty()) {
            return;
        }
        FuelDefinition def = FuelLoader.getDefinitionForItem(stack, registryAccess);
        if (def == null) {
            return;
        }
        float space = Math.max(0f, controller.getMaxFuelUnitsTotal() - controller.getTotalFuelUnits());
        int wantItems = def.inputAmountBatched(def.inputAmountForSpaceUnits(space));
        if (wantItems <= 0) {
            return;
        }
        int cap = Math.min(wantItems, 64);
        for (int i = 0; i < cap; i++) {
            ItemStack extracted = port.getItemHandler().extractItem(0, 1, false);
            if (extracted.isEmpty()) break;
            float grant = def.fuelUnitsFromInputAmount(1f);
            float added = controller.addFuel(def.fuelId(), grant);
            if (added <= 0.0001f) {
                port.getItemHandler().insertItem(0, extracted, false);
                break;
            }
            if (added + 0.001f < grant) {
                port.getItemHandler().insertItem(0, extracted, false);
                controller.consumeFuel(def.fuelId(), added);
                break;
            }
        }
    }

    private static void pullLiquidFuel(
            ResourcePortBlockEntity port,
            ReactorControllerBlockEntity controller,
            RegistryAccess registryAccess) {
        FluidStack stored = port.getStoredFluid();
        if (stored.isEmpty() || stored.getFluid() == Fluids.EMPTY) {
            return;
        }
        FuelDefinition fuelDef = FuelLoader.getDefinitionForFluid(stored.getFluid(), registryAccess);
        if (fuelDef == null) {
            return;
        }
        float spaceUnits = Math.max(0f, controller.getMaxFuelUnitsTotal() - controller.getTotalFuelAndWasteUnits());
        int maxMbInTank = stored.getAmount();
        int maxMbBySpace = fuelDef.inputAmountForSpaceUnits(spaceUnits);
        int wantMb = fuelDef.pullAmountMb(Math.min(maxMbBySpace, maxMbInTank));
        if (wantMb <= 0) {
            return;
        }
        int drained = port.takeFluidForReactor(stored.getFluid(), wantMb);
        if (drained <= 0) {
            return;
        }
        float wouldAdd = fuelDef.fuelUnitsFromInputAmount(drained);
        float added = controller.addFuel(fuelDef.fuelId(), wouldAdd);
        if (added + 0.001f < wouldAdd) {
            int usedMb = fuelDef.inputAmountForGrantedUnits(added);
            int leftover = drained - usedMb;
            if (leftover > 0) {
                port.getFluidHandler().fill(new FluidStack(stored.getFluid(), leftover), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private static int pullLiquidCoolant(
            ResourcePortBlockEntity port,
            ReactorControllerBlockEntity controller,
            RegistryAccess registryAccess,
            int budget) {
        FluidStack stored = port.getStoredFluid();
        if (stored.isEmpty() || stored.getFluid() == Fluids.EMPTY) {
            return budget;
        }
        var coolantDef = CoolantLoader.getDefinitionForFluid(stored.getFluid(), registryAccess);
        if (coolantDef == null) {
            return budget;
        }
        int space = Math.max(0, controller.getCoolantCapacityMbTotal() - controller.getTotalCoolantMb());
        int toMove = Math.min(space, Math.min(stored.getAmount(), budget));
        if (toMove <= 0) {
            return budget;
        }
        int drained = port.takeFluidForReactor(stored.getFluid(), toMove);
        if (drained <= 0) {
            return budget;
        }
        int added = controller.addCoolant(stored.getFluid(), drained);
        int leftover = drained - added;
        if (leftover > 0) {
            port.getFluidHandler().fill(new FluidStack(stored.getFluid(), leftover), IFluidHandler.FluidAction.EXECUTE);
        }
        return budget - drained;
    }
}
