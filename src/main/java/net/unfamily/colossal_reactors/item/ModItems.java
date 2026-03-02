package net.unfamily.colossal_reactors.item;

import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ColossalReactors.MODID);

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

    private ModItems() {}
}
