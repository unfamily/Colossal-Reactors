package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
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
     * Called each tick (or every N ticks) when the reactor is ON and valid. Pulls from INSERT ports and distributes to rods.
     */
    public static void tickFill(ServerLevel level, ReactorControllerBlockEntity controller) {
        ReactorValidation.Result result = controller.getCachedResult();
        if (result == null || !result.valid()) return;

        int minX = result.minX();
        int minY = result.minY();
        int minZ = result.minZ();
        int maxX = result.maxX();
        int maxY = result.maxY();
        int maxZ = result.maxZ();

        List<ResourcePortBlockEntity> insertPorts = new ArrayList<>();
        List<ReactorRodBlockEntity> rods = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.RESOURCE_PORT.get())) {
                        if (level.getBlockEntity(pos) instanceof ResourcePortBlockEntity port && port.getPortMode() == PortMode.INSERT) {
                            insertPorts.add(port);
                        }
                    } else if (level.getBlockState(pos).is(ModBlocks.REACTOR_ROD.get())) {
                        if (level.getBlockEntity(pos) instanceof ReactorRodBlockEntity rod) {
                            rods.add(rod);
                        }
                    }
                }
            }
        }

        if (rods.isEmpty()) return;

        var registryAccess = level.registryAccess();

        for (ResourcePortBlockEntity port : insertPorts) {
            // Fuel: pool capacity across all rods; pull as many items as total space allows
            ItemStack stack = port.getItemHandler().getStackInSlot(0);
            if (!stack.isEmpty()) {
                FuelDefinition def = FuelLoader.getDefinitionForItem(stack, registryAccess);
                if (def != null) {
                    int units = def.unitsPerFuel();
                    if (def.fuelId().equals(ReactorRodBlockEntity.URANIUM_FUEL_ID) && units < 1000) {
                        units = 1000;
                    }
                    if (units <= 0) units = 1;
                    int totalSpace = 0;
                    for (ReactorRodBlockEntity rod : rods) {
                        totalSpace += Math.max(0, ReactorRodBlockEntity.getMaxFuelUnits() - (int) rod.getTotalFuelUnits());
                    }
                    int maxItems = totalSpace >= units ? totalSpace / units : 0;
                    for (int i = 0; i < maxItems; i++) {
                        totalSpace = 0;
                        for (ReactorRodBlockEntity rod : rods) {
                            totalSpace += Math.max(0, ReactorRodBlockEntity.getMaxFuelUnits() - (int) rod.getTotalFuelUnits());
                        }
                        if (totalSpace < units) break;
                        ItemStack extracted = port.getItemHandler().extractItem(0, 1, false);
                        if (extracted.isEmpty()) break;
                        int remaining = units;
                        for (ReactorRodBlockEntity rod : rods) {
                            if (remaining <= 0) break;
                            float added = rod.addFuel(def.fuelId(), remaining);
                            remaining -= (int) added;
                        }
                        if (remaining == units) {
                            port.getItemHandler().insertItem(0, extracted, false);
                            break;
                        }
                    }
                }
            }
        }
    }
}
