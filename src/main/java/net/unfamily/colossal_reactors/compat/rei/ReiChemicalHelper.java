package net.unfamily.colossal_reactors.compat.rei;

import java.util.ArrayList;
import java.util.List;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.api.common.entry.type.EntryType;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.ModList;
import net.unfamily.colossal_reactors.compat.jei.JeiIngredientsHelper;

/**
 * Mek chemical entry stacks for REI via {@code reimekanismintegration} entry type
 * {@code reimekanismintegration:chemical}.
 */
public final class ReiChemicalHelper {
    private static final String BRIDGE_MOD_ID = "reimekanismintegration";
    private static final Identifier CHEMICAL_TYPE_ID =
            Identifier.fromNamespaceAndPath(BRIDGE_MOD_ID, "chemical");

    private ReiChemicalHelper() {}

    public static boolean isBridgeLoaded() {
        return ModList.get().isLoaded(BRIDGE_MOD_ID);
    }

    public static boolean canShowChemicals() {
        return isBridgeLoaded() && JeiIngredientsHelper.jeiChemicalsAvailable();
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void addChemicals(List<EntryIngredient> target, List<String> selectors) {
        if (target == null) {
            return;
        }
        List<EntryStack<?>> entries = new ArrayList<>();
        appendChemicalEntries(entries, selectors);
        if (!entries.isEmpty()) {
            target.add(EntryIngredient.of(entries));
        }
    }

    /** Append chemical entry stacks into an existing list (for shared liquid+gas slots). */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void appendChemicalEntries(List<EntryStack<?>> entries, List<String> selectors) {
        if (entries == null || selectors == null || selectors.isEmpty() || !canShowChemicals()) {
            return;
        }
        EntryType type = EntryType.deferred(CHEMICAL_TYPE_ID);
        for (Object stack : JeiIngredientsHelper.getChemicalStacks(selectors)) {
            if (stack == null) {
                continue;
            }
            try {
                entries.add(EntryStack.of(type, stack));
            } catch (Throwable ignored) {
                // Entry type not registered yet or wrong stack class.
            }
        }
    }
}
