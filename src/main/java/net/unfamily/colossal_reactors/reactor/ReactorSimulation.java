package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.PowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.minecraft.core.RegistryAccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Consumption and production cycle when reactor is ON: consume fuel from rods (produce solid waste),
 * produce RF and/or steam depending on coolant mode.
 * Formulas follow big_reactor style: base RF/MB, rod count, coolant modifiers, efficiency.
 *
 * <p><b>Water mode</b> (coolant = water / reduce_rf_production): RF converted to steam when fluid is sufficient
 * for full conversion to steam; only the unconverted part (insufficient water) is output as RF.
 * Example: 5x5 interior with 5 rods, water in rods → only steam (no RF). If water runs out → RF for the remainder.
 *
 * <p><b>Normal mode</b> (other coolants): RF to power ports only; steam conversion is defined per coolant in scripts (rf_to_coolant_factor, steam_per_coolant).
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
        double effectiveRodCount = computeEffectiveRodCount(level, rods);
        double totalFuelUnits = rods.stream().mapToDouble(ReactorRodBlockEntity::getTotalFuelUnits).sum();
        if (totalFuelUnits <= 0) return;

        double baseRf = Config.BASE_RF_PER_TICK.get();
        double baseFuelUnitsPerTick = Config.BASE_FUEL_UNITS_PER_TICK.get();
        double rfEfficiency = 1.0 - Config.RF_EFFICIENCY_LOSS.get();
        double fuelEfficiency = Config.FUEL_EFFICIENCY_LOSS.get();
        double productionMult = Config.PRODUCTION_MULTIPLIER.get();
        double consumptionMult = Config.CONSUMPTION_MULTIPLIER.get();

        CoolantDefinition coolantDef = getCoolantModifierFromPorts(resourcePorts, level.registryAccess());
        if (coolantDef == null) coolantDef = CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
        double rfMultiplier = coolantDef != null ? coolantDef.rfMultiplier() : 1.0;
        double mbMultiplier = coolantDef != null ? coolantDef.mbMultiplier() : 1.0;
        Fluid coolantFluidFromPorts = (coolantDef != null) ? CoolantLoader.getFirstFluidFromDefinition(coolantDef, level.registryAccess()) : null;

        HeatSinkLoader.HeatSinkModifiersResult heatSink = computeHeatSinkModifiers(level, result, rods.size(), rods, coolantFluidFromPorts);
        double heatSinkFuelMult = heatSink.fuelMultiplier();
        double heatSinkEnergyMult = heatSink.energyMultiplier();
        int countAdj = heatSink.countAdj();
        int countNon = heatSink.countNon();
        double sumEnergyAdj = heatSink.sumEnergyAdj();
        double sumFuelAdj = heatSink.sumFuelAdj();

        double efficiencyFactor = Math.log(effectiveRodCount + 1) / 2.3;

        // Consumption: empirical curve; divisor exponent fades with size so big reactors tend toward linear (less curve advantage). Fade reduced to 40% so curve keeps 60% of strength at large size.
        double decayRods = Math.max(0.0, Config.CONSUMPTION_CURVE_DECAY_RODS.get());
        double curveStrength = (decayRods <= 0) ? 1.0 : decayRods / (effectiveRodCount + decayRods);
        double curveStrengthAdjusted = 0.4 + 0.50 * curveStrength; // decay effect reduced by 50%
        double consumptionScale = Config.CONSUMPTION_SCALE.get() / Math.pow(effectiveRodCount + 1, 0.5 * curveStrengthAdjusted);
        double mbDivisor = (coolantDef != null && coolantDef.mbMultiplier() > 0) ? coolantDef.mbMultiplier() : 1.0;
        double consumptionDivisor = Math.max(0.1, Config.HEAT_SINK_CONSUMPTION_DIVISOR.get());
        double fuelConsumptionRate = baseFuelUnitsPerTick * consumptionMult * fuelEfficiency * effectiveRodCount * consumptionScale / mbDivisor / heatSinkFuelMult / consumptionDivisor;
        if (countAdj + countNon > 0) {
            fuelConsumptionRate *= Math.max(0.1, Config.HEAT_SINK_FUEL_UNITS_MULTIPLIER.get());
        }
        fuelConsumptionRate = Math.max(fuelConsumptionRate, Config.MIN_FUEL_UNITS_PER_TICK.get());
        double fuelUnitsToConsume = Math.min(fuelConsumptionRate, totalFuelUnits);

        if (fuelUnitsToConsume > 0) {
            consumeFuelFromRods(rods, fuelUnitsToConsume, level.registryAccess());
        }

        // RF with coolant cells: Base * (adjacent energy sum + nonAdj * rodCount) * efficiencyFactor / effectiveRodCount
        double rfProduced;
        if (countAdj + countNon > 0 && effectiveRodCount > 0) {
            double heatSinkRfFactor = (sumEnergyAdj + (double) countNon * rodCount) * efficiencyFactor / effectiveRodCount;
            rfProduced = baseRf * productionMult * rfEfficiency * heatSinkRfFactor * rfMultiplier * Math.max(0.1, Config.HEAT_SINK_RF_MULTIPLIER.get());
        } else {
            rfProduced = baseRf * productionMult * rfEfficiency * effectiveRodCount * efficiencyFactor * rfMultiplier * heatSinkEnergyMult;
        }
        rfProduced = Math.max(rfProduced, Config.MIN_RF_PER_TICK.get());

        // Water mode: coolant consumed for steam; RF reduced (only unconverted part). Active if def is water (by id) or has reduce_rf_production.
        boolean waterMode = coolantDef != null
                && (coolantDef.reduceRfProduction() || CoolantLoader.WATER_COOLANT_ID.equals(coolantDef.coolantId()));

        int rfPushedThisTick = 0;
        int steamProducedThisTick = 0;
        int waterConsumedThisTick = 0;

        if (waterMode) {
            // Water mode: consume coolant from INSERT ports for steam; push steam to EXTRACT ports only (EJECT = input back out, not reactor output). If all EXTRACT fluid ports are full, do not consume water (saturated).
            Fluid coolantFluid = CoolantLoader.getFirstFluidFromDefinition(coolantDef, level.registryAccess());
            int steamOutputSpace = 0;
            for (ResourcePortBlockEntity port : resourcePorts) {
                if (port.getPortMode() != PortMode.EXTRACT) continue;
                if (port.getPortFilter() == PortFilter.ONLY_SOLID_FUEL) continue;
                steamOutputSpace += port.getFluidTank().getCapacity() - port.getFluidTank().getFluidAmount();
            }
            int coolantToConsumeMb = (steamOutputSpace <= 0) ? 0 : (int) (rfProduced * coolantDef.rfToCoolantFactor());
            if (coolantToConsumeMb > 0 && coolantFluid != null && coolantFluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                int totalDrained = 0;
                for (ResourcePortBlockEntity port : resourcePorts) {
                    if (totalDrained >= coolantToConsumeMb) break;
                    totalDrained += port.takeFluidForReactor(coolantFluid, coolantToConsumeMb - totalDrained);
                }
                waterConsumedThisTick = totalDrained;
                double steamMb = totalDrained * coolantDef.steamPerCoolant();
                int steamPerTick = (int) steamMb;
                steamProducedThisTick = steamPerTick;
                if (steamPerTick > 0) {
                    String outputTag = coolantDef.output();
                    Fluid steamFluid = CoolantLoader.getFirstFluidFromTag(outputTag, level.registryAccess());
                    if (steamFluid != null && steamFluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                        int remaining = steamPerTick;
                        for (ResourcePortBlockEntity port : resourcePorts) {
                            if (remaining <= 0) break;
                            if (port.getPortMode() != PortMode.EXTRACT) continue;
                            if (port.getPortFilter() == PortFilter.ONLY_SOLID_FUEL) continue;
                            int filled = port.receiveFluidFromReactor(new FluidStack(steamFluid, remaining));
                            remaining -= filled;
                        }
                    }
                }
                // Produce energy only when water was insufficient (unconverted part goes to RF)
                if (totalDrained < coolantToConsumeMb && coolantToConsumeMb > 0) {
                    double fractionNotConverted = 1.0 - (double) totalDrained / coolantToConsumeMb;
                    int rfToPush = (int) (rfProduced * fractionNotConverted);
                    if (rfToPush > 0 && !powerPorts.isEmpty()) {
                        for (PowerPortBlockEntity port : powerPorts) {
                            int accepted = port.receiveEnergyFromReactor(rfToPush);
                            rfPushedThisTick += accepted;
                            rfToPush -= accepted;
                            if (rfToPush <= 0) break;
                        }
                    }
                }
            } else {
                // No valid coolant fluid: push all as RF
                int rfPerTick = (int) Math.max(0, rfProduced);
                if (rfPerTick > 0 && !powerPorts.isEmpty()) {
                    for (PowerPortBlockEntity port : powerPorts) {
                        int accepted = port.receiveEnergyFromReactor(rfPerTick);
                        rfPushedThisTick += accepted;
                        rfPerTick -= accepted;
                        if (rfPerTick <= 0) break;
                    }
                }
            }
        } else {
            // Normal mode: only RF (no steam). Steam is only produced in water mode from consumed coolant.
            int rfPerTick = (int) Math.max(0, rfProduced);
            if (rfPerTick > 0 && !powerPorts.isEmpty()) {
                for (PowerPortBlockEntity port : powerPorts) {
                    int accepted = port.receiveEnergyFromReactor(rfPerTick);
                    rfPushedThisTick += accepted;
                    rfPerTick -= accepted;
                    if (rfPerTick <= 0) break;
                }
            }
        }

        pushEjectToPorts(rods, resourcePorts, level.registryAccess());
        pushWasteToPorts(rods, resourcePorts);

        int fuelHundredths = (int) Math.round(fuelConsumptionRate * 100);
        controller.setLastTickStats(rfPushedThisTick, steamProducedThisTick, waterConsumedThisTick, fuelHundredths);
    }

    /**
     * Pushes input material back out to EJECT ports: fuel (from rod fuel units, converted at unitsPerItem) and coolant.
     */
    private static void pushEjectToPorts(List<ReactorRodBlockEntity> rods, List<ResourcePortBlockEntity> resourcePorts, RegistryAccess registryAccess) {
        List<ResourcePortBlockEntity> ejectPorts = resourcePorts.stream()
                .filter(p -> p.getPortMode() == PortMode.EJECT)
                .toList();
        if (ejectPorts.isEmpty()) return;

        // Eject fuel: convert units back to items using definition's unitsPerItem
        for (ReactorRodBlockEntity rod : rods) {
            for (var entry : rod.getFuelEntries()) {
                if (entry.units() < 1e-6f) continue;
                FuelDefinition def = FuelLoader.get(entry.id());
                if (def == null) continue;
                int unitsPerItem = Math.max(1, def.unitsPerItem());
                int items = (int) (entry.units() / unitsPerItem);
                if (items <= 0) continue;
                float toConsume = items * (float) unitsPerItem;
                float consumed = rod.consumeFuel(entry.id(), toConsume);
                if (consumed < 1e-6f) continue;
                int actualItems = (int) (consumed / unitsPerItem);
                if (actualItems <= 0) continue;
                ItemStack template = FuelLoader.getFirstInputStack(entry.id(), registryAccess);
                if (template.isEmpty()) continue;
                ItemStack stack = new ItemStack(template.getItem(), actualItems);
                for (ResourcePortBlockEntity port : ejectPorts) {
                    if (port.getPortFilter() == PortFilter.ONLY_COOLANT_LIQUID) continue;
                    if (!port.canAcceptItemFromReactor() || stack.isEmpty()) continue;
                    ItemStack remaining = port.receiveItemFromReactor(stack);
                    stack = remaining;
                    if (stack.isEmpty()) break;
                }
                if (!stack.isEmpty() && stack.getCount() > 0) {
                    float putBack = stack.getCount() * (float) unitsPerItem;
                    rod.addFuel(entry.id(), putBack);
                }
            }
        }
        // Liquids (coolant/steam) use ports only; no coolant in rods to eject.
    }

    /**
     * Pushes solid waste and liquid waste (steam) from rods to resource ports in EXTRACT mode.
     */
    private static void pushWasteToPorts(List<ReactorRodBlockEntity> rods, List<ResourcePortBlockEntity> resourcePorts) {
        if (resourcePorts.isEmpty()) return;

        List<ResourcePortBlockEntity> extractPorts = resourcePorts.stream()
                .filter(p -> p.getPortMode() == PortMode.EXTRACT)
                .toList();
        if (extractPorts.isEmpty()) return;

        int totalSolidWaste = rods.stream().mapToInt(ReactorRodBlockEntity::getTotalSolidWasteCount).sum();
        if (totalSolidWaste > 0) {
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
                        if (port.getPortFilter() == PortFilter.ONLY_COOLANT_LIQUID) continue;
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
        // Liquid waste (steam) is pushed directly to EXTRACT/EJECT ports in water mode; no rod liquid waste.
    }

    /**
     * Fuel/energy multipliers from interior with adjacency influence: coolant adjacent to a rod (Refrigerante 0)
     * and coolant not adjacent (Refrigerante 1) are weighted separately; each cell counted once (no double-count).
     * Rod cells use their coolant fluid modifier; adjacent/non-adjacent coolant use block modifier with weights wAdj/wNon.
     */
    private static HeatSinkLoader.HeatSinkModifiersResult computeHeatSinkModifiers(ServerLevel level, ReactorValidation.Result result, int rodsFromList, List<ReactorRodBlockEntity> rods, Fluid coolantFluidFromPorts) {
        int minX = result.minX(), minY = result.minY(), minZ = result.minZ();
        int maxX = result.maxX(), maxY = result.maxY(), maxZ = result.maxZ();
        Set<BlockPos> rodPositions = new HashSet<>();
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.REACTOR_ROD.get())) {
                        rodPositions.add(pos);
                    }
                }
            }
        }
        double sumFuelRod = 0, sumEnergyRod = 0;
        int countRod = 0;
        double sumFuelAdj = 0, sumEnergyAdj = 0;
        int countAdj = 0;
        double sumFuelNon = 0, sumEnergyNon = 0;
        int countNon = 0;
        var reg = level.registryAccess();
        double wAdj = Config.HEAT_SINK_ADJACENT_WEIGHT.get();
        double wNon = Config.HEAT_SINK_NON_ADJACENT_WEIGHT.get();
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (level.getBlockState(pos).is(ModBlocks.REACTOR_ROD.get())) {
                        HeatSinkLoader.HeatSinkModifiers m = HeatSinkLoader.getModifiersForFluidOrDefault(coolantFluidFromPorts, reg);
                        sumFuelRod += m.fuelMultiplier();
                        sumEnergyRod += m.energyMultiplier();
                        countRod++;
                    } else {
                        HeatSinkLoader.HeatSinkModifiers m = HeatSinkLoader.getModifiersForBlockOrDefault(level.getBlockState(pos), reg);
                        boolean adjacentToRod = false;
                        for (Direction d : Direction.values()) {
                            if (rodPositions.contains(pos.relative(d))) {
                                adjacentToRod = true;
                                break;
                            }
                        }
                        if (adjacentToRod) {
                            sumFuelAdj += m.fuelMultiplier();
                            sumEnergyAdj += m.energyMultiplier();
                            countAdj++;
                        } else {
                            sumFuelNon += m.fuelMultiplier();
                            sumEnergyNon += m.energyMultiplier();
                            countNon++;
                        }
                    }
                }
            }
        }
        double totalWeightedFuel = sumFuelRod + sumFuelAdj * wAdj + sumFuelNon * wNon;
        double totalWeightedEnergy = sumEnergyRod + sumEnergyAdj * wAdj + sumEnergyNon * wNon;
        double totalWeight = countRod + countAdj * wAdj + countNon * wNon;
        if (totalWeight <= 0) return new HeatSinkLoader.HeatSinkModifiersResult(1.0, 1.0, 0.0, 0.0, 0, 0);
        double effFuel = totalWeightedFuel / totalWeight;
        double effEnergy = totalWeightedEnergy / totalWeight;
        if (Boolean.TRUE.equals(Config.REACTOR_SIMULATION_DEBUG.get())) {
            String coolantStr = (coolantFluidFromPorts != null && coolantFluidFromPorts != Fluids.EMPTY) ? BuiltInRegistries.FLUID.getKey(coolantFluidFromPorts).toString() : "none";
            ColossalReactors.LOGGER.info("[ReactorSimulation] Heat sink: rod(scan)={} rod(list)={} coolantFromPorts={} adj={} nonAdj={} sumEnergyAdj={} sumFuelAdj={} wAdj={} wNon={} => fuelMult={} energyMult={}",
                    countRod, rodsFromList, coolantStr, countAdj, countNon, sumEnergyAdj, sumFuelAdj, wAdj, wNon, effFuel, effEnergy);
        }
        return new HeatSinkLoader.HeatSinkModifiersResult(effFuel, effEnergy, sumEnergyAdj, sumFuelAdj, countAdj, countNon);
    }

    /**
     * Effective rod count for production/consumption: each rod contributes less when it has horizontal
     * neighbors that are another rod or a border block (casing, glass, ports). Only horizontal (same Y) is checked.
     */
    private static double computeEffectiveRodCount(ServerLevel level, List<ReactorRodBlockEntity> rods) {
        if (rods.isEmpty()) return 0;
        Set<BlockPos> rodPositions = new HashSet<>();
        for (ReactorRodBlockEntity rod : rods) {
            rodPositions.add(rod.getBlockPos());
        }
        double penalty = Config.ROD_ADJACENCY_PENALTY.get();
        double effective = 0;
        for (ReactorRodBlockEntity rod : rods) {
            BlockPos pos = rod.getBlockPos();
            int adjacentCount = 0;
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                if (rodPositions.contains(neighbor) || ReactorValidation.isShellBlock(level.getBlockState(neighbor))) {
                    adjacentCount++;
                }
            }
            double mult = Math.max(0.0, 1.0 - penalty * adjacentCount);
            effective += mult;
        }
        return effective;
    }

    /** Coolant definition from INSERT ports (fluid in tank). Prefers water when present. */
    private static CoolantDefinition getCoolantModifierFromPorts(List<ResourcePortBlockEntity> resourcePorts, net.minecraft.core.RegistryAccess registryAccess) {
        CoolantDefinition waterDef = CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
        for (ResourcePortBlockEntity port : resourcePorts) {
            if (port.getPortMode() != PortMode.INSERT) continue;
            var stack = port.getFluidTank().getFluid();
            if (stack.isEmpty()) continue;
            CoolantDefinition def = CoolantLoader.getDefinitionForFluid(stack.getFluid(), registryAccess);
            if (def != null) {
                if (CoolantLoader.WATER_COOLANT_ID.equals(def.coolantId())) return waterDef != null ? waterDef : def;
            }
        }
        for (ResourcePortBlockEntity port : resourcePorts) {
            if (port.getPortMode() != PortMode.INSERT) continue;
            var stack = port.getFluidTank().getFluid();
            if (stack.isEmpty()) continue;
            CoolantDefinition def = CoolantLoader.getDefinitionForFluid(stack.getFluid(), registryAccess);
            if (def != null) return def;
        }
        return CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
    }

    private static void consumeFuelFromRods(List<ReactorRodBlockEntity> rods, double totalUnitsToConsume, net.minecraft.core.RegistryAccess registryAccess) {
        double remaining = totalUnitsToConsume;
        int rodIndex = 0;
        while (remaining > 1e-6 && rodIndex < rods.size() * 2) {
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
            float take = (float) Math.min(remaining, first.units());
            float consumed = rod.consumeFuel(first.id(), take);
            remaining -= consumed;
            if (consumed > 0 && !def.output().isEmpty() && !def.output().startsWith("#")) {
                ResourceLocation wasteId = ResourceLocation.tryParse(def.output());
                if (wasteId != null) {
                    int up = def.unitsPerItem();
                    rod.recordConsumedAndAddWaste(wasteId, consumed, up <= 0 ? 1 : up);
                }
            }
            rodIndex++;
        }
    }
}
