package net.unfamily.colossal_reactors.reactor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;

import java.util.List;

/**
 * Routes reactor/turbine production to EXTRACT ports by medium and per-port toggles (first port with space wins).
 */
public final class ResourcePortOutputRouter {

    private ResourcePortOutputRouter() {}

    public static int pushFluid(List<ResourcePortBlockEntity> ports, FluidStack stack) {
        if (stack.isEmpty()) return stack.getAmount();
        int remaining = stack.getAmount();
        for (ResourcePortBlockEntity port : ports) {
            if (remaining <= 0) break;
            if (port.getPortMode() != PortMode.EXTRACT) continue;
            if (!port.isAllowLiquid() || port.isAllowGas()) continue;
            int filled = port.receiveFluidFromReactor(new FluidStack(stack.getFluid(), remaining));
            remaining -= filled;
        }
        return remaining;
    }

    public static int pushGas(List<ResourcePortBlockEntity> ports, Object chemicalStack) {
        if (!MekChemicalHelper.isLoaded() || chemicalStack == null || MekChemicalHelper.isEmpty(chemicalStack)) {
            return (int) MekChemicalHelper.getAmount(chemicalStack);
        }
        long remaining = MekChemicalHelper.getAmount(chemicalStack);
        for (ResourcePortBlockEntity port : ports) {
            if (remaining <= 0) break;
            if (port.getPortMode() != PortMode.EXTRACT) continue;
            if (!port.isAllowGas() || port.isAllowLiquid()) continue;
            Object copy = MekChemicalHelper.copyStack(chemicalStack, remaining);
            if (copy == null) break;
            int filled = port.receiveGasFromReactor(copy);
            remaining -= filled;
        }
        return (int) remaining;
    }

    public static ItemStack pushItem(List<ResourcePortBlockEntity> ports, ItemStack stack) {
        if (stack.isEmpty()) return stack;
        ItemStack remaining = stack.copy();
        for (ResourcePortBlockEntity port : ports) {
            if (remaining.isEmpty()) break;
            if (port.getPortMode() != PortMode.EXTRACT) continue;
            if (!port.isAllowSolid()) continue;
            if (!port.canAcceptItemFromReactor()) continue;
            remaining = port.receiveItemFromReactor(remaining);
        }
        return remaining;
    }

    public static int availableFluidSpace(List<ResourcePortBlockEntity> ports) {
        int space = 0;
        for (ResourcePortBlockEntity port : ports) {
            if (port.getPortMode() != PortMode.EXTRACT) continue;
            if (!port.isAllowLiquid() || port.isAllowGas()) continue;
            space += port.getFluidTank().getCapacity() - port.getFluidTank().getFluidAmount();
        }
        return space;
    }

    public static long availableGasSpace(List<ResourcePortBlockEntity> ports) {
        long space = 0;
        for (ResourcePortBlockEntity port : ports) {
            if (port.getPortMode() != PortMode.EXTRACT) continue;
            if (!port.isAllowGas() || port.isAllowLiquid()) continue;
            space += port.getGasSpaceMb();
        }
        return space;
    }
}
