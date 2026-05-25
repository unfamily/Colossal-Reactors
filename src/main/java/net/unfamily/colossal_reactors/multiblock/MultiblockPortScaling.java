package net.unfamily.colossal_reactors.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.fuel.FuelMedium;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.reactor.ReactorValidation;

/**
 * Resizes resource port fluid/gas tanks from estimated per-tick demand (× {@link Config#PORT_DEMAND_MULTIPLIER}),
 * floored at the config default tank capacity.
 */
public final class MultiblockPortScaling {

    private MultiblockPortScaling() {}

    public record ReactorDemandEstimate(
            int fuelMbPerTick,
            int coolantMbPerTick,
            int wasteMbPerTick,
            int exhaustMbPerTick
    ) {}

    public static void scaleReactorPorts(ServerLevel level, ReactorControllerBlockEntity controller) {
        if (!Config.SCALE_PORT_TANK_WITH_MULTIBLOCK.get()) {
            return;
        }
        ReactorValidation.Result result = controller.getCachedResult();
        if (result == null || !result.valid()) {
            return;
        }
        int minMb = Config.RESOURCE_PORT_TANK_CAPACITY_MB.get();
        int multiplier = Config.PORT_DEMAND_MULTIPLIER.get();
        ReactorDemandEstimate estimate = estimateReactorDemand(level, controller, result);
        applyReactorPorts(level, controller.getCachedResourcePortPositions(), minMb, multiplier, estimate);
    }

    public static void scaleTurbinePorts(ServerLevel level, TurbineControllerBlockEntity controller) {
        if (!Config.SCALE_PORT_TANK_WITH_MULTIBLOCK.get()) {
            return;
        }
        int minMb = Config.TURBINE_RESOURCE_PORT_TANK_CAPACITY_MB.get();
        int multiplier = Config.PORT_DEMAND_MULTIPLIER.get();
        int steamPerTick = Math.max(1, controller.getCachedSteamConsumeMbPerTick());
        int target = scaledTargetMb(minMb, steamPerTick, multiplier);
        applyUniformPorts(level, controller.getCachedResourcePortPositions(), target);
    }

    public static int estimateReactorCoolantMoveBudgetMb(
            ReactorControllerBlockEntity controller,
            RegistryAccess registryAccess
    ) {
        if (!Config.SCALE_PORT_TANK_WITH_MULTIBLOCK.get()) {
            return 4000;
        }
        ReactorValidation.Result result = controller.getCachedResult();
        if (result == null || !result.valid()) {
            return 4000;
        }
        ServerLevel level = controller.getLevel() instanceof ServerLevel sl ? sl : null;
        if (level == null) {
            return 4000;
        }
        ReactorDemandEstimate estimate = estimateReactorDemand(level, controller, result);
        return Math.max(1000, estimate.coolantMbPerTick());
    }

    private static void applyReactorPorts(
            ServerLevel level,
            long[] portPositions,
            int minMb,
            int multiplier,
            ReactorDemandEstimate estimate
    ) {
        if (portPositions == null) {
            return;
        }
        for (long packed : portPositions) {
            BlockEntity be = level.getBlockEntity(BlockPos.of(packed));
            if (!(be instanceof ResourcePortBlockEntity port)) {
                continue;
            }
            PortFilter filter = port.getPortFilter();
            PortMode mode = port.getPortMode();
            int demand = demandMbForPort(mode, filter, estimate);
            port.applyTankCapacityMb(scaledTargetMb(minMb, demand, multiplier));
        }
    }

    private static void applyUniformPorts(ServerLevel level, long[] portPositions, int targetMb) {
        if (portPositions == null) {
            return;
        }
        for (long packed : portPositions) {
            BlockEntity be = level.getBlockEntity(BlockPos.of(packed));
            if (be instanceof ResourcePortBlockEntity port) {
                port.applyTankCapacityMb(targetMb);
            }
        }
    }

    private static int demandMbForPort(PortMode mode, PortFilter filter, ReactorDemandEstimate estimate) {
        return switch (mode) {
            case INSERT -> switch (filter) {
                case BOTH -> Math.max(estimate.fuelMbPerTick(), estimate.coolantMbPerTick());
                case ONLY_SOLID_FUEL -> estimate.fuelMbPerTick();
                case ONLY_COOLANT_LIQUID -> estimate.coolantMbPerTick();
            };
            case EXTRACT -> switch (filter) {
                case BOTH -> Math.max(estimate.wasteMbPerTick(), estimate.exhaustMbPerTick());
                case ONLY_SOLID_FUEL -> estimate.wasteMbPerTick();
                case ONLY_COOLANT_LIQUID -> estimate.exhaustMbPerTick();
            };
            case EJECT -> Math.max(
                    Math.max(estimate.fuelMbPerTick(), estimate.coolantMbPerTick()),
                    Math.max(estimate.wasteMbPerTick(), estimate.exhaustMbPerTick()));
        };
    }

    private static int scaledTargetMb(int configDefaultMb, int demandMbPerTick, int multiplier) {
        long scaled = (long) demandMbPerTick * multiplier;
        if (scaled > Integer.MAX_VALUE) {
            scaled = Integer.MAX_VALUE;
        }
        return (int) Math.max(configDefaultMb, scaled);
    }

    static ReactorDemandEstimate estimateReactorDemand(
            ServerLevel level,
            ReactorControllerBlockEntity controller,
            ReactorValidation.Result result
    ) {
        RegistryAccess registryAccess = level.registryAccess();
        int rodCount = controller.getCachedRodPositions() != null ? controller.getCachedRodPositions().length : 0;
        if (rodCount <= 0) {
            return new ReactorDemandEstimate(0, 0, 0, 0);
        }

        double effectiveRodCount = controller.getCachedEffectiveRodCount();
        if (effectiveRodCount <= 1e-9) {
            effectiveRodCount = Math.max(1.0, rodCount);
        }

        double[] base = effectiveBaseFromControllerFuel(controller);
        double baseRf = base[0];
        double baseFuelUnitsPerTick = base[1];

        CoolantDefinition coolantDef = controller.getCoolantDefinition(registryAccess);
        if (coolantDef == null) {
            coolantDef = CoolantLoader.get(CoolantLoader.WATER_COOLANT_ID);
        }
        double rfMultiplier = coolantDef != null ? coolantDef.rfMultiplier() : 1.0;
        double mbMultiplier = coolantDef != null && coolantDef.mbMultiplier() > 0 ? coolantDef.mbMultiplier() : 1.0;

        ReactorControllerBlockEntity.HeatSinkStaticCache cache = controller.getCachedHeatSinkStaticCache();
        double heatSinkFuelMult = 1.0;
        double heatSinkEnergyMult = 1.0;
        int countAdj = 0;
        int countNon = 0;
        double sumEnergyAdj = 0.0;
        if (cache != null) {
            double wAdj = Config.HEAT_SINK_ADJACENT_WEIGHT.get();
            double wNon = Config.HEAT_SINK_NON_ADJACENT_WEIGHT.get();
            var rodM = HeatSinkLoader.getModifiersForFluidOrDefault(Fluids.EMPTY, registryAccess);
            double sumFuelRod = cache.countRod() * rodM.fuelMultiplier();
            double sumEnergyRod = cache.countRod() * rodM.energyMultiplier();
            double totalWeightedFuel = sumFuelRod + cache.sumFuelAdj() * wAdj + cache.sumFuelNon() * wNon;
            double totalWeightedEnergy = sumEnergyRod + cache.sumEnergyAdj() * wAdj + cache.sumEnergyNon() * wNon;
            double totalWeight = cache.countRod() + cache.countAdj() * wAdj + cache.countNon() * wNon;
            if (totalWeight > 0) {
                heatSinkFuelMult = Math.max(0.1, totalWeightedFuel / totalWeight);
                heatSinkEnergyMult = Math.max(0.1, totalWeightedEnergy / totalWeight);
            }
            countAdj = cache.countAdj();
            countNon = cache.countNon();
            sumEnergyAdj = cache.sumEnergyAdj();
        }

        double rfEfficiency = 1.0 - Config.RF_EFFICIENCY_LOSS.get();
        double fuelEfficiency = Config.FUEL_EFFICIENCY_LOSS.get();
        double productionMult = Config.PRODUCTION_MULTIPLIER.get();
        double consumptionMult = Config.CONSUMPTION_MULTIPLIER.get();
        double energyRodFactor = rodEnergyScaling(effectiveRodCount);

        double decayRods = Math.max(0.0, Config.CONSUMPTION_CURVE_DECAY_RODS.get());
        double curveStrength = (decayRods <= 0) ? 1.0 : decayRods / (effectiveRodCount + decayRods);
        double curveStrengthAdjusted = 0.4 + 0.50 * curveStrength;
        double consumptionScale = Config.CONSUMPTION_SCALE.get() / Math.pow(effectiveRodCount + 1, 0.5 * curveStrengthAdjusted);
        double consumptionDivisor = Math.max(0.1, Config.HEAT_SINK_CONSUMPTION_DIVISOR.get());
        double fuelConsumptionRate = baseFuelUnitsPerTick * consumptionMult * fuelEfficiency * effectiveRodCount
                * consumptionScale / mbMultiplier / heatSinkFuelMult / consumptionDivisor;
        if (countAdj + countNon > 0) {
            fuelConsumptionRate *= Math.max(0.1, Config.HEAT_SINK_FUEL_UNITS_MULTIPLIER.get());
        }
        fuelConsumptionRate = Math.max(fuelConsumptionRate, Config.MIN_FUEL_UNITS_PER_TICK.get());

        double rfProduced;
        if (countAdj + countNon > 0 && effectiveRodCount > 0) {
            double adjEnergyAvg = countAdj > 0 ? sumEnergyAdj / countAdj : 1.0;
            double adjCoverage = Math.min(1.0, (double) countAdj / Math.max(1.0, rodCount));
            double heatSinkRfFactor = adjEnergyAvg * adjCoverage;
            rfProduced = baseRf * productionMult * rfEfficiency * energyRodFactor * heatSinkRfFactor * rfMultiplier
                    * Math.max(0.1, Config.HEAT_SINK_RF_MULTIPLIER.get());
        } else {
            rfProduced = baseRf * productionMult * rfEfficiency * energyRodFactor * rfMultiplier * heatSinkEnergyMult;
        }
        rfProduced = Math.max(rfProduced, Config.MIN_RF_PER_TICK.get());

        boolean waterMode = coolantDef != null
                && (coolantDef.reduceRfProduction() || CoolantLoader.WATER_COOLANT_ID.equals(coolantDef.coolantId()));

        int coolantMbPerTick = 0;
        int exhaustMbPerTick = 0;
        if (waterMode && coolantDef != null) {
            coolantMbPerTick = (int) Math.ceil(rfProduced * coolantDef.rfToCoolantFactor());
            exhaustMbPerTick = (int) Math.ceil(coolantMbPerTick * coolantDef.steamPerCoolant());
        }

        FuelDefinition primaryFuel = primaryFuelDefinition(controller);
        int fuelMbPerTick = fluidFuelMbPerTick(primaryFuel, (float) fuelConsumptionRate);
        int wasteMbPerTick = fluidWasteMbPerTick(primaryFuel, (float) fuelConsumptionRate);

        return new ReactorDemandEstimate(fuelMbPerTick, coolantMbPerTick, wasteMbPerTick, exhaustMbPerTick);
    }

    private static FuelDefinition primaryFuelDefinition(ReactorControllerBlockEntity controller) {
        var entries = controller.getFuelEntries();
        if (entries.isEmpty()) {
            return FuelLoader.get(net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity.URANIUM_FUEL_ID);
        }
        return FuelLoader.get(entries.get(0).id());
    }

    private static int fluidFuelMbPerTick(FuelDefinition def, float fuelUnitsPerTick) {
        if (def == null || fuelUnitsPerTick <= 0) {
            return 0;
        }
        FuelMedium medium = def.inputMedium();
        if (medium != FuelMedium.FLUID && medium != FuelMedium.CHEMICAL) {
            return 0;
        }
        return Math.max(0, def.inputAmountForGrantedUnits(fuelUnitsPerTick));
    }

    private static int fluidWasteMbPerTick(FuelDefinition def, float fuelUnitsPerTick) {
        if (def == null || fuelUnitsPerTick <= 0) {
            return 0;
        }
        FuelMedium medium = def.outputMedium();
        if (medium != FuelMedium.FLUID && medium != FuelMedium.CHEMICAL) {
            return 0;
        }
        float wasteUnits = def.wasteUnitsFromFuelConsumed(fuelUnitsPerTick);
        return Math.max(0, def.wasteEjectAmountFromWasteUnits(wasteUnits));
    }

    private static double[] effectiveBaseFromControllerFuel(ReactorControllerBlockEntity controller) {
        double sumRf = 0;
        double sumFuel = 0;
        double totalUnits = 0;
        for (var e : controller.getFuelEntries()) {
            FuelDefinition def = FuelLoader.get(e.id());
            if (def == null) {
                continue;
            }
            double u = e.units();
            sumRf += u * def.baseRfPerTick();
            sumFuel += u * def.baseFuelUnitsPerTick();
            totalUnits += u;
        }
        if (totalUnits <= 0) {
            return new double[] { 200.0, 0.03 };
        }
        return new double[] { sumRf / totalUnits, sumFuel / totalUnits };
    }

    private static double rodEnergyScaling(double effectiveRodCount) {
        double n = Math.max(0.0, effectiveRodCount);
        int mode = Config.ROD_ENERGY_SCALING_MODE.get();
        if (mode == 2) {
            double k = Math.max(1.0, Config.ROD_ENERGY_SCALING_SATURATION_K.get());
            return n / (1.0 + (n / k));
        }
        if (mode == 1) {
            double exp = Config.ROD_ENERGY_SCALING_EXPONENT.get();
            return Math.pow(n, exp);
        }
        return n * (Math.log(n + 1.0) / 2.3);
    }
}
