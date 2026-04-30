package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * When the reactor controller sees the multiblock as valid (ON), it pulls fuel items from INSERT ports into rods.
 * Liquids (coolant, steam) use ports only; the simulation drains coolant from INSERT ports and pushes steam to EXTRACT/EJECT ports.
 */
public final class ReactorFiller {

    private ReactorFiller() {}

    /**
     * Called each tick (or every N ticks) when the reactor is ON and valid.
     * Pulls fuel items from INSERT ports into the controller-wide aggregated fuel buffer (no per-rod distribution).
     */
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

        var registryAccess = level.registryAccess();
        int coolantMoveBudgetMb = 4000;

        for (ResourcePortBlockEntity port : insertPorts) {
            // Fuel: controller-wide buffer; pull as many items as total space allows
            ItemStack stack = port.getItemHandler().getStackInSlot(0);
            if (!stack.isEmpty()) {
                FuelDefinition def = FuelLoader.getDefinitionForItem(stack, registryAccess);
                if (def != null) {
                    int units = def.unitsPerFuel();
                    if (units <= 0) units = 1;
                    int max = controller.getMaxFuelUnitsTotal();
                    float total = controller.getTotalFuelUnits();
                    float space = Math.max(0f, max - total);
                    int maxItems = (int) (space / units);
                    if (maxItems <= 0) continue;
                    int cap = Math.min(maxItems, 64);
                    for (int i = 0; i < cap; i++) {
                        ItemStack extracted = port.getItemHandler().extractItem(0, 1, false);
                        if (extracted.isEmpty()) break;
                        float added = controller.addFuel(def.fuelId(), units);
                        if (added <= 0.0001f) {
                            port.getItemHandler().insertItem(0, extracted, false);
                            break;
                        }
                    }
                }
            }

            // Coolant: move valid coolant fluids from INSERT ports into controller aggregated coolant buffer.
            if (coolantMoveBudgetMb > 0 && port.getPortFilter() != PortFilter.ONLY_SOLID_FUEL) {
                var stored = port.getStoredFluid();
                if (!stored.isEmpty() && stored.getFluid() != Fluids.EMPTY) {
                    var coolantDef = CoolantLoader.getDefinitionForFluid(stored.getFluid(), registryAccess);
                    if (coolantDef != null) {
                        int space = Math.max(0, controller.getCoolantCapacityMbTotal() - controller.getTotalCoolantMb());
                        int toMove = Math.min(space, Math.min(stored.getAmount(), coolantMoveBudgetMb));
                        if (toMove > 0) {
                            int drained = port.takeFluidForReactor(stored.getFluid(), toMove);
                            if (drained > 0) {
                                int added = controller.addCoolant(stored.getFluid(), drained);
                                int leftover = drained - added;
                                if (leftover > 0) {
                                    port.getFluidHandler().fill(new net.neoforged.neoforge.fluids.FluidStack(stored.getFluid(), leftover),
                                            net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
                                }
                                coolantMoveBudgetMb -= drained;
                            }
                        }
                    }
                }
            }
        }
    }
}
