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
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.PowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Consumption and production cycle when reactor is ON: consume fuel from rods (produce solid waste),
 * produce RF and/or steam depending on coolant mode.
 * Formulas follow big_reactor style: base RF/MB, rod count, coolant modifiers, efficiency.
 *
 * <p><b>Water mode</b> (coolant = water / consumesFluidForSteam): no RF when water is sufficient
 * for full conversion to steam; only the unconverted part (insufficient water) is output as RF.
 * Example: 5x5 interior with 5 rods, water in rods → only steam (no RF). If water runs out → RF for the remainder.
 *
 * <p><b>Normal mode</b> (other coolants): RF to power ports and steam from RF (STEAM_PER_RF).
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
        int totalFuelUnits = rods.stream().mapToInt(ReactorRodBlockEntity::getTotalFuelUnits).sum();
        if (totalFuelUnits <= 0) return;

        double baseRf = Config.BASE_RF_PER_TICK.get();
        double baseMb = Config.BASE_MB_PER_TICK.get();
        double rfEfficiency = 1.0 - Config.RF_EFFICIENCY_LOSS.get();
        double mbEfficiency = Config.MB_EFFICIENCY_LOSS.get();
        double productionMult = Config.PRODUCTION_MULTIPLIER.get();
        double consumptionMult = Config.CONSUMPTION_MULTIPLIER.get();

        CoolantDefinition coolantDef = getCoolantModifierFromRods(rods);
        if (coolantDef == null) coolantDef = CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
        double rfMultiplier = coolantDef != null ? coolantDef.rfMultiplier() : 1.0;
        double mbMultiplier = coolantDef != null ? coolantDef.mbMultiplier() : 1.0;

        double efficiencyFactor = Math.log(effectiveRodCount + 1) / 2.3;

        // Consumption: empirical curve (scale/sqrt(rodCount+1)) fitted to experimental data; then divided by coolant mbMultiplier so each coolant adapts (e.g. Air 100% -> 1.0, Gelid 160% -> 1.6).
        double consumptionScale = Config.CONSUMPTION_SCALE.get() / Math.sqrt(effectiveRodCount + 1);
        double mbDivisor = (coolantDef != null && coolantDef.mbMultiplier() > 0) ? coolantDef.mbMultiplier() : 1.0;
        double ingotsToConsumeRaw = baseMb * consumptionMult * mbEfficiency * effectiveRodCount * consumptionScale / mbDivisor;
        int mbToConsume = (int) Math.min(ingotsToConsumeRaw * 1000, totalFuelUnits);

        if (mbToConsume > 0) {
            consumeFuelFromRods(rods, mbToConsume, level.registryAccess());
        }

        double rfProduced = baseRf * productionMult * rfEfficiency * effectiveRodCount * efficiencyFactor * rfMultiplier;

        // Water mode: coolant is consumed for steam; no RF when water is sufficient. Use water mode if def is water (by id) or has consumesFluidForSteam.
        boolean waterMode = coolantDef != null
                && (coolantDef.consumesFluidForSteam() || CoolantLoader.WATER_COOLANT_ID.equals(coolantDef.coolantId()));

        int rfPushedThisTick = 0;
        int steamProducedThisTick = 0;
        int waterConsumedThisTick = 0;

        if (waterMode) {
            // Water mode: consume coolant for steam. Only produce RF when water is insufficient for full conversion.
            int coolantToConsumeMb = (int) (rfProduced * coolantDef.rfToCoolantFactor());
            Fluid coolantFluid = CoolantLoader.getFirstFluidFromDefinition(coolantDef, level.registryAccess());
            if (coolantToConsumeMb > 0 && coolantFluid != null && coolantFluid != net.minecraft.world.level.material.Fluids.EMPTY) {
                int totalDrained = 0;
                for (ReactorRodBlockEntity rod : rods) {
                    if (totalDrained >= coolantToConsumeMb) break;
                    totalDrained += rod.drainCoolant(coolantFluid, coolantToConsumeMb - totalDrained);
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
                        for (ReactorRodBlockEntity rod : rods) {
                            if (remaining <= 0) break;
                            int added = rod.addLiquidWaste(steamFluid, remaining);
                            remaining -= added;
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

        pushWasteToPorts(rods, resourcePorts);

        int fuelHundredths = (int) Math.round(ingotsToConsumeRaw * 100);
        controller.setLastTickStats(rfPushedThisTick, steamProducedThisTick, waterConsumedThisTick, fuelHundredths);
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

        for (ResourcePortBlockEntity port : extractPorts) {
            if (port.getPortFilter() == PortFilter.ONLY_SOLID_FUEL) continue;
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

    /**
     * Returns the coolant definition to use. Prefers water: if any rod contains water (matches water definition),
     * return water definition so we always use water mode when water is present.
     */
    private static CoolantDefinition getCoolantModifierFromRods(List<ReactorRodBlockEntity> rods) {
        if (rods.isEmpty()) return CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
        var level = rods.get(0).getLevel();
        if (level == null) return CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
        var registryAccess = level.registryAccess();
        CoolantDefinition waterDef = CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
        for (ReactorRodBlockEntity rod : rods) {
            for (var entry : rod.getCoolantEntries()) {
                CoolantDefinition def = CoolantLoader.getDefinitionForFluid(entry.fluid(), registryAccess);
                if (def != null) {
                    if (CoolantLoader.WATER_COOLANT_ID.equals(def.coolantId())) return waterDef != null ? waterDef : def;
                }
            }
        }
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
                    rod.recordConsumedAndAddWaste(wasteId, consumed, up <= 0 ? 1 : up);
                }
            }
            rodIndex++;
        }
    }
}
