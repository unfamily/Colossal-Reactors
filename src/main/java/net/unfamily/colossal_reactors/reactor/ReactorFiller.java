package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
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
        // Use cached resource port positions (rebuilt on validate/revalidate).
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

        for (ResourcePortBlockEntity port : insertPorts) {
            // Fuel: controller-wide buffer; pull as many items as total space allows.
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
                    // Hard cap per tick to avoid draining huge stacks in one tick.
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
        }
    }
}
