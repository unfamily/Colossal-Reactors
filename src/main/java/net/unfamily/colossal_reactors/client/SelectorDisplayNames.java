package net.unfamily.colossal_reactors.client;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.unfamily.colossal_reactors.compat.jei.JeiIngredientsHelper;

import java.util.List;

/** Resolves datapack selectors to in-world item/fluid display names for builder GUIs. */
public final class SelectorDisplayNames {

    private SelectorDisplayNames() {}

    public static Component fromSelector(String selector, RegistryAccess registryAccess) {
        if (registryAccess == null) {
            return Component.literal(selector != null ? selector : "?");
        }
        return JeiIngredientsHelper.displayNameForSelector(selector, registryAccess);
    }

    public static Component fromFirstSelector(List<String> selectors, RegistryAccess registryAccess) {
        if (registryAccess == null) {
            return Component.literal("?");
        }
        return JeiIngredientsHelper.displayNameForFirstSelector(selectors, registryAccess);
    }
}
