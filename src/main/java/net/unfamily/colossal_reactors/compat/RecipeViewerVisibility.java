package net.unfamily.colossal_reactors.compat;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.item.ModCreativeModeTabs;
import net.unfamily.colossal_reactors.item.ModItems;
import org.jetbrains.annotations.Nullable;

/**
 * Items shown in recipe viewers should match the Colossal creative tab.
 */
public final class RecipeViewerVisibility {
    private RecipeViewerVisibility() {}

    /** Items present in {@link ModCreativeModeTabs#COLOSSAL_REACTORS_TAB}, or a safe fallback. */
    public static Set<Item> visibleItems() {
        Set<Item> fromTab = fromCreativeTab();
        if (!fromTab.isEmpty()) {
            return fromTab;
        }
        return fallbackVisibleItems();
    }

    private static Set<Item> fromCreativeTab() {
        Set<Item> out = new HashSet<>();
        CreativeModeTab tab = ModCreativeModeTabs.COLOSSAL_REACTORS_TAB.get();
        HolderLookup.Provider holders = holders();
        if (holders != null) {
            FeatureFlagSet flags = featureFlags();
            tab.buildContents(new CreativeModeTab.ItemDisplayParameters(flags, true, holders));
        }
        for (ItemStack stack : tab.getDisplayItems()) {
            if (!stack.isEmpty()) {
                out.add(stack.getItem());
            }
        }
        for (ItemStack stack : tab.getSearchTabDisplayItems()) {
            if (!stack.isEmpty()) {
                out.add(stack.getItem());
            }
        }
        return out;
    }

    /** When the tab cannot be built yet, still hide heating-coil ON variants. */
    private static Set<Item> fallbackVisibleItems() {
        Set<Item> onCoils = new HashSet<>();
        for (var item : ModItems.HEATING_COIL_ITEMS) {
            onCoils.add(item.get());
        }
        for (var item : ModItems.HEATING_COIL_OFF_ITEMS) {
            onCoils.remove(item.get());
        }
        Set<Item> visible = new HashSet<>();
        for (var item : ModItems.ITEMS.getEntries()) {
            Item value = item.get();
            if (!onCoils.contains(value)) {
                visible.add(value);
            }
        }
        return visible;
    }

    @Nullable
    private static HolderLookup.Provider holders() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.registryAccess();
        }
        ClientPacketListener connection = mc.getConnection();
        return connection != null ? connection.registryAccess() : null;
    }

    private static FeatureFlagSet featureFlags() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.enabledFeatures() : FeatureFlags.DEFAULT_FLAGS;
    }
}
