package net.unfamily.colossal_reactors.compat.rei;

import java.util.Set;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.common.display.DisplaySerializerRegistry;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.compat.RecipeViewerVisibility;

@REIPluginClient
public class ColossalReiPlugin implements REIClientPlugin {

    @Override
    public void registerCategories(CategoryRegistry registry) {
        ColossalReiCategories.register(registry);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        Set<Item> visible = RecipeViewerVisibility.visibleItems();
        registry.removeEntryIf(stack -> {
            ResourceLocation id = stack.getIdentifier();
            if (id == null || !ColossalReactors.MODID.equals(id.getNamespace())) {
                return false;
            }
            Object value = stack.getValue();
            return value instanceof ItemStack itemStack && !visible.contains(itemStack.getItem());
        });
    }

    @Override
    public void registerDisplays(DisplayRegistry registry) {
        ColossalReiRecipeRegistrar.registerAll(registry);
        ReiDatapackRecipeSync.onRegistryAvailable(registry);
    }

    @Override
    public void registerDisplaySerializer(DisplaySerializerRegistry registry) {
        registry.registerNotSerializable(ColossalReiCategories.COOLANT);
        registry.registerNotSerializable(ColossalReiCategories.FUEL);
        registry.registerNotSerializable(ColossalReiCategories.HEAT_SINK);
        registry.registerNotSerializable(ColossalReiCategories.MELTER);
        registry.registerNotSerializable(ColossalReiCategories.MELTER_HEAT);
        registry.registerNotSerializable(ColossalReiCategories.HEATING_COIL);
        registry.registerNotSerializable(ColossalReiCategories.ELEC_COIL);
        registry.registerNotSerializable(ColossalReiCategories.TURBINE);
    }
}
