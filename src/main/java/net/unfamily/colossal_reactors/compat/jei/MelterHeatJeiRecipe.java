package net.unfamily.colossal_reactors.compat.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import org.jetbrains.annotations.Nullable;

public record MelterHeatJeiRecipe(
        MelterHeatEntry definition, @Nullable Identifier recipeId, int entryIndex) {

    public static MelterHeatJeiRecipe of(
            MelterHeatEntry definition, @Nullable Identifier recipeId, int entryIndex) {
        return new MelterHeatJeiRecipe(definition, recipeId, entryIndex);
    }

    public static List<MelterHeatJeiRecipe> wrapAll(List<MelterHeatEntry> defs) {
        List<MelterHeatJeiRecipe> out = new ArrayList<>(defs.size());
        for (int i = 0; i < defs.size(); i++) {
            out.add(of(defs.get(i), null, i));
        }
        return out;
    }
}
