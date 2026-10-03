package net.unfamily.colossal_reactors.compat.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import org.jetbrains.annotations.Nullable;

/** One JEI row for a turbine generation definition on a single medium (liquid or gas). */
public record TurbineJeiRecipe(
        TurbineGenerationDefinition definition, JeiMedium medium, @Nullable ResourceLocation recipeId) {

    public TurbineJeiRecipe(TurbineGenerationDefinition definition, JeiMedium medium) {
        this(definition, medium, null);
    }

    public ResourceLocation jeiId() {
        String suffix = medium == JeiMedium.LIQUID ? "_jei_liquid" : "_jei_gas";
        return ResourceLocation.fromNamespaceAndPath(
                ColossalReactors.MODID, definition.generationId().getPath() + suffix);
    }

    public String mediumCollisionSuffix() {
        return medium == JeiMedium.LIQUID ? "liquid" : "gas";
    }

    public List<String> inputSelectors() {
        return definition.inputs().stream().filter(medium::matchesSelector).toList();
    }

    public List<String> outputSelectors() {
        return definition.outputs().stream().filter(medium::matchesSelector).toList();
    }

    public static List<TurbineJeiRecipe> expand(TurbineGenerationDefinition def) {
        return expand(def, null);
    }

    public static List<TurbineJeiRecipe> expand(
            TurbineGenerationDefinition def, @Nullable ResourceLocation recipeId) {
        boolean liquidIn = def.inputs().stream().anyMatch(s -> !MaterialSelector.isChemicalPrefix(s));
        boolean gasIn = def.inputs().stream().anyMatch(MaterialSelector::isChemicalPrefix);
        boolean liquidOut = def.outputs().stream().anyMatch(s -> !MaterialSelector.isChemicalPrefix(s));
        boolean gasOut = def.outputs().stream().anyMatch(MaterialSelector::isChemicalPrefix);

        List<TurbineJeiRecipe> out = new ArrayList<>();
        // Same RecipeManager / KubeJS id on every medium card (no synthetic /gas ids).
        if (liquidIn) {
            out.add(new TurbineJeiRecipe(def, JeiMedium.LIQUID, recipeId));
        }
        if (gasIn && (JeiIngredientsHelper.jeiChemicalsAvailable() || !liquidIn)) {
            out.add(new TurbineJeiRecipe(def, JeiMedium.GAS, recipeId));
        }
        if (out.isEmpty()) {
            if (liquidOut || !gasOut) {
                out.add(new TurbineJeiRecipe(def, JeiMedium.LIQUID, recipeId));
            } else {
                out.add(new TurbineJeiRecipe(def, JeiMedium.GAS, recipeId));
            }
        }
        return out;
    }
}
