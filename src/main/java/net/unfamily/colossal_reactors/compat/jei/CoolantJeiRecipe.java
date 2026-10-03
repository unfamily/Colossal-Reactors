package net.unfamily.colossal_reactors.compat.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import org.jetbrains.annotations.Nullable;

/** One JEI row for a coolant definition on a single medium (liquid or gas). */
public record CoolantJeiRecipe(
        CoolantDefinition definition, JeiMedium medium, @Nullable Identifier recipeId) {

    public CoolantJeiRecipe(CoolantDefinition definition, JeiMedium medium) {
        this(definition, medium, null);
    }

    public Identifier jeiId() {
        String suffix = medium == JeiMedium.LIQUID ? "_jei_liquid" : "_jei_gas";
        return Identifier.fromNamespaceAndPath(ColossalReactors.MODID, definition.coolantId().getPath() + suffix);
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

    public static List<CoolantJeiRecipe> expand(CoolantDefinition def) {
        return expand(def, null);
    }

    public static List<CoolantJeiRecipe> expand(CoolantDefinition def, @Nullable Identifier recipeId) {
        boolean liquidIn = def.inputs().stream().anyMatch(s -> s == null || !s.startsWith("%"));
        boolean gasIn = def.inputs().stream().anyMatch(s -> s != null && s.startsWith("%"));
        boolean liquidOut = def.outputs().stream().anyMatch(s -> s == null || !s.startsWith("%"));
        boolean gasOut = def.outputs().stream().anyMatch(s -> s != null && s.startsWith("%"));

        List<CoolantJeiRecipe> out = new ArrayList<>();
        // Same RecipeManager / KubeJS id on every medium card (no synthetic /gas ids).
        if (liquidIn || liquidOut) {
            out.add(new CoolantJeiRecipe(def, JeiMedium.LIQUID, recipeId));
        }
        if ((gasIn || gasOut) && (JeiIngredientsHelper.jeiChemicalsAvailable() || !liquidIn && !liquidOut)) {
            out.add(new CoolantJeiRecipe(def, JeiMedium.GAS, recipeId));
        }
        if (out.isEmpty()) {
            out.add(new CoolantJeiRecipe(def, JeiMedium.LIQUID, recipeId));
        }
        return out;
    }
}
