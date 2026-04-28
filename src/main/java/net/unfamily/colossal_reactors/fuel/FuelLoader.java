package net.unfamily.colossal_reactors.fuel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.ReactorRodBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads fuel definitions from datapack JSON: data/colossal_reactors/reactor_fuel/*.json.
 * Each file is one definition (fuel_id, inputs, output, units_per_fuel, units_per_waste, etc.).
 * Internal defaults (uranium, azurite) are applied first; datapack entries override by fuel_id.
 */
public class FuelLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(FuelLoader.class);

    private static final String KEY_FUEL_ID = "fuel_id";
    private static final String KEY_INPUTS = "inputs";
    private static final String KEY_UNITS_PER_ITEM = "units_per_item";
    private static final String KEY_UNITS_PER_FUEL = "units_per_fuel";
    private static final String KEY_UNITS_PER_WASTE = "units_per_waste";
    private static final String KEY_BASE_RF_PER_TICK = "base_rf_per_tick";
    private static final String KEY_BASE_FUEL_UNITS_PER_TICK = "base_fuel_units_per_tick";
    private static final String KEY_BASE_MB_PER_TICK_LEGACY = "base_mb_per_tick";
    private static final String KEY_OUTPUT = "output";
    private static final String KEY_OVERWRITABLE = "overwritable";

    private static final Map<Identifier, FuelDefinition> DEFINITIONS = new HashMap<>();

    /**
     * Applies loaded datapack data: clears, registers internal defaults, then merges in loaded map (later overwrites by fuel_id).
     */
    public static void applyLoaded(Map<Identifier, FuelDefinition> loaded) {
        DEFINITIONS.clear();
        registerInternalDefaults();
        if (loaded != null) {
            for (FuelDefinition def : loaded.values()) {
                processEntry(def);
            }
        }
    }

    private static void registerInternalDefaults() {
        Identifier uraniumId = ReactorRodBlockEntity.URANIUM_FUEL_ID;
        int unitsPerFuel = 1000;
        int unitsPerWaste = 1000;
        double baseRf = 200.0;
        double baseFuelUnitsPerTick = 0.03;
        List<String> inputs = List.of("#c:ingots/uranium");
        String output = ColossalReactors.MODID + ":nuclear_waste";
        DEFINITIONS.put(uraniumId, new FuelDefinition(uraniumId, inputs, output, unitsPerFuel, unitsPerWaste, baseRf, baseFuelUnitsPerTick, true));

        // Azurite: 500 base RF, 500 fuel units per ingot, 1500 consumed units per 1 waste (nuclear_waste)
        Identifier azuriteId = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "azurite");
        DEFINITIONS.put(azuriteId, new FuelDefinition(
                azuriteId,
                List.of("#c:ingots/azurite"),
                ColossalReactors.MODID + ":nuclear_waste",
                500,
                1500,
                500.0,
                baseFuelUnitsPerTick,
                true
        ));
    }

    /** Parses a single fuel definition from JSON (one file = one entry). Used by datapack reload listener. */
    public static FuelDefinition parseEntry(JsonObject json, String sourcePath, boolean defaultOverwritable) {
        if (!json.has(KEY_FUEL_ID)) {
            LOGGER.warn("Fuel entry in {}: missing 'fuel_id'", sourcePath);
            return null;
        }
        Identifier fuelId = Identifier.tryParse(json.get(KEY_FUEL_ID).getAsString());
        if (fuelId == null) {
            LOGGER.warn("Fuel entry in {}: invalid fuel_id", sourcePath);
            return null;
        }
        List<String> inputs = new ArrayList<>();
        if (json.has(KEY_INPUTS) && json.get(KEY_INPUTS).isJsonArray()) {
            for (JsonElement i : json.getAsJsonArray(KEY_INPUTS)) {
                if (i.isJsonPrimitive()) inputs.add(i.getAsString());
            }
        }
        String output = json.has(KEY_OUTPUT) ? json.get(KEY_OUTPUT).getAsString() : "";
        int unitsPerFuel = 1000;
        int unitsPerWaste = 1000;
        if (json.has(KEY_UNITS_PER_FUEL)) unitsPerFuel = json.get(KEY_UNITS_PER_FUEL).getAsInt();
        else if (json.has(KEY_UNITS_PER_ITEM)) unitsPerFuel = json.get(KEY_UNITS_PER_ITEM).getAsInt();
        if (json.has(KEY_UNITS_PER_WASTE)) unitsPerWaste = json.get(KEY_UNITS_PER_WASTE).getAsInt();
        else if (json.has(KEY_UNITS_PER_ITEM)) unitsPerWaste = json.get(KEY_UNITS_PER_ITEM).getAsInt();
        double baseRf = json.has(KEY_BASE_RF_PER_TICK) ? json.get(KEY_BASE_RF_PER_TICK).getAsDouble() : 200.0;
        double baseFuelUnitsPerTick = json.has(KEY_BASE_FUEL_UNITS_PER_TICK) ? json.get(KEY_BASE_FUEL_UNITS_PER_TICK).getAsDouble()
                : json.has(KEY_BASE_MB_PER_TICK_LEGACY) ? json.get(KEY_BASE_MB_PER_TICK_LEGACY).getAsDouble()
                : 0.03;
        boolean overwritable = json.has(KEY_OVERWRITABLE) ? json.get(KEY_OVERWRITABLE).getAsBoolean() : defaultOverwritable;
        return new FuelDefinition(fuelId, inputs.isEmpty() ? List.of(fuelId.toString()) : List.copyOf(inputs), output, unitsPerFuel, unitsPerWaste, baseRf, baseFuelUnitsPerTick, overwritable);
    }

    private static void processEntry(FuelDefinition def) {
        FuelDefinition existing = DEFINITIONS.get(def.fuelId());
        if (existing != null && !existing.overwritable()) {
            LOGGER.debug("Skipping fuel {}: existing definition is not overwritable", def.fuelId());
            return;
        }
        DEFINITIONS.put(def.fuelId(), def);
    }

    public static FuelDefinition get(Identifier fuelId) {
        return DEFINITIONS.get(fuelId);
    }

    public static Map<Identifier, FuelDefinition> getAll() {
        return new HashMap<>(DEFINITIONS);
    }

    /** No longer used (disable removed); kept for API compatibility. */
    public static boolean isInputExcluded(String inputSelector) {
        return false;
    }

    /**
     * Finds the fuel definition that matches the given item (by item id or item tag). Returns null if excluded or no match.
     * Prefers exact item id match over tag match so e.g. uranium_ingot gets the uranium definition (with correct unitsPerFuel).
     */
    @Nullable
    public static FuelDefinition getDefinitionForItem(ItemStack stack, RegistryAccess registryAccess) {
        if (stack == null || stack.isEmpty()) return null;
        Item item = stack.getItem();
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        FuelDefinition tagMatch = null;
        for (FuelDefinition def : DEFINITIONS.values()) {
            for (String input : def.inputs()) {
                if (isInputExcluded(input)) continue;
                if (input.startsWith("#")) {
                    Identifier tagId = Identifier.tryParse(input.substring(1));
                    if (tagId == null) continue;
                    TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
                    if (stack.is(tagKey)) tagMatch = def;
                } else {
                    if (Identifier.tryParse(input).equals(itemId)) return def;
                }
            }
        }
        return tagMatch;
    }

    /**
     * Returns a single item stack for the first valid input of this fuel type (for eject: convert fuel units back to items).
     * Caller must use definition's unitsPerFuel when converting fuel units back to item count.
     */
    public static ItemStack getFirstInputStack(Identifier fuelId, RegistryAccess registryAccess) {
        FuelDefinition def = DEFINITIONS.get(fuelId);
        if (def == null || def.inputs().isEmpty()) return ItemStack.EMPTY;
        for (String input : def.inputs()) {
            if (isInputExcluded(input)) continue;
            if (input.startsWith("#")) {
                Identifier tagId = Identifier.tryParse(input.substring(1));
                if (tagId == null) continue;
                TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
                var optItem = registryAccess.lookup(Registries.ITEM)
                        .flatMap(l -> l.get(tagKey))
                        .stream()
                        .flatMap(holders -> holders.stream())
                        .findFirst()
                        .map(h -> h.value());
                if (optItem.isPresent()) return new ItemStack(optItem.get(), 1);
            } else {
                Identifier id = Identifier.tryParse(input);
                if (id != null) {
                    Item item = BuiltInRegistries.ITEM.get(id).map(h -> h.value()).orElse(net.minecraft.world.item.Items.AIR);
                    if (item != null && item != net.minecraft.world.item.Items.AIR) return new ItemStack(item, 1);
                }
            }
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns a single item stack for the waste output of this fuel type (for tooltip display).
     * Output is either "#tag" (first item in tag) or "namespace:item_id".
     */
    public static ItemStack getFirstOutputStack(Identifier fuelId, RegistryAccess registryAccess) {
        FuelDefinition def = DEFINITIONS.get(fuelId);
        if (def == null) return ItemStack.EMPTY;
        String output = def.output();
        if (output == null || output.isEmpty()) return ItemStack.EMPTY;
        if (output.startsWith("#")) {
            Identifier tagId = Identifier.tryParse(output.substring(1));
            if (tagId == null) return ItemStack.EMPTY;
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
            var optItem = registryAccess.lookup(Registries.ITEM)
                    .flatMap(l -> l.get(tagKey))
                    .stream()
                    .flatMap(holders -> holders.stream())
                    .findFirst()
                    .map(h -> h.value());
            if (optItem.isPresent()) return new ItemStack(optItem.get(), 1);
        } else {
            Identifier id = Identifier.tryParse(output);
            if (id != null) {
                Item item = BuiltInRegistries.ITEM.get(id).map(h -> h.value()).orElse(net.minecraft.world.item.Items.AIR);
                if (item != null && item != net.minecraft.world.item.Items.AIR) return new ItemStack(item, 1);
            }
        }
        return ItemStack.EMPTY;
    }
}
