package net.unfamily.colossal_reactors.fuel;

import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;
import net.unfamily.colossal_reactors.reactor.ResourcePortOutputRouter;

import java.util.List;

/**
 * Fuel/waste I/O routed by {@link FuelDefinition#inputMedium()} and {@link FuelDefinition#outputMedium()}.
 */
public final class FuelIo {

    private FuelIo() {}

    public static void ejectFuelToPorts(
            ReactorControllerBlockEntity controller,
            List<ResourcePortBlockEntity> ejectPorts,
            RegistryAccess registryAccess) {
        if (ejectPorts.isEmpty()) {
            return;
        }
        for (ReactorControllerBlockEntity.FuelEntry entry : controller.getFuelEntries()) {
            if (entry.units() < 1e-6f) {
                continue;
            }
            FuelDefinition def = FuelLoader.get(entry.id());
            if (def == null) {
                continue;
            }
            switch (def.inputMedium()) {
                case ITEM -> ejectItemFuel(controller, ejectPorts, entry.id(), entry.units(), def, registryAccess);
                case FLUID -> ejectFluidFuel(controller, ejectPorts, entry.id(), entry.units(), def, registryAccess);
                case CHEMICAL -> ejectChemicalFuel(controller, ejectPorts, entry.id(), entry.units(), def);
            }
        }
    }

    public static void pushWasteToExtractPorts(
            ReactorControllerBlockEntity controller,
            List<ResourcePortBlockEntity> extractPorts,
            RegistryAccess registryAccess) {
        if (extractPorts.isEmpty()) {
            return;
        }
        for (ReactorControllerBlockEntity.WasteEntry entry : controller.getWasteEntries()) {
            if (entry.units() <= 1e-6f) {
                continue;
            }
            FuelDefinition def = FuelLoader.getDefinitionForWasteBuffer(entry.id());
            if (def == null) {
                continue;
            }
            switch (def.outputMedium()) {
                case ITEM -> pushItemWaste(controller, extractPorts, entry.id(), entry.units(), def, registryAccess);
                case FLUID -> pushFluidWaste(controller, extractPorts, entry.id(), entry.units(), def, registryAccess);
                case CHEMICAL -> pushChemicalWaste(controller, extractPorts, entry.id(), entry.units(), def);
            }
        }
    }

    private static void ejectItemFuel(
            ReactorControllerBlockEntity controller,
            List<ResourcePortBlockEntity> ejectPorts,
            ResourceLocation fuelId,
            float units,
            FuelDefinition def,
            RegistryAccess registryAccess) {
        float unitsPerItem = Math.max(1f, def.fuelUnitsPerItemStack());
        int items = (int) (units / unitsPerItem);
        if (items <= 0) {
            return;
        }
        float toConsume = items * unitsPerItem;
        float consumed = controller.consumeFuel(fuelId, toConsume);
        if (consumed < 1e-6f) {
            return;
        }
        int actualItems = (int) (consumed / unitsPerItem);
        if (actualItems <= 0) {
            return;
        }
        ItemStack template = FuelLoader.getFirstInputStack(fuelId, registryAccess);
        if (template.isEmpty()) {
            controller.addFuel(fuelId, consumed);
            return;
        }
        ItemStack stack = new ItemStack(template.getItem(), actualItems);
        for (ResourcePortBlockEntity port : ejectPorts) {
            if (!port.isAllowSolid() || stack.isEmpty() || !port.canAcceptItemFromReactor()) {
                continue;
            }
            stack = port.receiveItemFromReactor(stack);
            if (stack.isEmpty()) {
                break;
            }
        }
        if (!stack.isEmpty() && stack.getCount() > 0) {
            controller.addFuel(fuelId, stack.getCount() * unitsPerItem);
        }
    }

    private static void ejectFluidFuel(
            ReactorControllerBlockEntity controller,
            List<ResourcePortBlockEntity> ejectPorts,
            ResourceLocation fuelId,
            float units,
            FuelDefinition def,
            RegistryAccess registryAccess) {
        int mb = def.inputAmountForGrantedUnits(units);
        if (mb <= 0) {
            return;
        }
        Fluid fluid = MaterialSelector.resolvePreferredBufferFluid(def.inputs(), registryAccess);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return;
        }
        float wouldConsume = def.fuelUnitsFromInputAmount(mb);
        float consumed = controller.consumeFuel(fuelId, wouldConsume);
        int actualMb = def.inputAmountForGrantedUnits(consumed);
        if (actualMb <= 0) {
            return;
        }
        int remaining = actualMb;
        for (ResourcePortBlockEntity port : ejectPorts) {
            if (remaining <= 0) {
                break;
            }
            if (!port.isAllowLiquid() || port.isAllowGas()) {
                continue;
            }
            int filled = port.receiveFluidFromReactor(new FluidStack(fluid, remaining));
            remaining -= filled;
        }
        if (remaining > 0) {
            controller.addFuel(fuelId, def.fuelUnitsFromInputAmount(remaining));
        }
    }

    private static void ejectChemicalFuel(
            ReactorControllerBlockEntity controller,
            List<ResourcePortBlockEntity> ejectPorts,
            ResourceLocation fuelId,
            float units,
            FuelDefinition def) {
        if (!MekChemicalHelper.isLoaded()) {
            return;
        }
        String selector = FuelLoader.getFirstChemicalInputSelector(fuelId);
        if (selector == null || !MaterialSelector.isChemicalPrefix(selector)) {
            return;
        }
        ResourceLocation chemId = ResourceLocation.tryParse(selector.substring(1));
        if (chemId == null) {
            return;
        }
        int mb = def.inputAmountForGrantedUnits(units);
        if (mb <= 0) {
            return;
        }
        float wouldConsume = def.fuelUnitsFromInputAmount(mb);
        float consumed = controller.consumeFuel(fuelId, wouldConsume);
        int actualMb = def.inputAmountForGrantedUnits(consumed);
        if (actualMb <= 0) {
            return;
        }
        Object stack = MekChemicalHelper.createStack(chemId, actualMb);
        if (stack == null) {
            controller.addFuel(fuelId, consumed);
            return;
        }
        long remaining = actualMb;
        for (ResourcePortBlockEntity port : ejectPorts) {
            if (remaining <= 0) {
                break;
            }
            if (!port.isAllowGas() || port.isAllowLiquid()) {
                continue;
            }
            Object copy = MekChemicalHelper.copyStack(stack, remaining);
            if (copy == null) {
                break;
            }
            int filled = port.receiveGasFromReactor(copy);
            remaining -= filled;
        }
        if (remaining > 0) {
            controller.addFuel(fuelId, def.fuelUnitsFromInputAmount((int) remaining));
        }
    }

    private static void pushItemWaste(
            ReactorControllerBlockEntity controller,
            List<ResourcePortBlockEntity> extractPorts,
            ResourceLocation wasteBufferId,
            float wasteUnits,
            FuelDefinition def,
            RegistryAccess registryAccess) {
        int items = def.wasteEjectAmountFromWasteUnits(wasteUnits);
        if (items <= 0) {
            return;
        }
        ItemStack template = FuelLoader.getFirstOutputStack(def.fuelId(), registryAccess);
        if (template.isEmpty()) {
            return;
        }
        int toMove = Math.min(64, items);
        float wasteUnitsCost = def.wasteUnitsCostForOutputAmount(toMove);
        float consumedUnits = controller.consumeWasteUnits(wasteBufferId, wasteUnitsCost);
        int actualItems = def.wasteEjectAmountFromWasteUnits(consumedUnits);
        if (actualItems <= 0) {
            controller.addWasteUnits(wasteBufferId, consumedUnits);
            return;
        }
        ItemStack stack = new ItemStack(template.getItem(), actualItems);
        ItemStack remaining = ResourcePortOutputRouter.pushItem(extractPorts, stack);
        if (!remaining.isEmpty() && remaining.getCount() > 0) {
            controller.addWasteUnits(wasteBufferId, def.wasteUnitsCostForOutputAmount(remaining.getCount()));
        }
    }

    private static void pushFluidWaste(
            ReactorControllerBlockEntity controller,
            List<ResourcePortBlockEntity> extractPorts,
            ResourceLocation wasteBufferId,
            float wasteUnits,
            FuelDefinition def,
            RegistryAccess registryAccess) {
        int wasteMb = def.wasteEjectAmountFromWasteUnits(wasteUnits);
        if (wasteMb <= 0) {
            return;
        }
        float wasteUnitsCost = def.wasteUnitsCostForOutputAmount(wasteMb);
        float consumedUnits = controller.consumeWasteUnits(wasteBufferId, wasteUnitsCost);
        int actualMb = def.wasteEjectAmountFromWasteUnits(consumedUnits);
        if (actualMb <= 0) {
            controller.addWasteUnits(wasteBufferId, consumedUnits);
            return;
        }
        Fluid fluid = resolveOutputFluid(def.output(), registryAccess);
        if (fluid == null || fluid == Fluids.EMPTY) {
            controller.addWasteUnits(wasteBufferId, wasteUnitsCost);
            return;
        }
        int left = ResourcePortOutputRouter.pushFluid(extractPorts, new FluidStack(fluid, actualMb));
        if (left > 0) {
            controller.addWasteUnits(wasteBufferId, def.wasteUnitsCostForOutputAmount(left));
        }
    }

    private static void pushChemicalWaste(
            ReactorControllerBlockEntity controller,
            List<ResourcePortBlockEntity> extractPorts,
            ResourceLocation wasteBufferId,
            float wasteUnits,
            FuelDefinition def) {
        if (!MekChemicalHelper.isLoaded()) {
            return;
        }
        String wasteSelector = def.output();
        if (wasteSelector == null || !MaterialSelector.isChemicalPrefix(wasteSelector)) {
            return;
        }
        ResourceLocation chemId = ResourceLocation.tryParse(wasteSelector.substring(1));
        if (chemId == null) {
            return;
        }
        int wasteMb = def.wasteEjectAmountFromWasteUnits(wasteUnits);
        if (wasteMb <= 0) {
            return;
        }
        float wasteUnitsCost = def.wasteUnitsCostForOutputAmount(wasteMb);
        float consumedUnits = controller.consumeWasteUnits(wasteBufferId, wasteUnitsCost);
        int actualMb = def.wasteEjectAmountFromWasteUnits(consumedUnits);
        if (actualMb <= 0) {
            controller.addWasteUnits(wasteBufferId, consumedUnits);
            return;
        }
        Object stack = MekChemicalHelper.createStack(chemId, actualMb);
        if (stack == null) {
            controller.addWasteUnits(wasteBufferId, wasteUnitsCost);
            return;
        }
        int left = ResourcePortOutputRouter.pushGas(extractPorts, stack);
        if (left > 0) {
            controller.addWasteUnits(wasteBufferId, def.wasteUnitsCostForOutputAmount(left));
        }
    }

    private static Fluid resolveOutputFluid(String output, RegistryAccess registryAccess) {
        if (output == null || output.isBlank()) {
            return Fluids.EMPTY;
        }
        if (output.startsWith("#")) {
            return CoolantLoader.getFirstFluidFromTag(output, registryAccess);
        }
        return BuiltInRegistries.FLUID.get(ResourceLocation.tryParse(output));
    }
}
