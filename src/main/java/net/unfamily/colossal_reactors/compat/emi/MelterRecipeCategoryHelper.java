package net.unfamily.colossal_reactors.compat.emi;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.unfamily.colossal_reactors.melter.MelterRecipe;

final class MelterRecipeCategoryHelper {
    private MelterRecipeCategoryHelper() {}

    static List<ItemStack> resolveInputs(MelterRecipe recipe, RegistryAccess registryAccess) {
        Identifier id = recipe.inputId();
        if (id == null) {
            return List.of();
        }
        int safeCount = Math.max(1, recipe.count());
        List<ItemStack> out = new ArrayList<>();
        if (recipe.inputIsTag()) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, id);
            registryAccess.lookup(Registries.ITEM).ifPresent(lookup ->
                    lookup.get(tagKey).ifPresent(holders ->
                            holders.forEach(h -> out.add(new ItemStack(h.value(), safeCount)))));
        } else {
            Item item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            if (item != null && item != Items.AIR) {
                out.add(new ItemStack(item, safeCount));
            }
        }
        return out;
    }
}
