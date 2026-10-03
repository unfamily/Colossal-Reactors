package net.unfamily.colossal_reactors.compat.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import org.jetbrains.annotations.Nullable;

/** One JEI row for a turbine generation definition on a single medium (liquid or gas). */
public record TurbineJeiRecipe(
        TurbineGenerationDefinition definition, JeiMedium medium, @Nullable Identifier recipeId) {

    public TurbineJeiRecipe(TurbineGenerationDefinition definition, JeiMedium medium) {
        this(definition, medium, null);
    }

    public Identifier jeiId() {
        String suffix = medium == JeiMedium.LIQUID ? "_jei_liquid" : "_jei_gas";
        return Identifier.fromNamespaceAndPath(
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
            TurbineGenerationDefinition def, @Nullable Identifier recipeId) {
        boolean liquidIn = def.inputs().stream().anyMatch(s -> s == null || !s.startsWith("%"));
        boolean gasIn = def.inputs().stream().anyMatch(s -> s != null && s.startsWith("%"));
        boolean liquidOut = def.outputs().stream().anyMatch(s -> s == null || !s.startsWith("%"));
        boolean gasOut = def.outputs().stream().anyMatch(s -> s != null && s.startsWith("%"));

        List<TurbineJeiRecipe> out = new ArrayList<>();
        // Same RecipeManager / KubeJS id on every medium card (no synthetic /gas ids).
        if (liquidIn || liquidOut) {
            out.add(new TurbineJeiRecipe(def, JeiMedium.LIQUID, recipeId));
        }
        if ((gasIn || gasOut) && (JeiIngredientsHelper.jeiChemicalsAvailable() || !liquidIn && !liquidOut)) {
            out.add(new TurbineJeiRecipe(def, JeiMedium.GAS, recipeId));
        }
        if (out.isEmpty()) {
            out.add(new TurbineJeiRecipe(def, JeiMedium.LIQUID, recipeId));
        }
        return out;
    }
}
