package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Routes reactor/turbine production to EXTRACT ports by medium, port role (fuel vs coolant), and toggles.
 * Coolant exhaust uses {@link #pushFluid}/{@link #pushGas}; fuel waste uses {@link #pushFuelFluid}/{@link #pushFuelGas}.
 */
public final class ResourcePortOutputRouter {

    private ResourcePortOutputRouter() {}

    /** Coolant liquid output (steam, etc.) — ports with coolant role and liquid medium only. */
    public static int pushFluid(List<ResourcePortBlockEntity> ports, FluidStack stack) {
        return pushFluidToRole(ports, stack, false);
    }

    /** Fuel/fluid waste — fuel-role ports first, then coolant-role ports for the remainder. */
    public static int pushFuelFluid(List<ResourcePortBlockEntity> ports, FluidStack stack) {
        int left = pushFluidToRole(ports, stack, true);
        if (left > 0) {
            left = pushFluidToRole(ports, new FluidStack(stack.getFluid(), left), false);
        }
        return left;
    }

    private static int pushFluidToRole(List<ResourcePortBlockEntity> ports, FluidStack stack, boolean fuelRole) {
        if (stack.isEmpty()) {
            return stack.getAmount();
        }
        int remaining = stack.getAmount();
        for (ResourcePortBlockEntity port : ports) {
            if (remaining <= 0) {
                break;
            }
            if (port.getPortMode() != PortMode.EXTRACT) {
                continue;
            }
            if (fuelRole ? !port.getPortFilter().acceptsFuelRole() : !port.getPortFilter().acceptsCoolantRole()) {
                continue;
            }
            if (!port.isAllowLiquid() || port.isAllowGas()) {
                continue;
            }
            int filled = port.receiveFluidFromReactor(new FluidStack(stack.getFluid(), remaining));
            remaining -= filled;
        }
        return remaining;
    }

    /** Coolant gas output — ports with coolant role and gas medium only. */
    public static int pushGas(List<ResourcePortBlockEntity> ports, Object chemicalStack) {
        return pushGasToRole(ports, chemicalStack, false);
    }

    /**
     * Fuel chemical waste (e.g. Mek nuclear waste). Tries fuel-role gas ports first, then coolant-role gas ports.
     */
    public static int pushFuelGas(List<ResourcePortBlockEntity> ports, Object chemicalStack) {
        if (!MekChemicalHelper.isLoaded() || chemicalStack == null || MekChemicalHelper.isEmpty(chemicalStack)) {
            return (int) MekChemicalHelper.getAmount(chemicalStack);
        }
        int left = pushGasToRole(ports, chemicalStack, true);
        if (left > 0) {
            Object remainder = MekChemicalHelper.copyStack(chemicalStack, left);
            if (remainder != null) {
                left = pushGasToRole(ports, remainder, false);
            }
        }
        return left;
    }

    private static int pushGasToRole(List<ResourcePortBlockEntity> ports, Object chemicalStack, boolean fuelRole) {
        if (!MekChemicalHelper.isLoaded() || chemicalStack == null || MekChemicalHelper.isEmpty(chemicalStack)) {
            return (int) MekChemicalHelper.getAmount(chemicalStack);
        }
        long remaining = MekChemicalHelper.getAmount(chemicalStack);
        for (ResourcePortBlockEntity port : ports) {
            if (remaining <= 0) {
                break;
            }
            if (port.getPortMode() != PortMode.EXTRACT) {
                continue;
            }
            if (fuelRole ? !port.getPortFilter().acceptsFuelRole() : !port.getPortFilter().acceptsCoolantRole()) {
                continue;
            }
            if (!port.isAllowGas() || port.isAllowLiquid()) {
                continue;
            }
            Object copy = MekChemicalHelper.copyStack(chemicalStack, remaining);
            if (copy == null) {
                continue;
            }
            int filled = port.receiveGasFromReactor(copy);
            remaining -= filled;
        }
        return (int) remaining;
    }

    public static ItemStack pushItem(List<ResourcePortBlockEntity> ports, ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = stack.copy();
        for (ResourcePortBlockEntity port : ports) {
            if (remaining.isEmpty()) {
                break;
            }
            if (port.getPortMode() != PortMode.EXTRACT) {
                continue;
            }
            if (!port.getPortFilter().acceptsFuelRole()) {
                continue;
            }
            if (!port.isAllowSolid()) {
                continue;
            }
            if (!port.canAcceptItemFromReactor()) {
                continue;
            }
            remaining = port.receiveItemFromReactor(remaining);
        }
        return remaining;
    }

    public static int availableFluidSpace(List<ResourcePortBlockEntity> ports) {
        int space = 0;
        for (ResourcePortBlockEntity port : ports) {
            if (port.getPortMode() != PortMode.EXTRACT) {
                continue;
            }
            if (!port.getPortFilter().acceptsCoolantRole()) {
                continue;
            }
            if (!port.canAcceptFluidFromReactor()) {
                continue;
            }
            space += port.getFluidCapacityMb() - port.getFluidAmountMb();
        }
        return space;
    }

    /**
     * True when at least one EXTRACT port can accept the coolant's configured output (fluid and/or Mek gas).
     */
    public static boolean canExportCoolantOutput(
            List<ResourcePortBlockEntity> extractPorts,
            @Nullable CoolantDefinition coolantDef,
            RegistryAccess registryAccess) {
        return availableCoolantOutputSpaceMb(extractPorts, coolantDef, registryAccess) > 0;
    }

    public static long availableGasSpace(List<ResourcePortBlockEntity> ports) {
        long space = 0;
        for (ResourcePortBlockEntity port : ports) {
            if (port.getPortMode() != PortMode.EXTRACT) {
                continue;
            }
            if (!port.getPortFilter().acceptsCoolantRole()) {
                continue;
            }
            if (!port.canAcceptGasFromReactor()) {
                continue;
            }
            space += port.getGasSpaceMb();
        }
        return space;
    }

    /**
     * Free mB on EXTRACT ports for this coolant's configured outputs (max of liquid and/or gas paths).
     */
    public static int availableCoolantOutputSpaceMb(
            List<ResourcePortBlockEntity> extractPorts,
            @Nullable CoolantDefinition coolantDef,
            RegistryAccess registryAccess) {
        if (extractPorts.isEmpty() || coolantDef == null) {
            return 0;
        }
        int space = 0;
        String liquidSel = coolantDef.liquidOutputSelector();
        if (liquidSel != null && !liquidSel.isBlank()) {
            Fluid fluid = liquidSel.startsWith("#")
                    ? CoolantLoader.getFirstFluidFromTag(liquidSel, registryAccess)
                    : BuiltInRegistries.FLUID.getValue(Identifier.tryParse(liquidSel));
            if (fluid != null && fluid != Fluids.EMPTY) {
                space = Math.max(space, availableFluidSpace(extractPorts));
            }
        }
        String gasSel = coolantDef.gasOutputSelector();
        if (gasSel != null && MekChemicalHelper.isLoaded()) {
            space = Math.max(space, (int) Math.min(Integer.MAX_VALUE, availableGasSpace(extractPorts)));
        }
        return space;
    }
}
