package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.PowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumption and production cycle when reactor is ON: consume fuel from rods (produce solid waste),
 * produce RF (push to power ports), produce steam (add to rods' liquid waste).
 * Formulas follow big_reactor style: base RF/MB, rod count, coolant modifiers, efficiency.
 */
public final class ReactorSimulation {

    private ReactorSimulation() {}

    /**
     * Runs one tick of consumption and production. Call when reactor is ON and valid.
     */
    public static void tick(ServerLevel level, ReactorControllerBlockEntity controller) {
        ReactorValidation.Result result = controller.getCachedResult();
        if (result == null || !result.valid()) return;

        int minX = result.minX();
        int minY = result.minY();
        int minZ = result.minZ();
        int maxX = result.maxX();
        int maxY = result.maxY();
        int maxZ = result.maxZ();

        List<ReactorRodBlockEntity> rods = new ArrayList<>();
        List<PowerPortBlockEntity> powerPorts = new ArrayList<>();
        List<ResourcePortBlockEntity> resourcePorts = new ArrayList<>();

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.REACTOR_ROD.get())) {
                        if (level.getBlockEntity(pos) instanceof ReactorRodBlockEntity rod) {
                            rods.add(rod);
                        }
                    } else if (level.getBlockState(pos).is(ModBlocks.POWER_PORT.get())) {
                        if (level.getBlockEntity(pos) instanceof PowerPortBlockEntity port) {
                            powerPorts.add(port);
                        }
                    } else if (level.getBlockState(pos).is(ModBlocks.RESOURCE_PORT.get())) {
                        if (level.getBlockEntity(pos) instanceof ResourcePortBlockEntity port) {
                            resourcePorts.add(port);
                        }
                    }
                }
            }
        }

        if (rods.isEmpty()) return;

        int rodCount = rods.size();
        int totalFuelUnits = rods.stream().mapToInt(ReactorRodBlockEntity::getTotalFuelUnits).sum();
        if (totalFuelUnits <= 0) return;

        double baseRf = Config.BASE_RF_PER_TICK.get();
        double baseMb = Config.BASE_MB_PER_TICK.get();
        double rfEfficiency = 1.0 - Config.RF_EFFICIENCY_LOSS.get();
        double mbEfficiency = Config.MB_EFFICIENCY_LOSS.get();
        double productionMult = Config.PRODUCTION_MULTIPLIER.get();
        double consumptionMult = Config.CONSUMPTION_MULTIPLIER.get();

        CoolantDefinition coolantDef = getCoolantModifierFromRods(rods);
        double rfMultiplier = coolantDef != null ? coolantDef.rfMultiplier() : 1.0;
        double mbMultiplier = coolantDef != null ? coolantDef.mbMultiplier() : 1.0;

        double efficiencyFactor = Math.log(rodCount + 1) / 2.3;

        // baseMb = ingots per tick; 1 ingot = URANIUM_INGOT_MB units -> units to consume per tick
        double ingotsToConsumeRaw = baseMb * consumptionMult * mbEfficiency * rodCount * mbMultiplier;
        int mbToConsume = (int) Math.min(ingotsToConsumeRaw * Config.URANIUM_INGOT_MB.get(), totalFuelUnits);

        if (mbToConsume > 0) {
            consumeFuelFromRods(rods, mbToConsume, level.registryAccess());
        }

        double rfProduced = baseRf * productionMult * rfEfficiency * rodCount * efficiencyFactor * rfMultiplier;

        if (coolantDef != null && coolantDef.consumesFluidForSteam()) {
            // Water mode: no RF output; consume coolant (wouldBeRf * factor) and produce steam (consumed * steamPerCoolant).
            int coolantToConsumeMb = (int) (rfProduced * coolantDef.rfToCoolantFactor());
            Fluid coolantFluid = CoolantLoader.getFirstFluidFromDefinition(coolantDef, level.registryAccess());
            if (coolantToConsumeMb > 0 && coolantFluid != null && coolantFluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                int totalDrained = 0;
                for (ReactorRodBlockEntity rod : rods) {
                    if (totalDrained >= coolantToConsumeMb) break;
                    totalDrained += rod.drainCoolant(coolantFluid, coolantToConsumeMb - totalDrained);
                }
                double steamMb = totalDrained * coolantDef.steamPerCoolant();
                int steamPerTick = (int) steamMb;
                if (steamPerTick > 0) {
                    String outputTag = coolantDef.output();
                    Fluid steamFluid = CoolantLoader.getFirstFluidFromTag(outputTag, level.registryAccess());
                    if (steamFluid != null && steamFluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                        int remaining = steamPerTick;
                        for (ReactorRodBlockEntity rod : rods) {
                            if (remaining <= 0) break;
                            int added = rod.addLiquidWaste(steamFluid, remaining);
                            remaining -= added;
                        }
                    }
                }
            }
        } else {
            // Normal mode: push RF to power ports and produce steam from RF.
            int rfPerTick = (int) Math.max(0, rfProduced);
            if (rfPerTick > 0 && !powerPorts.isEmpty()) {
                for (PowerPortBlockEntity port : powerPorts) {
                    int accepted = port.receiveEnergyFromReactor(rfPerTick);
                    rfPerTick -= accepted;
                    if (rfPerTick <= 0) break;
                }
            }
            double steamMb = rfProduced * Config.STEAM_PER_RF.get();
            int steamPerTick = (int) steamMb;
            if (steamPerTick > 0) {
                String outputTag = coolantDef != null ? coolantDef.output() : "#c:steam";
                Fluid steamFluid = CoolantLoader.getFirstFluidFromTag(outputTag, level.registryAccess());
                if (steamFluid != null && steamFluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                    int remaining = steamPerTick;
                    for (ReactorRodBlockEntity rod : rods) {
                        if (remaining <= 0) break;
                        int added = rod.addLiquidWaste(steamFluid, remaining);
                        remaining -= added;
                    }
                }
            }
        }

        pushWasteToPorts(rods, resourcePorts);
    }

    /**
     * Pushes solid waste (when total >= 1000) and liquid waste (steam) from rods to resource ports
     * that are in EXTRACT or EJECT mode and not full.
     */
    private static void pushWasteToPorts(List<ReactorRodBlockEntity> rods, List<ResourcePortBlockEntity> resourcePorts) {
        if (resourcePorts.isEmpty()) return;

        List<ResourcePortBlockEntity> extractPorts = resourcePorts.stream()
                .filter(p -> p.getPortMode() == PortMode.EXTRACT || p.getPortMode() == PortMode.EJECT)
                .toList();

        int totalSolidWaste = rods.stream().mapToInt(ReactorRodBlockEntity::getTotalSolidWasteCount).sum();
        if (totalSolidWaste >= ReactorRodBlockEntity.getSolidWasteCapacity()) {
            ResourceLocation wasteId = null;
            for (ReactorRodBlockEntity rod : rods) {
                for (var e : rod.getSolidWasteEntries()) {
                    if (e.count() > 0) {
                        wasteId = e.id();
                        break;
                    }
                }
                if (wasteId != null) break;
            }
            if (wasteId != null) {
                Item item = BuiltInRegistries.ITEM.get(wasteId);
                if (item != null && item != net.minecraft.world.item.Items.AIR) {
                    int toTake = Math.min(64, totalSolidWaste);
                    ItemStack stack = ItemStack.EMPTY;
                    for (ReactorRodBlockEntity rod : rods) {
                        if (toTake <= 0) break;
                        int taken = rod.takeSolidWaste(wasteId, toTake);
                        if (taken > 0) {
                            if (stack.isEmpty()) stack = new ItemStack(item, taken);
                            else stack.grow(taken);
                            toTake -= taken;
                        }
                    }
                    for (ResourcePortBlockEntity port : extractPorts) {
                        if (stack.isEmpty() || !port.canAcceptItemFromReactor()) continue;
                        ItemStack remaining = port.receiveItemFromReactor(stack);
                        stack = remaining;
                        if (stack.isEmpty()) break;
                    }
                    if (!stack.isEmpty() && stack.getCount() > 0) {
                        for (ReactorRodBlockEntity rod : rods) {
                            if (stack.isEmpty()) break;
                            int putBack = Math.min(stack.getCount(), ReactorRodBlockEntity.getSolidWasteCapacity() - rod.getTotalSolidWasteCount());
                            if (putBack > 0) {
                                int added = rod.addSolidWaste(wasteId, putBack);
                                stack.shrink(added);
                            }
                        }
                    }
                }
            }
        }

        for (ResourcePortBlockEntity port : extractPorts) {
            while (port.canAcceptFluidFromReactor()) {
                int space = port.getFluidTank().getCapacity() - port.getFluidTank().getFluidAmount();
                if (space <= 0) break;
                boolean pushed = false;
                for (ReactorRodBlockEntity rod : rods) {
                    for (var entry : rod.getLiquidWasteEntries()) {
                        if (entry.amount() <= 0 || entry.fluid() == Fluids.EMPTY) continue;
                        int drain = Math.min(entry.amount(), space);
                        if (drain <= 0) continue;
                        int drained = rod.drainLiquidWaste(entry.fluid(), drain);
                        if (drained > 0) {
                            int filled = port.receiveFluidFromReactor(new FluidStack(entry.fluid(), drained));
                            if (filled < drained) {
                                rod.addLiquidWaste(entry.fluid(), drained - filled);
                            }
                            pushed = true;
                            break;
                        }
                    }
                    if (pushed) break;
                }
                if (!pushed) break;
            }
        }
    }

    private static CoolantDefinition getCoolantModifierFromRods(List<ReactorRodBlockEntity> rods) {
        if (rods.isEmpty()) return CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
        var level = rods.get(0).getLevel();
        if (level == null) return CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
        var registryAccess = level.registryAccess();
        for (ReactorRodBlockEntity rod : rods) {
            for (var entry : rod.getCoolantEntries()) {
                CoolantDefinition def = CoolantLoader.getDefinitionForFluid(entry.fluid(), registryAccess);
                if (def != null) return def;
            }
        }
        return CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
    }

    private static void consumeFuelFromRods(List<ReactorRodBlockEntity> rods, int totalUnitsToConsume, net.minecraft.core.RegistryAccess registryAccess) {
        int remaining = totalUnitsToConsume;
        int rodIndex = 0;
        while (remaining > 0 && rodIndex < rods.size() * 2) {
            ReactorRodBlockEntity rod = rods.get(rodIndex % rods.size());
            var entries = rod.getFuelEntries();
            if (entries.isEmpty()) {
                rodIndex++;
                continue;
            }
            ReactorRodBlockEntity.FuelEntry first = entries.get(0);
            FuelDefinition def = FuelLoader.get(first.id());
            if (def == null) {
                rodIndex++;
                continue;
            }
            int take = Math.min(remaining, first.units());
            int consumed = rod.consumeFuel(first.id(), take);
            remaining -= consumed;
            if (consumed > 0 && !def.output().isEmpty() && !def.output().startsWith("#")) {
                ResourceLocation wasteId = ResourceLocation.tryParse(def.output());
                if (wasteId != null) {
                    int up = def.unitsPerItem();
                    // wasteCount = ceil(consumed / unitsPerItem): e.g. 1000 units per waste item -> 1 waste per 1000 units
                    int wasteCount = up <= 0 ? consumed : (consumed + up - 1) / up;
                    if (wasteCount > 0) rod.addSolidWaste(wasteId, wasteCount);
                }
            }
            rodIndex++;
        }
    }
}
