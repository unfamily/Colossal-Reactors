package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * When the reactor controller sees the multiblock as valid (ON), it pulls from resource ports in INSERT mode
 * and fills rods (fuel items and coolant fluid), respecting each port's filter (both / only solid fuel / only coolant).
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
            PortFilter filter = port.getPortFilter();

            if (filter == PortFilter.BOTH || filter == PortFilter.ONLY_SOLID_FUEL) {
                ItemStack stack = port.getItemHandler().getStackInSlot(0);
                if (!stack.isEmpty()) {
                    FuelDefinition def = FuelLoader.getDefinitionForItem(stack, registryAccess);
                    if (def != null) {
                        int units = def.unitsPerItem();
                        if (def.fuelId().equals(ReactorRodBlockEntity.URANIUM_FUEL_ID) && units < 1000) {
                            units = 1000;
                        }
                        if (units <= 0) units = 1;
                        int totalSpace = 0;
                        for (ReactorRodBlockEntity rod : rods) {
                            totalSpace += Math.max(0, ReactorRodBlockEntity.getMaxFuelUnits() - (int) rod.getTotalFuelUnits());
                        }
                        if (totalSpace >= units) {
                            ItemStack extracted = port.getItemHandler().extractItem(0, 1, false);
                            if (!extracted.isEmpty()) {
                                int remaining = units;
                                for (ReactorRodBlockEntity rod : rods) {
                                    if (remaining <= 0) break;
                                    float added = rod.addFuel(def.fuelId(), remaining);
                                    remaining -= (int) added;
                                }
                                if (remaining == units) {
                                    port.getItemHandler().insertItem(0, extracted, false);
                                }
                            }
                        }
                    }
                }
            }

            if (filter == PortFilter.BOTH || filter == PortFilter.ONLY_COOLANT_LIQUID) {
                FluidStack fluidStack = port.getFluidTank().getFluid();
                if (!fluidStack.isEmpty()) {
                    CoolantDefinition def = CoolantLoader.getDefinitionForFluid(fluidStack.getFluid(), registryAccess);
                    if (def != null) {
                        int totalSpace = 0;
                        for (ReactorRodBlockEntity rod : rods) {
                            totalSpace += ReactorRodBlockEntity.getCoolantCapacityMb() - rod.getTotalCoolantMb();
                        }
                        int maxPerTick = Config.RESOURCE_PORT_TANK_CAPACITY_MB.get();
                        int toDrain = Math.min(maxPerTick, Math.min(fluidStack.getAmount(), totalSpace));
                        if (toDrain > 0) {
                            FluidStack drained = port.getFluidTank().drain(toDrain, IFluidHandler.FluidAction.EXECUTE);
                            if (!drained.isEmpty()) {
                                int remainingMb = drained.getAmount();
                                for (ReactorRodBlockEntity rod : rods) {
                                    if (remainingMb <= 0) break;
                                    int added = rod.addCoolant(drained.getFluid(), remainingMb);
                                    remainingMb -= added;
                                }
                                if (remainingMb > 0) {
                                    port.getFluidTank().fill(new FluidStack(drained.getFluid(), remainingMb), IFluidHandler.FluidAction.EXECUTE);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
