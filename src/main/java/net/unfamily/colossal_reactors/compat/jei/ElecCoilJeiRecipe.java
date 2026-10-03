package net.unfamily.colossal_reactors.compat.jei;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.turbine.ElecCoilDefinition;
import org.jetbrains.annotations.Nullable;

public record ElecCoilJeiRecipe(ElecCoilDefinition definition, @Nullable ResourceLocation recipeId) {

    public static ElecCoilJeiRecipe of(ElecCoilDefinition definition, @Nullable ResourceLocation recipeId) {
        return new ElecCoilJeiRecipe(definition, recipeId);
    }

    public static List<ElecCoilJeiRecipe> wrapAll(List<ElecCoilDefinition> defs) {
        return defs.stream().map(d -> of(d, null)).toList();
    }
}
