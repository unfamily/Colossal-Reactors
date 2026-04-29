package net.unfamily.colossal_reactors.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.ReactorRodBlock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * BlockEntity for reactor rod.
 * <ul>
 *   <li>Fuel: array of (fuelTypeId, units). Shared max capacity (e.g. 100 uranium + 900 X = 100%). Uranium = colossal_reactors:uranium.</li>
 *   <li>Coolant: up to 10,000 mB of liquid, mixed fluid types (same structure as other mixed buffers).</li>
 *   <li>Solid waste buffer: same structure (id + count), capacity in total count.</li>
 *   <li>Liquid waste buffer: same structure as coolant (fluid id + amount), capacity in mB.</li>
 * </ul>
 */
public class ReactorRodBlockEntity extends BlockEntity {

    /** Default uranium fuel type id (colossal_reactors:uranium). */
    public static final Identifier URANIUM_FUEL_ID = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "uranium");

    private static final String TAG_FUEL = "Fuel";
    private static final String TAG_FUEL_ID = "Id";
    private static final String TAG_FUEL_UNITS = "Units";
    private static final String TAG_COOLANT = "Coolant";
    private static final String TAG_FLUID_ID = "FluidId";
    private static final String TAG_AMOUNT = "Amount";
    private static final String TAG_SOLID_WASTE = "SolidWaste";
    private static final String TAG_COUNT = "Count";
    private static final String TAG_LIQUID_WASTE = "LiquidWaste";
    private static final String TAG_WASTE_ACCUMULATOR = "WasteAccumulator";

    private static final int COOLANT_CAPACITY_MB = 10_000;
    private static final int SOLID_WASTE_CAPACITY = 1000;
    private static final int LIQUID_WASTE_CAPACITY_MB = 10_000;

    /** Fuel by type: id -> units (float). Total units must be <= getMaxFuelUnits(). */
    private final List<FuelEntry> fuelEntries = new ArrayList<>();
    /** Coolant: mixed fluids, total mB <= COOLANT_CAPACITY_MB. */
    private final List<FluidEntry> coolantEntries = new ArrayList<>();
    /** Solid waste: id -> count. Total count <= SOLID_WASTE_CAPACITY. */
    private final List<SolidWasteEntry> solidWasteEntries = new ArrayList<>();
    /** Liquid waste: same structure as coolant, total <= LIQUID_WASTE_CAPACITY_MB. */
    private final List<FluidEntry> liquidWasteEntries = new ArrayList<>();
    /** Accumulated consumed units per waste type (float, for fractional waste: 1000 units -> 1 item). */
    private final java.util.Map<Identifier, Float> wasteAccumulator = new java.util.HashMap<>();

    private static final Codec<FuelEntry> FUEL_ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf(TAG_FUEL_ID).forGetter(FuelEntry::id),
            Codec.FLOAT.fieldOf(TAG_FUEL_UNITS).forGetter(FuelEntry::units)
    ).apply(i, FuelEntry::new));

    private static final Codec<FluidEntry> FLUID_ENTRY_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf(TAG_FLUID_ID).forGetter(e -> BuiltInRegistries.FLUID.getKey(e.fluid())),
            Codec.INT.fieldOf(TAG_AMOUNT).forGetter(FluidEntry::amount)
    ).apply(i, (id, amt) -> {
        Fluid f = BuiltInRegistries.FLUID.getValue(id);
        return new FluidEntry(f != null ? f : Fluids.EMPTY, amt);
    }));

    private static final Codec<SolidWasteEntry> SOLID_WASTE_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf(TAG_FUEL_ID).forGetter(SolidWasteEntry::id),
            Codec.INT.fieldOf(TAG_COUNT).forGetter(SolidWasteEntry::count)
    ).apply(i, SolidWasteEntry::new));

    private static final Codec<Map<Identifier, Float>> WASTE_ACC_CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.FLOAT);

    public record FuelEntry(Identifier id, float units) {}
    public record FluidEntry(Fluid fluid, int amount) {}
    public record SolidWasteEntry(Identifier id, int count) {}

    public ReactorRodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_ROD_BE.get(), pos, state);
    }

    // ---------- Fuel (array, shared max capacity) ----------

    public List<FuelEntry> getFuelEntries() {
        return List.copyOf(fuelEntries);
    }

    public float getTotalFuelUnits() {
        return (float) fuelEntries.stream().mapToDouble(FuelEntry::units).sum();
    }

    public float getFuelUnits(Identifier fuelId) {
        return (float) fuelEntries.stream()
                .filter(e -> e.id().equals(fuelId))
                .mapToDouble(FuelEntry::units)
                .sum();
    }

    /** Max total fuel units per rod (from config). */
    public static int getMaxFuelUnits() {
        return Config.ROD_MAX_FUEL_UNITS.get();
    }

    /** Add units of a fuel type. Clamps to max total capacity; returns actual amount added. */
    public float addFuel(Identifier fuelId, float units) {
        if (units <= 0) return 0;
        float total = getTotalFuelUnits();
        int max = getMaxFuelUnits();
        float add = Math.min(units, max - total);
        if (add <= 0) return 0;
        mergeFuel(fuelId, add);
        setChanged();
        updateBlockState();
        return add;
    }

    /** Consumes up to `units` of the given fuel type. Returns the amount actually consumed. */
    public float consumeFuel(Identifier fuelId, float units) {
        if (units <= 0) return 0;
        for (int i = 0; i < fuelEntries.size(); i++) {
            if (fuelEntries.get(i).id().equals(fuelId)) {
                FuelEntry e = fuelEntries.get(i);
                float take = Math.min(units, e.units());
                if (take <= 0) return 0;
                float newUnits = e.units() - take;
                if (newUnits <= 0.0001f) {
                    fuelEntries.remove(i);
                } else {
                    fuelEntries.set(i, new FuelEntry(fuelId, newUnits));
                }
                setChanged();
                updateBlockState();
                return take;
            }
        }
        return 0;
    }

    /** Set units for a fuel type (replaces that type). Total is clamped to max. */
    public void setFuel(Identifier fuelId, float units) {
        fuelEntries.removeIf(e -> e.id().equals(fuelId));
        if (units > 0) {
            float totalOthers = getTotalFuelUnits();
            int max = getMaxFuelUnits();
            float allowed = Math.min(units, max - totalOthers);
            if (allowed > 0) {
                fuelEntries.add(new FuelEntry(fuelId, allowed));
            }
        }
        setChanged();
        updateBlockState();
    }

    private void mergeFuel(Identifier fuelId, float add) {
        for (int i = 0; i < fuelEntries.size(); i++) {
            if (fuelEntries.get(i).id().equals(fuelId)) {
                FuelEntry e = fuelEntries.get(i);
                int max = getMaxFuelUnits();
                float totalOthers = getTotalFuelUnits() - e.units();
                float newUnits = Math.min(e.units() + add, max - totalOthers);
                fuelEntries.set(i, new FuelEntry(fuelId, newUnits));
                return;
            }
        }
        fuelEntries.add(new FuelEntry(fuelId, add));
    }

    /** Fill level 0..1 for rendering (1 = full). */
    public float getFillLevel() {
        int max = getMaxFuelUnits();
        return max <= 0 ? 0f : (float) getTotalFuelUnits() / max;
    }

    // ---------- Coolant (mixed, 10k mB) ----------

    public List<FluidEntry> getCoolantEntries() {
        return List.copyOf(coolantEntries);
    }

    public static int getCoolantCapacityMb() {
        return COOLANT_CAPACITY_MB;
    }

    public int getTotalCoolantMb() {
        return coolantEntries.stream().mapToInt(FluidEntry::amount).sum();
    }

    /** Add coolant (mixed). Returns amount actually added. */
    public int addCoolant(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        int total = getTotalCoolantMb();
        int add = Math.min(amountMb, COOLANT_CAPACITY_MB - total);
        if (add <= 0) return 0;
        mergeFluid(coolantEntries, fluid, add);
        setChanged();
        return add;
    }

    /** Drains up to amountMb of the given fluid from coolant. Returns amount actually drained. */
    public int drainCoolant(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        int drained = 0;
        for (int i = coolantEntries.size() - 1; i >= 0 && drained < amountMb; i--) {
            FluidEntry e = coolantEntries.get(i);
            if (e.fluid() != fluid) continue;
            int take = Math.min(amountMb - drained, e.amount());
            if (take <= 0) continue;
            drained += take;
            int remain = e.amount() - take;
            if (remain <= 0) coolantEntries.remove(i);
            else coolantEntries.set(i, new FluidEntry(fluid, remain));
        }
        if (drained > 0) {
            setChanged();
            updateBlockState();
        }
        return drained;
    }

    // ---------- Solid waste (same structure: id + count) ----------

    public List<SolidWasteEntry> getSolidWasteEntries() {
        return List.copyOf(solidWasteEntries);
    }

    public static int getSolidWasteCapacity() {
        return SOLID_WASTE_CAPACITY;
    }

    public int getTotalSolidWasteCount() {
        return solidWasteEntries.stream().mapToInt(SolidWasteEntry::count).sum();
    }

    public int addSolidWaste(Identifier itemId, int count) {
        if (count <= 0) return 0;
        int total = getTotalSolidWasteCount();
        int add = Math.min(count, SOLID_WASTE_CAPACITY - total);
        if (add <= 0) return 0;
        mergeSolidWaste(itemId, add);
        setChanged();
        return add;
    }

    private void mergeSolidWaste(Identifier id, int add) {
        for (int i = 0; i < solidWasteEntries.size(); i++) {
            if (solidWasteEntries.get(i).id().equals(id)) {
                SolidWasteEntry e = solidWasteEntries.get(i);
                solidWasteEntries.set(i, new SolidWasteEntry(id, e.count() + add));
                return;
            }
        }
        solidWasteEntries.add(new SolidWasteEntry(id, add));
    }

    /**
     * Records consumed fuel units and adds solid waste when accumulation reaches unitsPerWaste.
     * E.g. unitsPerWaste=1000: every 1000 consumed units produce 1 waste item (remainder carried over).
     */
    public void recordConsumedAndAddWaste(Identifier wasteId, float consumedUnits, int unitsPerWaste) {
        if (consumedUnits <= 0 || unitsPerWaste <= 0) return;
        float total = wasteAccumulator.getOrDefault(wasteId, 0f) + consumedUnits;
        int wasteCount = (int) (total / unitsPerWaste);
        wasteAccumulator.put(wasteId, total - wasteCount * unitsPerWaste);
        if (wasteCount > 0) addSolidWaste(wasteId, wasteCount);
    }

    /** Takes up to count of the given waste item from this rod. Returns amount actually taken. */
    public int takeSolidWaste(Identifier itemId, int count) {
        if (count <= 0) return 0;
        for (int i = 0; i < solidWasteEntries.size(); i++) {
            SolidWasteEntry e = solidWasteEntries.get(i);
            if (!e.id().equals(itemId)) continue;
            int take = Math.min(count, e.count());
            if (take <= 0) return 0;
            int remain = e.count() - take;
            if (remain <= 0) solidWasteEntries.remove(i);
            else solidWasteEntries.set(i, new SolidWasteEntry(itemId, remain));
            setChanged();
            updateBlockState();
            return take;
        }
        return 0;
    }

    // ---------- Liquid waste (same structure as coolant) ----------

    public List<FluidEntry> getLiquidWasteEntries() {
        return List.copyOf(liquidWasteEntries);
    }

    public static int getLiquidWasteCapacityMb() {
        return LIQUID_WASTE_CAPACITY_MB;
    }

    public int getTotalLiquidWasteMb() {
        return liquidWasteEntries.stream().mapToInt(FluidEntry::amount).sum();
    }

    public int addLiquidWaste(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        int total = getTotalLiquidWasteMb();
        int add = Math.min(amountMb, LIQUID_WASTE_CAPACITY_MB - total);
        if (add <= 0) return 0;
        mergeFluid(liquidWasteEntries, fluid, add);
        setChanged();
        return add;
    }

    /** Drains up to amountMb of the given fluid from liquid waste. Returns amount actually drained. */
    public int drainLiquidWaste(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        int drained = 0;
        for (int i = liquidWasteEntries.size() - 1; i >= 0 && drained < amountMb; i--) {
            FluidEntry e = liquidWasteEntries.get(i);
            if (e.fluid() != fluid) continue;
            int take = Math.min(amountMb - drained, e.amount());
            if (take <= 0) continue;
            drained += take;
            int remain = e.amount() - take;
            if (remain <= 0) liquidWasteEntries.remove(i);
            else liquidWasteEntries.set(i, new FluidEntry(fluid, remain));
        }
        if (drained > 0) setChanged();
        return drained;
    }

    private void mergeFluid(List<FluidEntry> list, Fluid fluid, int add) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).fluid() == fluid) {
                FluidEntry e = list.get(i);
                list.set(i, new FluidEntry(fluid, e.amount() + add));
                return;
            }
        }
        list.add(new FluidEntry(fluid, add));
    }

    // ---------- Block state (fill level for model) ----------

    private static int fillPercentToLevel(float fillPercent) {
        if (fillPercent <= 0f) return 0;
        if (fillPercent >= 1f) return 12;
        int p = (int) (fillPercent * 100);
        if (p <= 5) return 1;
        if (p <= 10) return 2;
        if (p <= 20) return 3;
        if (p <= 30) return 4;
        if (p <= 40) return 5;
        if (p <= 50) return 6;
        if (p <= 60) return 7;
        if (p <= 70) return 8;
        if (p <= 80) return 9;
        if (p <= 90) return 10;
        return 11;
    }

    private void updateBlockState() {
        if (level == null) return;
        BlockState state = getBlockState();
        if (!(state.getBlock() instanceof ReactorRodBlock)) return;
        int fillLevel = fillPercentToLevel(getFillLevel());
        if (state.getValue(ReactorRodBlock.FILL) != fillLevel) {
            level.setBlock(worldPosition, state.setValue(ReactorRodBlock.FILL, fillLevel), Block.UPDATE_CLIENTS);
        }
    }

    private static final String TAG_FUEL_UNITS_LEGACY = "fuelUnits";

    // ---------- NBT ----------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(TAG_FUEL, Codec.list(FUEL_ENTRY_CODEC), List.copyOf(fuelEntries));
        output.store(TAG_COOLANT, Codec.list(FLUID_ENTRY_CODEC), coolantEntries.stream()
                .filter(e -> e.fluid() != null && e.fluid() != Fluids.EMPTY && e.amount() > 0)
                .toList());
        output.store(TAG_SOLID_WASTE, Codec.list(SOLID_WASTE_CODEC), List.copyOf(solidWasteEntries));
        output.store(TAG_LIQUID_WASTE, Codec.list(FLUID_ENTRY_CODEC), liquidWasteEntries.stream()
                .filter(e -> e.fluid() != null && e.fluid() != Fluids.EMPTY && e.amount() > 0)
                .toList());
        if (!wasteAccumulator.isEmpty()) {
            Map<Identifier, Float> positive = new java.util.HashMap<>();
            for (var e : wasteAccumulator.entrySet()) {
                if (e.getValue() != null && e.getValue() > 0) {
                    positive.put(e.getKey(), e.getValue());
                }
            }
            if (!positive.isEmpty()) {
                output.store(TAG_WASTE_ACCUMULATOR, WASTE_ACC_CODEC, positive);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuelEntries.clear();
        for (FuelEntry e : input.listOrEmpty(TAG_FUEL, FUEL_ENTRY_CODEC)) {
            fuelEntries.add(e);
        }
        if (fuelEntries.isEmpty()) {
            input.getInt(TAG_FUEL_UNITS_LEGACY).ifPresent(legacy -> {
                if (legacy > 0) {
                    fuelEntries.add(new FuelEntry(URANIUM_FUEL_ID, Math.min((float) legacy, getMaxFuelUnits())));
                }
            });
        }
        coolantEntries.clear();
        for (FluidEntry e : input.listOrEmpty(TAG_COOLANT, FLUID_ENTRY_CODEC)) {
            if (e.fluid() != Fluids.EMPTY && e.amount() > 0) {
                coolantEntries.add(e);
            }
        }
        solidWasteEntries.clear();
        for (SolidWasteEntry e : input.listOrEmpty(TAG_SOLID_WASTE, SOLID_WASTE_CODEC)) {
            solidWasteEntries.add(e);
        }
        liquidWasteEntries.clear();
        for (FluidEntry e : input.listOrEmpty(TAG_LIQUID_WASTE, FLUID_ENTRY_CODEC)) {
            if (e.fluid() != Fluids.EMPTY && e.amount() > 0) {
                liquidWasteEntries.add(e);
            }
        }
        wasteAccumulator.clear();
        input.read(TAG_WASTE_ACCUMULATOR, WASTE_ACC_CODEC).ifPresent(wasteAccumulator::putAll);
        updateBlockState();
    }
}
