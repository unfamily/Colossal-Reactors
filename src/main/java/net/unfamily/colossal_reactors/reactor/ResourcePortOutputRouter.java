package net.unfamily.colossal_reactors.reactor;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;

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
            space += port.getFluidCapacityMb() - port.getFluidAmountMb();
        }
        return space;
    }
}
