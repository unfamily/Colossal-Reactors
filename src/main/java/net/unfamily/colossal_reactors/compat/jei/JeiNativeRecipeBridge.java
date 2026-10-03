package net.unfamily.colossal_reactors.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.crafting.ColossalJsonRecipe;
import net.unfamily.colossal_reactors.crafting.ModColossalRecipes;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilDefinition;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilLoader;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilRegistry;
import net.unfamily.colossal_reactors.heatsink.HeatSinkDefinition;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.melter.MelterHeatEntry;
import net.unfamily.colossal_reactors.melter.MelterHeatsLoader;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import net.unfamily.colossal_reactors.turbine.ElecCoilDefinition;
import net.unfamily.colossal_reactors.turbine.ElecCoilLoader;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;
import org.jetbrains.annotations.Nullable;

/**
 * Builds JEI/EMI/REI recipe lists from native {@link RecipeHolder} ids when RecipeManager is available.
 */
public final class JeiNativeRecipeBridge {
    private JeiNativeRecipeBridge() {}

    @Nullable
    private static RecipeManager recipeManager() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return mc.level.getRecipeManager();
        }
        if (mc.getConnection() != null) {
            return mc.getConnection().getRecipeManager();
        }
        return null;
    }

    private static List<RecipeHolder<ColossalJsonRecipe>> holders(RecipeType<ColossalJsonRecipe> type) {
        RecipeManager manager = recipeManager();
        if (manager == null) {
            return List.of();
        }
        return List.copyOf(manager.getAllRecipesFor(type));
    }

    public static List<FuelJeiRecipe> fuels() {
        List<FuelJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.FUEL.get())) {
            FuelDefinition def = FuelLoader.parseEntry(holder.value().json(), holder.id().toString(), true);
            if (def != null) {
                out.add(FuelJeiRecipe.of(def, holder.id()));
            }
        }
        return out.isEmpty() ? FuelJeiRecipe.wrapAll(FuelLoader.getVisibleDefinitions()) : out;
    }

    public static List<CoolantJeiRecipe> coolants() {
        List<CoolantJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.COOLANT.get())) {
            CoolantDefinition def = CoolantLoader.parseEntry(holder.value().json(), holder.id().toString(), true);
            if (def != null) {
                out.addAll(CoolantJeiRecipe.expand(def, holder.id()));
            }
        }
        if (out.isEmpty()) {
            return CoolantLoader.getVisibleDefinitions().stream()
                    .flatMap(def -> CoolantJeiRecipe.expand(def).stream())
                    .toList();
        }
        return out;
    }

    public static List<HeatSinkJeiRecipe> heatSinks() {
        List<HeatSinkJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.HEAT_SINKS.get())) {
            HeatSinkDefinition def = HeatSinkLoader.parseEntry(holder.value().json(), holder.id().toString());
            if (def != null) {
                out.add(HeatSinkJeiRecipe.of(def, holder.id()));
            }
        }
        return out.isEmpty() ? HeatSinkJeiRecipe.wrapAll(HeatSinkLoader.getAllDefinitions()) : out;
    }

    public static List<MelterJeiRecipe> melterRecipes() {
        List<MelterJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.MELTER_RECIPES.get())) {
            MelterRecipe r = MelterRecipesLoader.parseEntry(holder.value().json(), holder.id().toString());
            if (r != null) {
                out.add(MelterJeiRecipe.of(r, holder.id()));
            }
        }
        return out.isEmpty() ? MelterJeiRecipe.wrapAll(MelterRecipesLoader.getAll()) : out;
    }

    public static List<MelterHeatJeiRecipe> melterHeats() {
        List<MelterHeatJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.MELTER_HEATS.get())) {
            ResourceLocation holderId = holder.id();
            List<MelterHeatEntry> list = MelterHeatsLoader.parseFromRoot(holder.value().json(), holderId.toString());
            if (list == null || list.isEmpty()) {
                continue;
            }
            // Always the RecipeManager / KubeJS id — never invent path_N that is not a holder key.
            for (int i = 0; i < list.size(); i++) {
                out.add(MelterHeatJeiRecipe.of(list.get(i), holderId, i));
            }
        }
        return out.isEmpty() ? MelterHeatJeiRecipe.wrapAll(MelterHeatsLoader.getAll()) : out;
    }

    public static List<ElecCoilJeiRecipe> elecCoils() {
        List<ElecCoilJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.ELEC_COILS.get())) {
            ElecCoilDefinition def = ElecCoilLoader.parseEntry(holder.value().json(), holder.id().toString());
            if (def != null) {
                out.add(ElecCoilJeiRecipe.of(def, holder.id()));
            }
        }
        return out.isEmpty() ? ElecCoilJeiRecipe.wrapAll(ElecCoilLoader.getJeIDefinitions()) : out;
    }

    public static List<TurbineJeiRecipe> turbineGeneration() {
        List<TurbineJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.TURBINE_GENERATION.get())) {
            TurbineGenerationDefinition def =
                    TurbineGenerationLoader.parseEntry(holder.value().json(), holder.id().toString(), true);
            if (def != null) {
                out.addAll(TurbineJeiRecipe.expand(def, holder.id()));
            }
        }
        if (out.isEmpty()) {
            return TurbineGenerationLoader.getJeIDefinitions().stream()
                    .flatMap(def -> TurbineJeiRecipe.expand(def).stream())
                    .toList();
        }
        return out;
    }

    public static List<HeatingCoilJeiRecipe> heatingCoils() {
        List<HeatingCoilJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.HEATING_COILS.get())) {
            ResourceLocation id = holder.id();
            for (HeatingCoilDefinition def :
                    HeatingCoilLoader.parseFromRoot(holder.value().json(), id.toString())) {
                out.addAll(HeatingCoilJeiRecipe.expand(def, id));
            }
        }
        if (out.isEmpty()) {
            return HeatingCoilRegistry.getAll().values().stream()
                    .flatMap(def -> HeatingCoilJeiRecipe.expand(def).stream())
                    .filter(Objects::nonNull)
                    .toList();
        }
        return out.stream().filter(Objects::nonNull).toList();
    }
}
