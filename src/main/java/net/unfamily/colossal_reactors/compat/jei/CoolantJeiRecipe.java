package net.unfamily.colossal_reactors.compat.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import org.jetbrains.annotations.Nullable;

/** One JEI row for a coolant definition on a single medium (liquid or gas). */
public record CoolantJeiRecipe(
        CoolantDefinition definition, JeiMedium medium, @Nullable ResourceLocation recipeId) {

    public CoolantJeiRecipe(CoolantDefinition definition, JeiMedium medium) {
        this(definition, medium, null);
    }

    /** Legacy synthetic id used only when {@link #recipeId} is null. */
    public ResourceLocation jeiId() {
        String suffix = medium == JeiMedium.LIQUID ? "_jei_liquid" : "_jei_gas";
        return ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, definition.coolantId().getPath() + suffix);
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

    public static List<CoolantJeiRecipe> expand(CoolantDefinition def, @Nullable ResourceLocation recipeId) {
        boolean liquidIn = def.inputs().stream().anyMatch(s -> !MaterialSelector.isChemicalPrefix(s));
        boolean gasIn = def.inputs().stream().anyMatch(MaterialSelector::isChemicalPrefix);
        boolean liquidOut = def.outputs().stream().anyMatch(s -> !MaterialSelector.isChemicalPrefix(s));
        boolean gasOut = def.outputs().stream().anyMatch(MaterialSelector::isChemicalPrefix);

        List<CoolantJeiRecipe> out = new ArrayList<>();
        if (liquidIn) {
            out.add(new CoolantJeiRecipe(def, JeiMedium.LIQUID, recipeId));
        }
        if (gasIn && (JeiIngredientsHelper.jeiChemicalsAvailable() || !liquidIn)) {
            out.add(new CoolantJeiRecipe(def, JeiMedium.GAS, recipeId));
        }
        if (out.isEmpty()) {
            if (liquidOut || !gasOut) {
                out.add(new CoolantJeiRecipe(def, JeiMedium.LIQUID, recipeId));
            } else {
                out.add(new CoolantJeiRecipe(def, JeiMedium.GAS, recipeId));
            }
        }
        return out;
    }
}
