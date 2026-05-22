package net.unfamily.colossal_reactors.compat.jei;

import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;

import java.util.ArrayList;
import java.util.List;

/** One JEI row for a turbine generation definition on a single medium (liquid or gas). */
public record TurbineJeiRecipe(TurbineGenerationDefinition definition, JeiMedium medium) {

    public ResourceLocation jeiId() {
        String suffix = medium == JeiMedium.LIQUID ? "_jei_liquid" : "_jei_gas";
        return ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, definition.generationId().getPath() + suffix);
    }

    public List<String> inputSelectors() {
        return definition.inputs().stream().filter(medium::matchesSelector).toList();
    }

    public List<String> outputSelectors() {
        return definition.outputs().stream().filter(medium::matchesSelector).toList();
    }

    public static List<TurbineJeiRecipe> expand(TurbineGenerationDefinition def) {
        boolean liquidIn = def.inputs().stream().anyMatch(s -> !MaterialSelector.isChemicalPrefix(s));
        boolean gasIn = def.inputs().stream().anyMatch(MaterialSelector::isChemicalPrefix);
        boolean liquidOut = def.outputs().stream().anyMatch(s -> !MaterialSelector.isChemicalPrefix(s));
        boolean gasOut = def.outputs().stream().anyMatch(MaterialSelector::isChemicalPrefix);

        List<TurbineJeiRecipe> out = new ArrayList<>();
        if (liquidIn || liquidOut) {
            out.add(new TurbineJeiRecipe(def, JeiMedium.LIQUID));
        }
        if ((gasIn || gasOut) && (JeiIngredientsHelper.jeiChemicalsAvailable() || !liquidIn && !liquidOut)) {
            out.add(new TurbineJeiRecipe(def, JeiMedium.GAS));
        }
        if (out.isEmpty()) {
            out.add(new TurbineJeiRecipe(def, JeiMedium.LIQUID));
        }
        return out;
    }
}
