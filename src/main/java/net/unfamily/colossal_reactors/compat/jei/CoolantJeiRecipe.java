package net.unfamily.colossal_reactors.compat.jei;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import org.jetbrains.annotations.Nullable;

/**
 * One JEI card per coolant RecipeManager entry. Liquid and gas share the same slots
 * (JEI cycles both media under one registry id).
 */
public record CoolantJeiRecipe(CoolantDefinition definition, @Nullable ResourceLocation recipeId) {

    public CoolantJeiRecipe(CoolantDefinition definition) {
        this(definition, null);
    }

    public ResourceLocation jeiId() {
        return ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, definition.coolantId().getPath());
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

    public static List<CoolantJeiRecipe> expand(CoolantDefinition def) {
        return expand(def, null);
    }

    public static List<CoolantJeiRecipe> expand(CoolantDefinition def, @Nullable ResourceLocation recipeId) {
        return List.of(new CoolantJeiRecipe(def, recipeId));
    }
}
