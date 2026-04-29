package net.unfamily.colossal_reactors.client.gui;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.reactor.ReactorSimulation;
import net.unfamily.colossal_reactors.reactor.RodPatternLogic;

import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

/**
 * Builder GUI simulation: same layout and formulas as {@link ReactorSimulation#simulateFromBuilderParams}
 * (identical RF and fuel formulas so GUI preview matches the real reactor).
 */
public final class ReactorBuilderSimulation {

    private ReactorBuilderSimulation() {}

    /**
     * Runs builder simulation with the same RF and fuel formulas as the real reactor.
     * Parameters must match ReactorBuilderBlockEntity (sizeLeft, sizeRight, sizeHeight, sizeDepth, rodPattern, patternMode, heatSinkIndex).
     */
    public static ReactorSimulation.SimulationResult run(
            RegistryAccess registryAccess,
            int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth,
            int rodPattern, int patternMode, int heatSinkIndex,
            @Nullable Identifier simulationFuelId,
            @Nullable CoolantDefinition coolantDef) {
        int w = sizeLeft + sizeRight + 1;
        int h = sizeHeight + 1;
        int d = sizeDepth + 1;
        int rw = RodPatternLogic.rodSpaceWidth(w, patternMode);
        int rh = RodPatternLogic.rodSpaceHeight(h, patternMode);
        int rd = RodPatternLogic.rodSpaceDepth(d, patternMode);
        int insetXZ = RodPatternLogic.rodSpaceInsetXZ(patternMode);
        boolean expansionRodAtCenter = (rodPattern == RodPatternLogic.PATTERN_EXPANSION)
                ? RodPatternLogic.getExpansionRodAtCenterForPreview(rw, rd)
                : false;

        Set<Long> rodSet = new HashSet<>();
        Set<Long> rodColumnSet = new HashSet<>();
        int rodCount = 0;
        for (int rx = 0; rx < rw; rx++) {
            for (int ry = 0; ry < rh; ry++) {
                for (int rz = 0; rz < rd; rz++) {
                    if (!RodPatternLogic.isRodForPreview(rx, ry, rz, rw, rh, rd, rodPattern, expansionRodAtCenter)) continue;
                    rodSet.add(key(rx, ry, rz));
                    rodColumnSet.add(((long) rx << 8) | (rz & 0xFF));
                    rodCount++;
                }
            }
        }
        int rodColumns = rodColumnSet.size();

        int countAdj = 0, countNon = 0;
        for (int lx = 1; lx < w - 1; lx++) {
            for (int ly = 1; ly < h - 1; ly++) {
                for (int lz = 1; lz < d - 1; lz++) {
                    if (isInteriorCellRod(lx, ly, lz, w, h, d, insetXZ, rodSet)) continue;
                    boolean adjacentToRod;
                    if (patternMode == RodPatternLogic.MODE_SUPER_ECONOMY) {
                        if (!isInRodSpace(lx, ly, lz, w, h, d, insetXZ)) continue;
                        adjacentToRod = isRodSpaceCellAdjacentToRod(lx, ly, lz, w, h, d, insetXZ, rodSet);
                        if (!adjacentToRod) continue;
                    } else {
                        adjacentToRod = isInteriorCellAdjacentToRod(lx, ly, lz, w, h, d, insetXZ, rodSet);
                        if (patternMode == RodPatternLogic.MODE_ECONOMY && !adjacentToRod) continue;
                    }
                    if (adjacentToRod) countAdj++;
                    else countNon++;
                }
            }
        }
        int coolantBlockCount = countAdj + countNon;
        // countAdj = cells with at least one rod neighbor (6 directions); countNon = cells with no rod neighbor. E.g. 7x7x7 Dots Optimized: rodCount=20, countAdj=60, countNon=45, coolantBlockCount=105
        if (Boolean.TRUE.equals(Config.REACTOR_SIMULATION_DEBUG.get())) {
            ColossalReactors.LOGGER.info("[ReactorBuilderSimulation] w={} h={} d={} patternMode={} rodPattern={} => rodCount={} countAdj={} countNon={} coolantBlockCount={}",
                    w, h, d, patternMode, rodPattern, rodCount, countAdj, countNon, coolantBlockCount);
        }

        // Match real reactor: only horizontal rod neighbors (no penalty for rod-space border; those neighbors are heat sink).
        double effectiveRodCount = 0;
        double penalty = Config.ROD_ADJACENCY_PENALTY.get();
        for (int rx = 0; rx < rw; rx++) {
            for (int ry = 0; ry < rh; ry++) {
                for (int rz = 0; rz < rd; rz++) {
                    if (!rodSet.contains(key(rx, ry, rz))) continue;
                    int adjacentCount = 0;
                    if (rx > 0 && rodSet.contains(key(rx - 1, ry, rz))) adjacentCount++;
                    if (rx < rw - 1 && rodSet.contains(key(rx + 1, ry, rz))) adjacentCount++;
                    if (rz > 0 && rodSet.contains(key(rx, ry, rz - 1))) adjacentCount++;
                    if (rz < rd - 1 && rodSet.contains(key(rx, ry, rz + 1))) adjacentCount++;
                    effectiveRodCount += Math.max(0.0, 1.0 - penalty * adjacentCount);
                }
            }
        }

        if (rodCount == 0) {
            return new ReactorSimulation.SimulationResult(0, 0, coolantBlockCount, 0, 0, 0, 0, 1000);
        }

        var coolantFluidFromPorts = (coolantDef != null) ? CoolantLoader.getFirstFluidFromDefinition(coolantDef, registryAccess) : null;
        HeatSinkLoader.HeatSinkModifiers rodMod = HeatSinkLoader.getModifiersForFluidOrDefault(coolantFluidFromPorts, registryAccess);
        HeatSinkLoader.HeatSinkModifiers heatSinkMod = HeatSinkLoader.getModifiersForHeatSinkIndex(registryAccess, heatSinkIndex);
        double wAdj = Config.HEAT_SINK_ADJACENT_WEIGHT.get();
        double wNon = Config.HEAT_SINK_NON_ADJACENT_WEIGHT.get();
        double sumFuelRod = rodCount * rodMod.fuelMultiplier();
        double sumEnergyRod = rodCount * rodMod.energyMultiplier();
        double sumFuelAdj = countAdj * heatSinkMod.fuelMultiplier();
        double sumEnergyAdj = countAdj * heatSinkMod.energyMultiplier();
        double sumFuelNon = countNon * heatSinkMod.fuelMultiplier();
        double sumEnergyNon = countNon * heatSinkMod.energyMultiplier();
        double totalWeightedFuel = sumFuelRod + sumFuelAdj * wAdj + sumFuelNon * wNon;
        double totalWeightedEnergy = sumEnergyRod + sumEnergyAdj * wAdj + sumEnergyNon * wNon;
        double totalWeight = rodCount + countAdj * wAdj + countNon * wNon;
        double heatSinkFuelMult = (totalWeight > 0) ? (totalWeightedFuel / totalWeight) : 1.0;
        double heatSinkEnergyMult = (totalWeight > 0) ? (totalWeightedEnergy / totalWeight) : 1.0;

        double rfMultiplier = coolantDef != null ? coolantDef.rfMultiplier() : 1.0;
        double mbMultiplier = coolantDef != null && coolantDef.mbMultiplier() > 0 ? coolantDef.mbMultiplier() : 1.0;
        Identifier fuelId = simulationFuelId != null ? simulationFuelId : ReactorRodBlockEntity.URANIUM_FUEL_ID;
        FuelDefinition fuelDef = FuelLoader.get(fuelId);
        double baseRf = fuelDef != null ? fuelDef.baseRfPerTick() : 200.0;
        double baseFuelUnitsPerTick = fuelDef != null ? fuelDef.baseFuelUnitsPerTick() : 0.03;
        double rfEfficiency = 1.0 - Config.RF_EFFICIENCY_LOSS.get();
        double fuelEfficiency = Config.FUEL_EFFICIENCY_LOSS.get();
        double productionMult = Config.PRODUCTION_MULTIPLIER.get();
        double consumptionMult = Config.CONSUMPTION_MULTIPLIER.get();
        double efficiencyFactor = Math.log(effectiveRodCount + 1) / 2.3;
        double decayRods = Math.max(0.0, Config.CONSUMPTION_CURVE_DECAY_RODS.get());
        double curveStrength = (decayRods <= 0) ? 1.0 : decayRods / (effectiveRodCount + decayRods);
        double curveStrengthAdjusted = 0.4 + 0.50 * curveStrength;
        double consumptionScale = Config.CONSUMPTION_SCALE.get() / Math.pow(effectiveRodCount + 1, 0.5 * curveStrengthAdjusted);
        double consumptionDivisor = Math.max(0.1, Config.HEAT_SINK_CONSUMPTION_DIVISOR.get());
        double fuelConsumptionRate = baseFuelUnitsPerTick * consumptionMult * fuelEfficiency * effectiveRodCount * consumptionScale / mbMultiplier / heatSinkFuelMult / consumptionDivisor;
        if (countAdj + countNon > 0) {
            fuelConsumptionRate *= Math.max(0.1, Config.HEAT_SINK_FUEL_UNITS_MULTIPLIER.get());
        }
        fuelConsumptionRate = Math.max(fuelConsumptionRate, Config.MIN_FUEL_UNITS_PER_TICK.get());

        // Same formula as real reactor: sumEnergyAdj + countNon * rodCount
        double rfProduced;
        if (countAdj + countNon > 0 && effectiveRodCount > 0) {
            double heatSinkRfFactor = (sumEnergyAdj + (double) countNon * rodCount) * efficiencyFactor / effectiveRodCount;
            rfProduced = baseRf * productionMult * rfEfficiency * heatSinkRfFactor * rfMultiplier * Math.max(0.1, Config.HEAT_SINK_RF_MULTIPLIER.get());
        } else {
            rfProduced = baseRf * productionMult * rfEfficiency * effectiveRodCount * efficiencyFactor * rfMultiplier * heatSinkEnergyMult;
        }
        rfProduced = Math.max(rfProduced, Config.MIN_RF_PER_TICK.get());

        boolean waterMode = coolantDef != null
                && (coolantDef.reduceRfProduction() || CoolantLoader.WATER_COOLANT_ID.equals(coolantDef.coolantId()));
        int steamPerTick = 0;
        int coolantConsumedPerTick = 0;
        int rfPerTick = 0;
        if (waterMode && coolantDef != null) {
            int coolantToConsumeMb = (int) (rfProduced * coolantDef.rfToCoolantFactor());
            coolantConsumedPerTick = coolantToConsumeMb;
            steamPerTick = (int) (coolantToConsumeMb * coolantDef.steamPerCoolant());
            rfPerTick = 0;
        } else {
            rfPerTick = (int) Math.max(0, rfProduced);
        }

        int fuelHundredths = (int) Math.round(fuelConsumptionRate * 100);
        return new ReactorSimulation.SimulationResult(rodCount, rodColumns, coolantBlockCount, rfPerTick, steamPerTick, coolantConsumedPerTick, fuelHundredths, 1000);
    }

    private static long key(int rx, int ry, int rz) {
        return ((long) rx << 16) | ((ry & 0xFF) << 8) | (rz & 0xFF);
    }

    private static boolean isInRodSpace(int lx, int ly, int lz, int w, int h, int d, int insetXZ) {
        return lx >= insetXZ && lx < w - insetXZ && ly >= 1 && ly < h - 1 && lz >= insetXZ && lz < d - insetXZ;
    }

    private static boolean isInteriorCellRod(int lx, int ly, int lz, int w, int h, int d, int insetXZ, Set<Long> rodSet) {
        if (!isInRodSpace(lx, ly, lz, w, h, d, insetXZ)) return false;
        return rodSet.contains(key(lx - insetXZ, ly - 1, lz - insetXZ));
    }

    private static boolean isRodSpaceCellAdjacentToRod(int lx, int ly, int lz, int w, int h, int d, int insetXZ, Set<Long> rodSet) {
        for (int dx = -1; dx <= 1; dx += 2)
            if (lx + dx >= insetXZ && lx + dx < w - insetXZ && isInteriorCellRod(lx + dx, ly, lz, w, h, d, insetXZ, rodSet)) return true;
        for (int dy = -1; dy <= 1; dy += 2)
            if (ly + dy >= 1 && ly + dy < h - 1 && isInteriorCellRod(lx, ly + dy, lz, w, h, d, insetXZ, rodSet)) return true;
        for (int dz = -1; dz <= 1; dz += 2)
            if (lz + dz >= insetXZ && lz + dz < d - insetXZ && isInteriorCellRod(lx, ly, lz + dz, w, h, d, insetXZ, rodSet)) return true;
        return false;
    }

    private static boolean isInteriorCellAdjacentToRod(int lx, int ly, int lz, int w, int h, int d, int insetXZ, Set<Long> rodSet) {
        for (int dx = -1; dx <= 1; dx += 2)
            if (lx + dx >= 1 && lx + dx < w - 1 && isInteriorCellRod(lx + dx, ly, lz, w, h, d, insetXZ, rodSet)) return true;
        for (int dy = -1; dy <= 1; dy += 2)
            if (ly + dy >= 1 && ly + dy < h - 1 && isInteriorCellRod(lx, ly + dy, lz, w, h, d, insetXZ, rodSet)) return true;
        for (int dz = -1; dz <= 1; dz += 2)
            if (lz + dz >= 1 && lz + dz < d - 1 && isInteriorCellRod(lx, ly, lz + dz, w, h, d, insetXZ, rodSet)) return true;
        return false;
    }
}
