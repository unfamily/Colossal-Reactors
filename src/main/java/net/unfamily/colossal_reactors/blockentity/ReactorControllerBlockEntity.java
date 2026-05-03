package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.menu.ReactorControllerMenu;
import net.unfamily.colossal_reactors.reactor.ReactorValidation;
import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * BlockEntity for the reactor controller. Caches multiblock validation result.
 * State (OFF/VALIDATING/ON) and tick logic are in ReactorControllerBlock.
 * When ON, right-click opens the Reactor OS GUI.
 */
public class ReactorControllerBlockEntity extends BlockEntity implements MenuProvider {

    private ReactorValidation.Result cachedResult;
    private ServerPlayer lastInteractingPlayer;

    /**
     * Runtime caches of reactor parts (positions as {@link BlockPos#asLong()}), rebuilt on validate/revalidate.
     * This avoids full-volume scans every tick for large reactors.
     */
    private long[] cachedRodPositions = new long[0];
    private long[] cachedPowerPortPositions = new long[0];
    private long[] cachedResourcePortPositions = new long[0];
    private long[] cachedRedstonePortPositions = new long[0];

    /**
     * Cached reactor-wide computations that previously scanned the whole interior each tick.
     * Rebuilt on validate/revalidate together with part caches.
     */
    private double cachedEffectiveRodCount = 0.0;
    @Nullable
    private HeatSinkStaticCache cachedHeatSinkStaticCache;

    public record HeatSinkStaticCache(
            int countRod,
            int countAdj,
            int countNon,
            double sumFuelAdj,
            double sumEnergyAdj,
            double sumOverheatingAdj,
            double sumFuelNon,
            double sumEnergyNon,
            double sumOverheatingNon
    ) {}

    // ---- Aggregated fuel + waste (performance: avoid per-rod updates) ----
    private static final String TAG_FUEL = "Fuel";
    private static final String TAG_WASTE = "Waste";
    private static final String TAG_COOLANT = "Coolant";
    private static final String TAG_FUEL_ID = "Id";
    private static final String TAG_FUEL_UNITS = "Units";
    private static final String TAG_COOLANT_MB = "Mb";
    private static final String TAG_WASTE_UNITS = "WasteUnits";

    public record FuelEntry(Identifier id, float units) {}
    public record WasteEntry(Identifier id, float units) {}
    public record CoolantEntry(Identifier fluidId, int mb) {}

    private static final Codec<FuelEntry> FUEL_ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf(TAG_FUEL_ID).forGetter(FuelEntry::id),
            Codec.FLOAT.fieldOf(TAG_FUEL_UNITS).forGetter(FuelEntry::units)
    ).apply(i, FuelEntry::new));

    private static final Codec<WasteEntry> WASTE_ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf(TAG_FUEL_ID).forGetter(WasteEntry::id),
            Codec.FLOAT.fieldOf(TAG_WASTE_UNITS).forGetter(WasteEntry::units)
    ).apply(i, WasteEntry::new));

    private static final Codec<CoolantEntry> COOLANT_ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf(TAG_FUEL_ID).forGetter(CoolantEntry::fluidId),
            Codec.INT.fieldOf(TAG_COOLANT_MB).forGetter(CoolantEntry::mb)
    ).apply(i, CoolantEntry::new));

    /** Fuel by type: id -> units (float). Total units <= getMaxFuelUnitsTotal(). */
    private final List<FuelEntry> fuelEntries = new ArrayList<>();
    /** Waste by type (same id as fuel type): id -> units (float). Shares capacity with fuel. */
    private final List<WasteEntry> wasteEntries = new ArrayList<>();
    /** Coolant by fluid id: fluidId -> mB. Total mB <= getCoolantCapacityMbTotal(). */
    private final List<CoolantEntry> coolantEntries = new ArrayList<>();

    /** Rod model fill level (0..12) driven by controller total fill. */
    private int cachedRodFillLevel = 0;
    private int rodVisualUpdateCursor = 0;

    /** Last tick stats for GUI (updated by ReactorSimulation.tick). Fuel is ingots/tick * 100 (e.g. 26 = 0.26). */
    private long lastRfPerTick;
    private int lastSteamPerTick;
    private int lastWaterPerTick;
    private int lastFuelPerTickHundredths;

    /** Reactor stability 0–1000 permille (0.0%–100.0%) when reactor unstability is enabled. Default 1000 = 100%. */
    private int stabilityPermille = 1000;

    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CONTROLLER_BE.get(), pos, state);
    }

    public void setLastInteractingPlayer(Player player) {
        this.lastInteractingPlayer = player instanceof ServerPlayer sp ? sp : null;
    }

    /** Called from block tick when VALIDATING -> ON/OFF; shows message in action bar. */
    public void notifyValidationResult() {
        if (lastInteractingPlayer != null && cachedResult != null) {
            Component message = cachedResult.valid()
                    ? Component.translatable("message.colossal_reactors.reactor_valid")
                    : Component.translatable("message.colossal_reactors.reactor_invalid");
            lastInteractingPlayer.sendSystemMessage(message, true);
            lastInteractingPlayer = null;
        }
    }

    public ReactorValidation.Result getCachedResult() {
        return cachedResult;
    }

    /** Set from block when validation is run (e.g. on player click). */
    public void setCachedResult(ReactorValidation.Result result) {
        this.cachedResult = result;
    }

    public long[] getCachedRodPositions() { return cachedRodPositions; }
    public long[] getCachedPowerPortPositions() { return cachedPowerPortPositions; }
    public long[] getCachedResourcePortPositions() { return cachedResourcePortPositions; }
    public long[] getCachedRedstonePortPositions() { return cachedRedstonePortPositions; }
    public double getCachedEffectiveRodCount() { return cachedEffectiveRodCount; }
    @Nullable
    public HeatSinkStaticCache getCachedHeatSinkStaticCache() { return cachedHeatSinkStaticCache; }

    public List<FuelEntry> getFuelEntries() { return List.copyOf(fuelEntries); }
    public float getTotalFuelUnits() { return (float) fuelEntries.stream().mapToDouble(FuelEntry::units).sum(); }

    public List<CoolantEntry> getCoolantEntries() { return List.copyOf(coolantEntries); }

    public int getTotalCoolantMb() {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, coolantEntries.stream().mapToLong(CoolantEntry::mb).sum()));
    }

    public int getCoolantCapacityMbTotal() {
        int perRod = ReactorRodBlockEntity.getCoolantCapacityMb();
        long rods = (cachedRodPositions != null && cachedRodPositions.length > 0)
                ? cachedRodPositions.length
                : (cachedResult != null && cachedResult.valid() ? cachedResult.rodCount() : 0);
        long total = rods * (long) Math.max(0, perRod);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, total));
    }

    /** Coolant definition from stored coolant fluids; prefers water when present. */
    public net.unfamily.colossal_reactors.coolant.CoolantDefinition getCoolantDefinition(net.minecraft.core.RegistryAccess registryAccess) {
        net.unfamily.colossal_reactors.coolant.CoolantDefinition waterDef =
                net.unfamily.colossal_reactors.coolant.CoolantLoader.get(net.unfamily.colossal_reactors.coolant.CoolantLoader.WATER_COOLANT_ID);
        for (CoolantEntry e : coolantEntries) {
            var fluid = BuiltInRegistries.FLUID.getValue(e.fluidId());
            if (fluid == null || fluid == Fluids.EMPTY) continue;
            var def = net.unfamily.colossal_reactors.coolant.CoolantLoader.getDefinitionForFluid(fluid, registryAccess);
            if (def != null && net.unfamily.colossal_reactors.coolant.CoolantLoader.WATER_COOLANT_ID.equals(def.coolantId())) {
                return waterDef != null ? waterDef : def;
            }
        }
        for (CoolantEntry e : coolantEntries) {
            var fluid = BuiltInRegistries.FLUID.getValue(e.fluidId());
            if (fluid == null || fluid == Fluids.EMPTY) continue;
            var def = net.unfamily.colossal_reactors.coolant.CoolantLoader.getDefinitionForFluid(fluid, registryAccess);
            if (def != null) return def;
        }
        return waterDef;
    }

    /** Adds coolant (mB) by fluid type; clamps to total capacity. Returns amount actually added. */
    public int addCoolant(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id == null) return 0;
        int total = getTotalCoolantMb();
        int max = getCoolantCapacityMbTotal();
        int add = Math.min(amountMb, Math.max(0, max - total));
        if (add <= 0) return 0;
        for (int i = 0; i < coolantEntries.size(); i++) {
            CoolantEntry e = coolantEntries.get(i);
            if (id.equals(e.fluidId())) {
                coolantEntries.set(i, new CoolantEntry(id, e.mb() + add));
                setChanged();
                return add;
            }
        }
        coolantEntries.add(new CoolantEntry(id, add));
        setChanged();
        return add;
    }

    /** Consumes coolant (mB) of a specific fluid type. Returns amount actually consumed. */
    public int consumeCoolant(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id == null) return 0;
        int remaining = amountMb;
        int consumed = 0;
        for (int i = 0; i < coolantEntries.size() && remaining > 0; i++) {
            CoolantEntry e = coolantEntries.get(i);
            if (!id.equals(e.fluidId())) continue;
            int take = Math.min(remaining, Math.max(0, e.mb()));
            if (take <= 0) continue;
            int left = e.mb() - take;
            consumed += take;
            remaining -= take;
            if (left <= 0) {
                coolantEntries.remove(i);
                i--;
            } else {
                coolantEntries.set(i, new CoolantEntry(id, left));
            }
        }
        if (consumed > 0) setChanged();
        return consumed;
    }

    public List<WasteEntry> getWasteEntries() { return List.copyOf(wasteEntries); }
    public float getTotalWasteUnits() { return (float) wasteEntries.stream().mapToDouble(WasteEntry::units).sum(); }
    public float getTotalFuelAndWasteUnits() { return getTotalFuelUnits() + getTotalWasteUnits(); }

    public float addWasteUnits(Identifier fuelId, float units) {
        if (units <= 0) return 0;
        float total = getTotalFuelAndWasteUnits();
        int max = getMaxFuelUnitsTotal();
        float add = Math.min(units, Math.max(0, max - total));
        if (add <= 0) return 0;
        mergeWaste(fuelId, add);
        setChanged();
        return add;
    }

    public float consumeWasteUnits(Identifier fuelId, float units) {
        if (units <= 0) return 0;
        for (int i = 0; i < wasteEntries.size(); i++) {
            WasteEntry e = wasteEntries.get(i);
            if (!e.id().equals(fuelId)) continue;
            float take = Math.min(units, e.units());
            float remain = e.units() - take;
            if (remain <= 0.0001f) wasteEntries.remove(i);
            else wasteEntries.set(i, new WasteEntry(fuelId, remain));
            if (take > 0) setChanged();
            return take;
        }
        return 0;
    }

    private void mergeWaste(Identifier fuelId, float add) {
        for (int i = 0; i < wasteEntries.size(); i++) {
            WasteEntry e = wasteEntries.get(i);
            if (e.id().equals(fuelId)) {
                wasteEntries.set(i, new WasteEntry(fuelId, e.units() + add));
                return;
            }
        }
        wasteEntries.add(new WasteEntry(fuelId, add));
    }

    public int getMaxFuelUnitsTotal() {
        int perRod = ReactorRodBlockEntity.getMaxFuelUnits();
        long rods = (cachedRodPositions != null && cachedRodPositions.length > 0)
                ? cachedRodPositions.length
                : (cachedResult != null && cachedResult.valid() ? cachedResult.rodCount() : 0);
        long total = rods * (long) Math.max(0, perRod);
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, total));
    }

    public float addFuel(Identifier fuelId, float units) {
        if (units <= 0) return 0;
        float total = getTotalFuelAndWasteUnits();
        int max = getMaxFuelUnitsTotal();
        float add = Math.min(units, Math.max(0, max - total));
        if (add <= 0) return 0;
        mergeFuel(fuelId, add);
        setChanged();
        updateRodVisualFillIfNeeded();
        return add;
    }

    public float consumeFuel(Identifier fuelId, float units) {
        if (units <= 0) return 0;
        for (int i = 0; i < fuelEntries.size(); i++) {
            FuelEntry e = fuelEntries.get(i);
            if (!e.id().equals(fuelId)) continue;
            float take = Math.min(units, e.units());
            if (take <= 0) return 0;
            float remain = e.units() - take;
            if (remain <= 0.0001f) fuelEntries.remove(i);
            else fuelEntries.set(i, new FuelEntry(fuelId, remain));
            setChanged();
            updateRodVisualFillIfNeeded();
            return take;
        }
        return 0;
    }

    private void mergeFuel(Identifier fuelId, float add) {
        for (int i = 0; i < fuelEntries.size(); i++) {
            FuelEntry e = fuelEntries.get(i);
            if (e.id().equals(fuelId)) {
                fuelEntries.set(i, new FuelEntry(fuelId, e.units() + add));
                return;
            }
        }
        fuelEntries.add(new FuelEntry(fuelId, add));
    }

    /** Called from block tick when ON: updates rod model states incrementally (bounded cost). */
    public void tickRodVisuals(ServerLevel level) {
        if (cachedRodPositions == null || cachedRodPositions.length == 0) return;
        int maxPerTick = 256;
        int n = Math.min(maxPerTick, cachedRodPositions.length);
        for (int i = 0; i < n; i++) {
            long lp = cachedRodPositions[(rodVisualUpdateCursor++) % cachedRodPositions.length];
            BlockPos pos = BlockPos.of(lp);
            BlockState state = level.getBlockState(pos);
            if (!state.is(ModBlocks.REACTOR_ROD.get())) continue;
            if (!state.hasProperty(net.unfamily.colossal_reactors.block.ReactorRodBlock.FILL)) continue;
            int cur = state.getValue(net.unfamily.colossal_reactors.block.ReactorRodBlock.FILL);
            if (cur != cachedRodFillLevel) {
                level.setBlock(pos, state.setValue(net.unfamily.colossal_reactors.block.ReactorRodBlock.FILL, cachedRodFillLevel), Block.UPDATE_CLIENTS);
            }
        }
    }

    private void updateRodVisualFillIfNeeded() {
        int max = getMaxFuelUnitsTotal();
        float pct = max <= 0 ? 0f : Math.max(0f, Math.min(1f, getTotalFuelAndWasteUnits() / (float) max));
        int level = ReactorRodBlockEntity.fillPercentToLevelForController(pct);
        if (cachedRodFillLevel != level) {
            cachedRodFillLevel = level;
            rodVisualUpdateCursor = 0;
        }
    }

    private void clampToNewCapacities() {
        clampSharedFuelAndWasteToCapacity();
        clampCoolantToCapacity();
        updateRodVisualFillIfNeeded();
    }

    /** Clamp shared (fuel + waste) units to capacity; drops the most voluminous entry first. */
    private void clampSharedFuelAndWasteToCapacity() {
        int max = getMaxFuelUnitsTotal();
        float excess = getTotalFuelAndWasteUnits() - max;
        if (excess <= 0.0001f) return;
        while (excess > 0.0001f) {
            int fuelIdx = -1;
            float fuelMax = 0;
            for (int i = 0; i < fuelEntries.size(); i++) {
                float u = fuelEntries.get(i).units();
                if (u > fuelMax) { fuelMax = u; fuelIdx = i; }
            }
            int wasteIdx = -1;
            float wasteMax = 0;
            for (int i = 0; i < wasteEntries.size(); i++) {
                float u = wasteEntries.get(i).units();
                if (u > wasteMax) { wasteMax = u; wasteIdx = i; }
            }
            if (fuelIdx < 0 && wasteIdx < 0) break;

            boolean dropWaste = (wasteIdx >= 0) && (fuelIdx < 0 || wasteMax >= fuelMax);
            if (dropWaste) {
                WasteEntry e = wasteEntries.get(wasteIdx);
                float take = Math.min(e.units(), excess);
                float left = e.units() - take;
                excess -= take;
                if (left <= 0.0001f) wasteEntries.remove(wasteIdx);
                else wasteEntries.set(wasteIdx, new WasteEntry(e.id(), left));
            } else {
                FuelEntry e = fuelEntries.get(fuelIdx);
                float take = Math.min(e.units(), excess);
                float left = e.units() - take;
                excess -= take;
                if (left <= 0.0001f) fuelEntries.remove(fuelIdx);
                else fuelEntries.set(fuelIdx, new FuelEntry(e.id(), left));
            }
        }
        setChanged();
    }

    private void clampCoolantToCapacity() {
        int max = getCoolantCapacityMbTotal();
        int total = getTotalCoolantMb();
        int excess = total - max;
        if (excess <= 0) return;
        for (int i = coolantEntries.size() - 1; i >= 0 && excess > 0; i--) {
            CoolantEntry e = coolantEntries.get(i);
            int take = Math.min(e.mb(), excess);
            int left = e.mb() - take;
            excess -= take;
            if (left <= 0) coolantEntries.remove(i);
            else coolantEntries.set(i, new CoolantEntry(e.fluidId(), left));
        }
        setChanged();
    }

    // waste clamp handled by clampSharedFuelAndWasteToCapacity()

    public void rebuildPartCaches(ServerLevel level, ReactorValidation.Result result) {
        if (level == null || result == null || !result.valid()) {
            cachedRodPositions = new long[0];
            cachedPowerPortPositions = new long[0];
            cachedResourcePortPositions = new long[0];
            cachedRedstonePortPositions = new long[0];
            cachedEffectiveRodCount = 0.0;
            cachedHeatSinkStaticCache = null;
            cachedRodFillLevel = 0;
            rodVisualUpdateCursor = 0;
            return;
        }

        int minX = result.minX();
        int minY = result.minY();
        int minZ = result.minZ();
        int maxX = result.maxX();
        int maxY = result.maxY();
        int maxZ = result.maxZ();

        LongArrayList rods = new LongArrayList();
        LongArrayList powerPorts = new LongArrayList();
        LongArrayList resourcePorts = new LongArrayList();
        LongArrayList redstonePorts = new LongArrayList();

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    p.set(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (state.is(ModBlocks.REACTOR_ROD.get())) {
                        rods.add(p.asLong());
                    } else if (state.is(ModBlocks.POWER_PORT.get())) {
                        powerPorts.add(p.asLong());
                    } else if (state.is(ModBlocks.RESOURCE_PORT.get())) {
                        resourcePorts.add(p.asLong());
                    } else if (state.is(ModBlocks.REDSTONE_PORT.get())) {
                        redstonePorts.add(p.asLong());
                    }
                }
            }
        }

        cachedRodPositions = rods.toLongArray();
        cachedPowerPortPositions = powerPorts.toLongArray();
        cachedResourcePortPositions = resourcePorts.toLongArray();
        cachedRedstonePortPositions = redstonePorts.toLongArray();

        rebuildSimulationCaches(level, result, cachedRodPositions);

        updateRodVisualFillIfNeeded();
    }

    private void rebuildSimulationCaches(ServerLevel level, ReactorValidation.Result result, long[] rodPositions) {
        // Precompute sets for fast adjacency checks
        LongOpenHashSet rodSet = new LongOpenHashSet(Math.max(16, rodPositions.length * 2));
        for (long p : rodPositions) rodSet.add(p);

        // Effective rod count: sum penalty by horizontal adjacency to rod or shell
        double penalty = net.unfamily.colossal_reactors.Config.ROD_ADJACENCY_PENALTY.get();
        double effective = 0.0;
        for (long p : rodPositions) {
            BlockPos pos = BlockPos.of(p);
            int adjacentCount = 0;
            for (var dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
                BlockPos neighbor = pos.relative(dir);
                long nl = neighbor.asLong();
                if (rodSet.contains(nl) || ReactorValidation.isShellBlock(level.getBlockState(neighbor))) {
                    adjacentCount++;
                }
            }
            effective += Math.max(0.0, 1.0 - penalty * adjacentCount);
        }
        cachedEffectiveRodCount = effective;

        // Heat sink static cache: scan interior once and classify adjacency to rod
        int minX = result.minX(), minY = result.minY(), minZ = result.minZ();
        int maxX = result.maxX(), maxY = result.maxY(), maxZ = result.maxZ();
        var reg = level.registryAccess();
        int countRod = 0, countAdj = 0, countNon = 0;
        double sumFuelAdj = 0, sumEnergyAdj = 0, sumOverheatingAdj = 0;
        double sumFuelNon = 0, sumEnergyNon = 0, sumOverheatingNon = 0;

        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        for (int x = minX + 1; x < maxX; x++) {
            for (int y = minY + 1; y < maxY; y++) {
                for (int z = minZ + 1; z < maxZ; z++) {
                    mp.set(x, y, z);
                    BlockState state = level.getBlockState(mp);
                    if (state.is(ModBlocks.REACTOR_ROD.get())) {
                        countRod++;
                        continue;
                    }
                    HeatSinkLoader.HeatSinkModifiers m = HeatSinkLoader.getModifiersForBlockOrDefault(state, reg);
                    boolean adjacentToRod = false;
                    // 6-neighbor check via rodSet on longs (avoid mutating mp)
                    for (var d : net.minecraft.core.Direction.values()) {
                        int nx = x + d.getStepX();
                        int ny = y + d.getStepY();
                        int nz = z + d.getStepZ();
                        if (rodSet.contains(BlockPos.asLong(nx, ny, nz))) {
                            adjacentToRod = true;
                            break;
                        }
                    }
                    if (adjacentToRod) {
                        sumFuelAdj += m.fuelMultiplier();
                        sumEnergyAdj += m.energyMultiplier();
                        sumOverheatingAdj += m.overheatingMultiplier();
                        countAdj++;
                    } else {
                        sumFuelNon += m.fuelMultiplier();
                        sumEnergyNon += m.energyMultiplier();
                        sumOverheatingNon += m.overheatingMultiplier();
                        countNon++;
                    }
                }
            }
        }

        cachedHeatSinkStaticCache = new HeatSinkStaticCache(
                countRod, countAdj, countNon,
                sumFuelAdj, sumEnergyAdj, sumOverheatingAdj,
                sumFuelNon, sumEnergyNon, sumOverheatingNon
        );

        // Reactor shape may have changed (e.g. resized): keep contents but clamp to new capacities.
        clampToNewCapacities();
    }

    public void invalidateCache() {
        cachedResult = null;
        cachedRodPositions = new long[0];
        cachedPowerPortPositions = new long[0];
        cachedResourcePortPositions = new long[0];
        cachedRedstonePortPositions = new long[0];
        cachedEffectiveRodCount = 0.0;
        cachedHeatSinkStaticCache = null;
        cachedRodFillLevel = 0;
        rodVisualUpdateCursor = 0;
        if (level != null && !level.isClientSide()) {
            setChanged();
        }
    }

    /** Called by ReactorSimulation.tick at end of each tick. Fuel hundredths = fuel units/tick * 100 (e.g. 0.26 -> 26). */
    public void setLastTickStats(long rfPerTick, int steamPerTick, int waterPerTick, int fuelPerTickHundredths) {
        this.lastRfPerTick = rfPerTick;
        this.lastSteamPerTick = steamPerTick;
        this.lastWaterPerTick = waterPerTick;
        this.lastFuelPerTickHundredths = fuelPerTickHundredths;
    }

    public long getLastRfPerTick() { return lastRfPerTick; }
    public int getLastSteamPerTick() { return lastSteamPerTick; }
    public int getLastWaterPerTick() { return lastWaterPerTick; }
    public int getLastFuelPerTickHundredths() { return lastFuelPerTickHundredths; }

    /** Stability in permille 0–1000 (display as 0.0%–100.0%). */
    public int getStabilityPermille() { return stabilityPermille; }
    public void setStabilityPermille(int permille) {
        this.stabilityPermille = Math.max(0, Math.min(1000, permille));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.reactor_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorControllerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (cachedResult != null && cachedResult.valid()) {
            output.putInt("val_minX", cachedResult.minX());
            output.putInt("val_minY", cachedResult.minY());
            output.putInt("val_minZ", cachedResult.minZ());
            output.putInt("val_maxX", cachedResult.maxX());
            output.putInt("val_maxY", cachedResult.maxY());
            output.putInt("val_maxZ", cachedResult.maxZ());
            output.putInt("val_rodCount", cachedResult.rodCount());
            output.putInt("val_rodColumns", cachedResult.rodColumns());
            output.putInt("val_coolantCount", cachedResult.coolantCount());
        }
        output.putInt("stability", stabilityPermille);

        if (!fuelEntries.isEmpty()) {
            output.store(TAG_FUEL, Codec.list(FUEL_ENTRY_CODEC), List.copyOf(fuelEntries));
        }
        if (!wasteEntries.isEmpty()) {
            output.store(TAG_WASTE, Codec.list(WASTE_ENTRY_CODEC), List.copyOf(wasteEntries));
        }
        if (!coolantEntries.isEmpty()) {
            output.store(TAG_COOLANT, Codec.list(COOLANT_ENTRY_CODEC), List.copyOf(coolantEntries));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Optional<Integer> minX = input.getInt("val_minX");
        if (minX.isPresent()) {
            cachedResult = new ReactorValidation.Result(
                    true,
                    minX.get(),
                    input.getIntOr("val_minY", 0),
                    input.getIntOr("val_minZ", 0),
                    input.getIntOr("val_maxX", 0),
                    input.getIntOr("val_maxY", 0),
                    input.getIntOr("val_maxZ", 0),
                    input.getIntOr("val_rodCount", 0),
                    input.getIntOr("val_rodColumns", 0),
                    input.getIntOr("val_coolantCount", 0)
            );
        } else {
            cachedResult = null;
        }
        input.getInt("stability").ifPresent(v -> {
            stabilityPermille = (v <= 100) ? Math.min(1000, v * 10) : Math.max(0, Math.min(1000, v));
        });

        fuelEntries.clear();
        for (FuelEntry e : input.listOrEmpty(TAG_FUEL, FUEL_ENTRY_CODEC)) {
            if (e != null && e.id() != null && e.units() > 0) fuelEntries.add(e);
        }
        wasteEntries.clear();
        for (WasteEntry e : input.listOrEmpty(TAG_WASTE, WASTE_ENTRY_CODEC)) {
            if (e != null && e.id() != null && e.units() > 0) wasteEntries.add(e);
        }
        coolantEntries.clear();
        for (CoolantEntry e : input.listOrEmpty(TAG_COOLANT, COOLANT_ENTRY_CODEC)) {
            if (e != null && e.fluidId() != null && e.mb() > 0) coolantEntries.add(e);
        }
        clampToNewCapacities();
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
