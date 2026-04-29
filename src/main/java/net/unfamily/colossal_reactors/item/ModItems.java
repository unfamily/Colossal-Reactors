package net.unfamily.colossal_reactors.item;

import java.util.ArrayList;
import java.util.List;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.HeatingCoilBlock;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ColossalReactors.MODID);

    /** Heating coil items: OFF and ON per coil (for registration). */
    public static final List<DeferredItem<BlockItem>> HEATING_COIL_ITEMS = new ArrayList<>();
    /** Heating coil OFF items only (for creative tab; ON items are not shown there). */
    public static final List<DeferredItem<BlockItem>> HEATING_COIL_OFF_ITEMS = new ArrayList<>();

    public static final DeferredItem<BlockItem> REACTOR_GLASS = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_GLASS);
    public static final DeferredItem<BlockItem> REACTOR_CASING = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_CASING);
    public static final DeferredItem<BlockItem> REACTOR_CONTROLLER = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_CONTROLLER);
    public static final DeferredItem<BlockItem> REACTOR_ROD = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_ROD);
    public static final DeferredItem<BlockItem> POWER_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.POWER_PORT);
    public static final DeferredItem<BlockItem> REDSTONE_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.REDSTONE_PORT);
    public static final DeferredItem<BlockItem> RESOURCE_PORT = ITEMS.registerSimpleBlockItem(ModBlocks.RESOURCE_PORT);
    public static final DeferredItem<BlockItem> MELTER = ITEMS.registerSimpleBlockItem(ModBlocks.MELTER);
    public static final DeferredItem<BlockItem> RADIATION_SCRUBBER = ITEMS.registerSimpleBlockItem(ModBlocks.RADIATION_SCRUBBER);
    public static final DeferredItem<BlockItem> ROD_CONTROLLER = ITEMS.registerSimpleBlockItem(ModBlocks.ROD_CONTROLLER);
    public static final DeferredItem<BlockItem> REACTOR_BUILDER = ITEMS.registerSimpleBlockItem(ModBlocks.REACTOR_BUILDER);

    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.registerSimpleItem("uranium_ingot");
    public static final DeferredItem<Item> NUCLEAR_WASTE = ITEMS.registerSimpleItem("nuclear_waste");
    public static final DeferredItem<Item> CATALYST_BREEZIUM = ITEMS.registerSimpleItem("catalyst_breezium");
    public static final DeferredItem<Item> RADIATION_CURE = ITEMS.registerItem("radiation_cure", RadiationCureItem::new,
            p -> p.stacksTo(16));

    // Resource block items
    public static final DeferredItem<BlockItem> URANIUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.URANIUM_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_URANIUM_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_URANIUM_ORE);
    public static final DeferredItem<BlockItem> LEAD_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.LEAD_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_LEAD_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_LEAD_ORE);
    public static final DeferredItem<BlockItem> BORON_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.BORON_ORE);
    public static final DeferredItem<BlockItem> DEEPSLATE_BORON_ORE = ITEMS.registerSimpleBlockItem(ModBlocks.DEEPSLATE_BORON_ORE);
    public static final DeferredItem<BlockItem> URANIUM_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.URANIUM_BLOCK);
    public static final DeferredItem<BlockItem> URANIUM_RAW_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.URANIUM_RAW_BLOCK);
    public static final DeferredItem<BlockItem> LEAD_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.LEAD_BLOCK);
    public static final DeferredItem<BlockItem> RAW_LEAD_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.RAW_LEAD_BLOCK);
    public static final DeferredItem<BlockItem> BORON_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.BORON_BLOCK);
    public static final DeferredItem<BlockItem> RAW_BORON_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.RAW_BORON_BLOCK);
    public static final DeferredItem<BlockItem> GRAPHITE_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.GRAPHITE_BLOCK);
    public static final DeferredItem<BlockItem> AZURITE_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.AZURITE_BLOCK);
    public static final DeferredItem<BlockItem> TOUGH_ALLOY_BLOCK = ITEMS.registerSimpleBlockItem(ModBlocks.TOUGH_ALLOY_BLOCK);

    // Raw materials and ingots
    public static final DeferredItem<Item> RAW_URANIUM = ITEMS.registerSimpleItem("raw_uranium");
    public static final DeferredItem<Item> LEAD_RAW = ITEMS.registerSimpleItem("lead_raw");
    public static final DeferredItem<Item> BORON_RAW = ITEMS.registerSimpleItem("boron_raw");
    public static final DeferredItem<Item> BORON_INGOT = ITEMS.registerSimpleItem("boron_ingot");
    public static final DeferredItem<Item> GRAPHITE_INGOT = ITEMS.registerSimpleItem("graphite_ingot");
    public static final DeferredItem<Item> AZURITE_INGOT = ITEMS.registerSimpleItem("azurite_ingot");
    public static final DeferredItem<Item> LEAD_INGOT = ITEMS.registerSimpleItem("lead_ingot");
    public static final DeferredItem<Item> TOUGH_ALLOY_INGOT = ITEMS.registerSimpleItem("tough_alloy_ingot");
    public static final DeferredItem<Item> UNREFINED_TOUGH_ALLOY = ITEMS.registerSimpleItem("unrefined_tough_alloy");

    // Dusts (for crusher/enriching integration)
    public static final DeferredItem<Item> URANIUM_DUST = ITEMS.registerSimpleItem("uranium_dust");
    public static final DeferredItem<Item> LEAD_DUST = ITEMS.registerSimpleItem("lead_dust");
    public static final DeferredItem<Item> BORON_DUST = ITEMS.registerSimpleItem("boron_dust");

    static {
        List<Identifier> coilIds = HeatingCoilRegistry.getBuiltinCoilIds();
        for (int i = 0; i < coilIds.size(); i++) {
            DeferredBlock<HeatingCoilBlock> off = ModBlocks.HEATING_COIL_BLOCKS.get(i * 2);
            DeferredBlock<HeatingCoilBlock> on = ModBlocks.HEATING_COIL_BLOCKS.get(i * 2 + 1);
            DeferredItem<BlockItem> offItem = ITEMS.registerSimpleBlockItem(off);
            HEATING_COIL_ITEMS.add(offItem);
            HEATING_COIL_OFF_ITEMS.add(offItem);
            HEATING_COIL_ITEMS.add(ITEMS.registerSimpleBlockItem(on));
        }
    }

    private ModItems() {}
}
