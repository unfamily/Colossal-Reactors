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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Fuel/waste I/O routed by {@link FuelDefinition#inputMedium()} and {@link FuelDefinition#outputMedium()}.
 */
public final class FuelIo {

    private static final Logger LOGGER = LoggerFactory.getLogger(FuelIo.class);

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
            if (!port.getPortFilter().acceptsFuelRole() || !port.isAllowSolid() || stack.isEmpty()
                    || !port.canAcceptItemFromReactor()) {
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
            if (!port.getPortFilter().acceptsFuelRole() || !port.isAllowLiquid() || port.isAllowGas()) {
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
            if (!port.getPortFilter().acceptsFuelRole() || !port.isAllowGas() || port.isAllowLiquid()) {
                continue;
            }
            Object copy = MekChemicalHelper.copyStack(stack, remaining);
            if (copy == null) {
                continue;
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
        Fluid fluid = resolveOutputFluid(def.output(), registryAccess);
        if (fluid == null || fluid == Fluids.EMPTY) {
            return;
        }
        int left = ResourcePortOutputRouter.pushFuelFluid(extractPorts, new FluidStack(fluid, wasteMb));
        if (left >= wasteMb) {
            return;
        }
        int exportedMb = wasteMb - left;
        float wasteUnitsCost = def.wasteUnitsCostForOutputAmount(exportedMb);
        float consumedUnits = controller.consumeWasteUnits(wasteBufferId, wasteUnitsCost);
        int actualMb = def.wasteEjectAmountFromWasteUnits(consumedUnits);
        if (actualMb <= 0) {
            controller.addWasteUnits(wasteBufferId, consumedUnits);
            return;
        }
        int refundMb = exportedMb - actualMb;
        if (refundMb > 0) {
            controller.addWasteUnits(wasteBufferId, def.wasteUnitsCostForOutputAmount(refundMb));
        }
    }

    private static void pushChemicalWaste(
            ReactorControllerBlockEntity controller,
            List<ResourcePortBlockEntity> extractPorts,
            ResourceLocation wasteBufferId,
            float wasteUnits,
            FuelDefinition def) {
        if (!MekChemicalHelper.isLoaded()) {
            LOGGER.warn("[CR-waste] Mekanism not loaded — skipping chemical waste push for {}", wasteBufferId);
            return;
        }
        String wasteSelector = def.output();
        if (wasteSelector == null || !MaterialSelector.isChemicalPrefix(wasteSelector)) {
            LOGGER.warn("[CR-waste] output selector '{}' is not a chemical selector for fuel {} — skipping", wasteSelector, def.fuelId());
            return;
        }
        int wasteMb = def.wasteEjectAmountFromWasteUnits(wasteUnits);
        if (wasteMb <= 0) {
            LOGGER.debug("[CR-waste] wasteUnits={} not enough for one produce={} batch (unitsPerWaste={}) — skipping {}",
                    wasteUnits, def.produce(), def.unitsPerWaste(), wasteBufferId);
            return;
        }
        Object template = MekChemicalHelper.createStackFromSelector(wasteSelector, 1);
        if (template == null) {
            LOGGER.warn("[CR-waste] createStackFromSelector('{}') returned null — check Mekanism registry for {}", wasteSelector, wasteBufferId);
            return;
        }
        long portSpaceMb = ResourcePortOutputRouter.availableFuelGasSpace(extractPorts, template);
        if (portSpaceMb <= 0) {
            LOGGER.debug("[CR-waste] no EXTRACT port has space for chemical '{}' (ports={}) — wasteUnits={} pending",
                    MekChemicalHelper.getTypeRegistryName(template), extractPorts.size(), wasteUnits);
            return;
        }
        int exportMb = (int) Math.min(wasteMb, portSpaceMb);
        Object stack = MekChemicalHelper.createStackFromSelector(wasteSelector, exportMb);
        if (stack == null) {
            LOGGER.warn("[CR-waste] createStackFromSelector('{}', {}) returned null — skipping", wasteSelector, exportMb);
            return;
        }
        int left = ResourcePortOutputRouter.pushFuelGas(extractPorts, stack);
        int exportedMb = exportMb - left;
        if (exportedMb <= 0) {
            LOGGER.warn("[CR-waste] pushFuelGas accepted 0 mB out of {} (ports={}) — check port gas-tank capacity and medium flags",
                    exportMb, extractPorts.size());
            return;
        }
        LOGGER.debug("[CR-waste] pushed {} mB of '{}' to EXTRACT ports; consuming {} waste units from buffer '{}'",
                exportedMb, wasteSelector, def.wasteUnitsCostForOutputAmount(exportedMb), wasteBufferId);
        float wasteUnitsCost = def.wasteUnitsCostForOutputAmount(exportedMb);
        float consumed = controller.consumeWasteUnits(wasteBufferId, wasteUnitsCost);
        if (consumed + 0.001f < wasteUnitsCost) {
            LOGGER.warn("[CR-waste] consumeWasteUnits returned {} instead of {} for buffer '{}' — waste accounting mismatch",
                    consumed, wasteUnitsCost, wasteBufferId);
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
