package net.unfamily.colossal_reactors.compat.jei;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import org.jetbrains.annotations.Nullable;

public record HeatSinkJeiRecipe(HeatSinkDefinition definition, @Nullable ResourceLocation recipeId) {

    public static HeatSinkJeiRecipe of(HeatSinkDefinition definition, @Nullable ResourceLocation recipeId) {
        return new HeatSinkJeiRecipe(definition, recipeId);
    }

    public static List<HeatSinkJeiRecipe> wrapAll(List<HeatSinkDefinition> defs) {
        return defs.stream().map(d -> of(d, null)).toList();
    }
}
