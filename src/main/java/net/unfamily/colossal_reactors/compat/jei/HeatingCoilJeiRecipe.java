package net.unfamily.colossal_reactors.compat.jei;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.unfamily.colossal_reactors.compat.ViewerRecipeIds;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilDefinition;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;
import org.jetbrains.annotations.Nullable;

/**
 * One JEI entry for a heating coil: a specific consume option (alternative) for a coil definition.
 * Display / KubeJS ids match EMI/REI: {@code ns:heating_coils_<coilPath>_<optionIndex>}.
 */
public record HeatingCoilJeiRecipe(
        Identifier coilId,
        int durationTicks,
        int optionIndex,
        ConsumeOption option,
        @Nullable Identifier recipeId
) {

    public HeatingCoilJeiRecipe(
            Identifier coilId, int durationTicks, int optionIndex, ConsumeOption option) {
        this(coilId, durationTicks, optionIndex, option, null);
    }

    public static List<HeatingCoilJeiRecipe> expand(HeatingCoilDefinition def) {
        return expand(def, null);
    }

    public static List<HeatingCoilJeiRecipe> expand(
            HeatingCoilDefinition def, @Nullable Identifier recipeId) {
        List<ConsumeOption> opts = def.consume();
        if (opts == null || opts.isEmpty()) {
            return List.of();
        }
        List<HeatingCoilJeiRecipe> out = new ArrayList<>();
        for (int i = 0; i < opts.size(); i++) {
            ConsumeOption jeiOpt = sanitizeForJei(opts.get(i));
            if (jeiOpt != null && !jeiOpt.isEmpty()) {
                // One option per RecipeManager holder → keep holder id.
                // Multiple options on one holder → same EMI/REI progressive ids for every viewer.
                Identifier cardId = opts.size() == 1
                        ? recipeId
                        : ViewerRecipeIds.heatingCoilDisplayLocation(null, def.id(), i);
                if (cardId == null) {
                    cardId = ViewerRecipeIds.heatingCoilDisplayLocation(null, def.id(), i);
                }
                out.add(new HeatingCoilJeiRecipe(def.id(), def.duration(), i, jeiOpt, cardId));
            }
        }
        return out;
    }

    @Nullable
    private static ConsumeOption sanitizeForJei(ConsumeOption opt) {
        if (opt.chemical() == null) {
            return opt;
        }
        if (!MekChemicalHelper.isGasSupportEnabled() || !MekChemicalHelper.isLoaded()) {
            return withoutChemical(opt);
        }
        if (!MaterialSelector.isChemicalPrefix(opt.chemical().selector())) {
            return withoutChemical(opt);
        }
        return opt;
    }

    @Nullable
    private static ConsumeOption withoutChemical(ConsumeOption opt) {
        ConsumeOption stripped = new ConsumeOption(opt.fluid(), null, opt.item(), opt.energy(), opt.burnable());
        return stripped.isEmpty() ? null : stripped;
    }
}
