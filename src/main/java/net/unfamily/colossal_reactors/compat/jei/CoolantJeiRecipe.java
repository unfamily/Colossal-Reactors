package net.unfamily.colossal_reactors.compat.jei;

import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;

import java.util.ArrayList;
import java.util.List;

/** One JEI row for a coolant definition on a single medium (liquid or gas). */
public record CoolantJeiRecipe(CoolantDefinition definition, JeiMedium medium) {

    public Identifier jeiId() {
        String suffix = medium == JeiMedium.LIQUID ? "_jei_liquid" : "_jei_gas";
        return Identifier.fromNamespaceAndPath(ColossalReactors.MODID, definition.coolantId().getPath() + suffix);
    }

    public List<String> inputSelectors() {
        return definition.inputs().stream().filter(medium::matchesSelector).toList();
    }

    public List<String> outputSelectors() {
        return definition.outputs().stream().filter(medium::matchesSelector).toList();
    }

    public static List<CoolantJeiRecipe> expand(CoolantDefinition def) {
        boolean liquidIn = def.inputs().stream().anyMatch(s -> s == null || !s.startsWith("%"));
        boolean gasIn = def.inputs().stream().anyMatch(s -> s != null && s.startsWith("%"));
        boolean liquidOut = def.outputs().stream().anyMatch(s -> s == null || !s.startsWith("%"));
        boolean gasOut = def.outputs().stream().anyMatch(s -> s != null && s.startsWith("%"));

        List<CoolantJeiRecipe> out = new ArrayList<>();
        if (liquidIn || liquidOut) {
            out.add(new CoolantJeiRecipe(def, JeiMedium.LIQUID));
        }
        if ((gasIn || gasOut) && (JeiIngredientsHelper.jeiChemicalsAvailable() || !liquidIn && !liquidOut)) {
            out.add(new CoolantJeiRecipe(def, JeiMedium.GAS));
        }
        if (out.isEmpty()) {
            out.add(new CoolantJeiRecipe(def, JeiMedium.LIQUID));
        }
        return out;
    }
}
