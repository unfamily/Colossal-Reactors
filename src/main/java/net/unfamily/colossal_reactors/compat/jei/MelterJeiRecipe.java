package net.unfamily.colossal_reactors.compat.jei;

import java.util.List;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import org.jetbrains.annotations.Nullable;

public record MelterJeiRecipe(MelterRecipe definition, @Nullable Identifier recipeId) {

    public static MelterJeiRecipe of(MelterRecipe definition, @Nullable Identifier recipeId) {
        return new MelterJeiRecipe(definition, recipeId);
    }

    public static List<MelterJeiRecipe> wrapAll(List<MelterRecipe> defs) {
        return defs.stream().map(d -> of(d, null)).toList();
    }
}
