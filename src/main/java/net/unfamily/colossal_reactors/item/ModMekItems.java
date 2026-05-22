package net.unfamily.colossal_reactors.item;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * Mekanism processing chain items (shards, crystals, dirty dusts). Registered only when Mekanism is loaded.
 */
public final class ModMekItems {

    public static final DeferredRegister.Items MEK_ITEMS =
            DeferredRegister.createItems(ColossalReactors.MODID);

    public static final DeferredItem<Item> BORON_CLUMP = MEK_ITEMS.registerSimpleItem("boron_clump");
    public static final DeferredItem<Item> CHROMIUM_CLUMP = MEK_ITEMS.registerSimpleItem("chromium_clump");
    public static final DeferredItem<Item> URANIUM_CLUMP = MEK_ITEMS.registerSimpleItem("uranium_clump");
    public static final DeferredItem<Item> BORON_SHARD = MEK_ITEMS.registerSimpleItem("boron_shard");
    public static final DeferredItem<Item> CHROMIUM_SHARD = MEK_ITEMS.registerSimpleItem("chromium_shard");
    public static final DeferredItem<Item> BORON_CRYSTAL = MEK_ITEMS.registerSimpleItem("boron_crystal");
    public static final DeferredItem<Item> CHROMIUM_CRYSTAL = MEK_ITEMS.registerSimpleItem("chromium_crystal");
    public static final DeferredItem<Item> BORON_DIRTY_DUST = MEK_ITEMS.registerSimpleItem("boron_dirty_dust");
    public static final DeferredItem<Item> CHROMIUM_DUST_DIRTY = MEK_ITEMS.registerSimpleItem("chromium_dust_dirty");
    public static final DeferredItem<Item> URANIUM_DUST_DIRTY = MEK_ITEMS.registerSimpleItem("uranium_dust_dirty");

    private ModMekItems() {}

    public static void addToCreative(net.minecraft.world.item.CreativeModeTab.Output output) {
        output.accept(BORON_CLUMP.get());
        output.accept(CHROMIUM_CLUMP.get());
        output.accept(URANIUM_CLUMP.get());
        output.accept(BORON_SHARD.get());
        output.accept(CHROMIUM_SHARD.get());
        output.accept(BORON_CRYSTAL.get());
        output.accept(CHROMIUM_CRYSTAL.get());
        output.accept(BORON_DIRTY_DUST.get());
        output.accept(CHROMIUM_DUST_DIRTY.get());
        output.accept(URANIUM_DUST_DIRTY.get());
    }
}
