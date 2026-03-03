package net.unfamily.colossal_reactors.reactor;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.material.Fluid;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.PowerPortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
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

        double mbToConsumeRaw = baseMb * consumptionMult * mbEfficiency * rodCount * mbMultiplier;
        int mbToConsume = (int) Math.min(mbToConsumeRaw, totalFuelUnits);

        if (mbToConsume > 0) {
            consumeFuelFromRods(rods, mbToConsume, level.registryAccess());
        }

        double rfProduced = baseRf * productionMult * rfEfficiency * rodCount * efficiencyFactor * rfMultiplier;
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
                    int wasteCount = up <= 0 ? consumed : (consumed + up - 1) / up;
                    if (wasteCount > 0) rod.addSolidWaste(wasteId, wasteCount);
                }
            }
            rodIndex++;
        }
    }
}
