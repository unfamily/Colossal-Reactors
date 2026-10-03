package net.unfamily.colossal_reactors.compat.jei;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import org.jetbrains.annotations.Nullable;

/** JEI/EMI/REI card for one fuel; {@link #recipeId} is the datapack RecipeHolder id when known. */
public record FuelJeiRecipe(FuelDefinition definition, @Nullable ResourceLocation recipeId) {

    public static FuelJeiRecipe of(FuelDefinition definition, @Nullable ResourceLocation recipeId) {
        return new FuelJeiRecipe(definition, recipeId);
    }

    public static List<FuelJeiRecipe> wrapAll(List<FuelDefinition> defs) {
        return defs.stream().map(d -> of(d, null)).toList();
    }
}
