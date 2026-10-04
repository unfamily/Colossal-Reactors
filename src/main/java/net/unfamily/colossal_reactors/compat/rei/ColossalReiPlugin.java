package net.unfamily.colossal_reactors.compat.rei;

import java.util.Set;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry;
import me.shedaniel.rei.api.client.registry.display.DisplayRegistry;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.client.gui.MelterScreen;
import net.unfamily.colossal_reactors.compat.RecipeViewerVisibility;

public class ColossalReiPlugin implements REIClientPlugin {
    public static final Identifier EPHEMERAL_SERIALIZER_ID =
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "ephemeral_display");

    @Override
    public void registerCategories(CategoryRegistry registry) {
        ColossalReiCategories.register(registry);
    }

    @Override
    public void registerScreens(ScreenRegistry registry) {
        registry.registerContainerClickArea(
                new Rectangle(
                        MelterScreen.getProgressBarX(),
                        MelterScreen.getProgressBarY(),
                        MelterScreen.getProgressBarWidth(),
                        MelterScreen.getProgressBarHeight()),
                MelterScreen.class,
                ColossalReiCategories.MELTER);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        Set<Item> visible = RecipeViewerVisibility.visibleItems();
        registry.removeEntryIf(stack -> {
            Identifier id = stack.getIdentifier();
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

}
