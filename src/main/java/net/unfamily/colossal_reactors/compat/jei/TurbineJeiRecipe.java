package net.unfamily.colossal_reactors.compat.jei;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import org.jetbrains.annotations.Nullable;

/**
 * One JEI card per turbine-generation RecipeManager entry. Liquid and gas share the same
 * slots (JEI cycles both media under one registry id).
 */
public record TurbineJeiRecipe(
        TurbineGenerationDefinition definition, @Nullable ResourceLocation recipeId) {

    public TurbineJeiRecipe(TurbineGenerationDefinition definition) {
        this(definition, null);
    }

    public ResourceLocation jeiId() {
        return ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, definition.generationId().getPath());
    }

    public List<String> liquidInputs() {
        return definition.inputs().stream().filter(s -> !MaterialSelector.isChemicalPrefix(s)).toList();
    }

    public List<String> gasInputs() {
        return definition.inputs().stream().filter(MaterialSelector::isChemicalPrefix).toList();
    }

    public List<String> liquidOutputs() {
        return definition.outputs().stream().filter(s -> !MaterialSelector.isChemicalPrefix(s)).toList();
    }

    public List<String> gasOutputs() {
        return definition.outputs().stream().filter(MaterialSelector::isChemicalPrefix).toList();
    }

    public static List<TurbineJeiRecipe> expand(TurbineGenerationDefinition def) {
        return expand(def, null);
    }

    public static List<TurbineJeiRecipe> expand(
            TurbineGenerationDefinition def, @Nullable ResourceLocation recipeId) {
        return List.of(new TurbineJeiRecipe(def, recipeId));
    }
}
