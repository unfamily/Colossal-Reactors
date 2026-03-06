package net.unfamily.colossal_reactors.item;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilRegistry;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ColossalReactors.MODID);

    /** Heating coil items (only _off block, one per coil id); filled in static block. */
    public static final List<DeferredItem<BlockItem>> HEATING_COIL_ITEMS = new ArrayList<>();

    public static final DeferredItem<BlockItem> REACTOR_GLASS = ITEMS.register("reactor_glass",
            () -> new BlockItem(ModBlocks.REACTOR_GLASS.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> REACTOR_CASING = ITEMS.register("reactor_casing",
            () -> new BlockItem(ModBlocks.REACTOR_CASING.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> REACTOR_CONTROLLER = ITEMS.register("reactor_controller",
            () -> new BlockItem(ModBlocks.REACTOR_CONTROLLER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> REACTOR_ROD = ITEMS.register("reactor_rod",
            () -> new BlockItem(ModBlocks.REACTOR_ROD.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> POWER_PORT = ITEMS.register("power_port",
            () -> new BlockItem(ModBlocks.POWER_PORT.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> REDSTONE_PORT = ITEMS.register("redstone_port",
            () -> new BlockItem(ModBlocks.REDSTONE_PORT.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RESOURCE_PORT = ITEMS.register("resource_port",
            () -> new BlockItem(ModBlocks.RESOURCE_PORT.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> MELTER = ITEMS.register("melter",
            () -> new BlockItem(ModBlocks.MELTER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> ROD_CONTROLLER = ITEMS.register("rod_controller",
            () -> new BlockItem(ModBlocks.ROD_CONTROLLER.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> REACTOR_BUILDER = ITEMS.register("reactor_builder",
            () -> new BlockItem(ModBlocks.REACTOR_BUILDER.get(), new Item.Properties()));
    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.register("uranium_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> NUCLEAR_WASTE = ITEMS.register("nuclear_waste",
            () -> new Item(new Item.Properties()));

    // Resource block items
    public static final DeferredItem<BlockItem> URANIUM_ORE = ITEMS.register("uranium_ore",
            () -> new BlockItem(ModBlocks.URANIUM_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_URANIUM_ORE = ITEMS.register("deep_uranium_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_URANIUM_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LEAD_ORE = ITEMS.register("lead_ore",
            () -> new BlockItem(ModBlocks.LEAD_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_LEAD_ORE = ITEMS.register("deep_lead_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_LEAD_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> BORON_ORE = ITEMS.register("boron_ore",
            () -> new BlockItem(ModBlocks.BORON_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> DEEPSLATE_BORON_ORE = ITEMS.register("deep_boron_ore",
            () -> new BlockItem(ModBlocks.DEEPSLATE_BORON_ORE.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> URANIUM_BLOCK = ITEMS.register("uranium_block",
            () -> new BlockItem(ModBlocks.URANIUM_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> URANIUM_RAW_BLOCK = ITEMS.register("uranium_raw_block",
            () -> new BlockItem(ModBlocks.URANIUM_RAW_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> LEAD_BLOCK = ITEMS.register("lead_block",
            () -> new BlockItem(ModBlocks.LEAD_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RAW_LEAD_BLOCK = ITEMS.register("raw_lead_block",
            () -> new BlockItem(ModBlocks.RAW_LEAD_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> BORON_BLOCK = ITEMS.register("boron_block",
            () -> new BlockItem(ModBlocks.BORON_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> RAW_BORON_BLOCK = ITEMS.register("raw_boron_block",
            () -> new BlockItem(ModBlocks.RAW_BORON_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> GRAPHITE_BLOCK = ITEMS.register("graphite_block",
            () -> new BlockItem(ModBlocks.GRAPHITE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> AZURITE_BLOCK = ITEMS.register("azurite_block",
            () -> new BlockItem(ModBlocks.AZURITE_BLOCK.get(), new Item.Properties()));
    public static final DeferredItem<BlockItem> TOUGH_ALLOY_BLOCK = ITEMS.register("tough_alloy_block",
            () -> new BlockItem(ModBlocks.TOUGH_ALLOY_BLOCK.get(), new Item.Properties()));

    // Raw materials and ingots
    public static final DeferredItem<Item> RAW_URANIUM = ITEMS.register("raw_uranium",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_RAW = ITEMS.register("lead_raw",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BORON_RAW = ITEMS.register("boron_raw",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BORON_INGOT = ITEMS.register("boron_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> GRAPHITE_INGOT = ITEMS.register("graphite_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> AZURITE_INGOT = ITEMS.register("azurite_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.register("lead_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> TOUGH_ALLOY_INGOT = ITEMS.register("tough_alloy_ingot",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> UNREFINED_TOUGH_ALLOY = ITEMS.register("unrefined_tough_alloy",
            () -> new Item(new Item.Properties()));

    // Dusts (for crusher/enriching integration)
    public static final DeferredItem<Item> URANIUM_DUST = ITEMS.register("uranium_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> LEAD_DUST = ITEMS.register("lead_dust",
            () -> new Item(new Item.Properties()));
    public static final DeferredItem<Item> BORON_DUST = ITEMS.register("boron_dust",
            () -> new Item(new Item.Properties()));

    static {
        for (ResourceLocation coilId : HeatingCoilRegistry.getBuiltinCoilIds()) {
            HEATING_COIL_ITEMS.add(ITEMS.register(coilId.getPath(),
                    () -> new BlockItem(ModBlocks.getHeatingCoilBlock(coilId, false), new Item.Properties())));
        }
    }

    private ModItems() {}
}
