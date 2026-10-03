package net.unfamily.colossal_reactors.compat.jei;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import net.unfamily.colossal_reactors.coolant.CoolantDefinition;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.crafting.ColossalJsonRecipe;
import net.unfamily.colossal_reactors.crafting.ModColossalRecipes;
import net.unfamily.colossal_reactors.fuel.FuelDefinition;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilDefinition;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilLoader;
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
 * Builds JEI recipe lists from native {@link RecipeHolder} ids when RecipeManager is available.
 */
public final class JeiNativeRecipeBridge {
    private JeiNativeRecipeBridge() {}

    @Nullable
    private static RecipeManager recipeManager() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server != null ? server.getRecipeManager() : null;
    }

    private static List<RecipeHolder<ColossalJsonRecipe>> holders(RecipeType<ColossalJsonRecipe> type) {
        RecipeManager manager = recipeManager();
        if (manager == null) {
            return List.of();
        }
        return manager.getRecipes().stream()
                .filter(holder -> holder.value().getType().equals(type))
                .map(holder -> (RecipeHolder<ColossalJsonRecipe>) holder)
                .toList();
    }

    public static List<FuelDefinition> fuels() {
        List<FuelDefinition> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.FUEL.get())) {
            FuelDefinition def = FuelLoader.parseEntry(holder.value().json(), holder.id().identifier().toString(), true);
            if (def != null) {
                out.add(def);
            }
        }
        return out.isEmpty() ? FuelLoader.getVisibleDefinitions() : out;
    }

    public static List<CoolantJeiRecipe> coolants() {
        List<CoolantDefinition> defs = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.COOLANT.get())) {
            CoolantDefinition def = CoolantLoader.parseEntry(holder.value().json(), holder.id().identifier().toString(), true);
            if (def != null) {
                defs.add(def);
            }
        }
        if (defs.isEmpty()) {
            defs = CoolantLoader.getVisibleDefinitions();
        }
        return defs.stream().flatMap(def -> CoolantJeiRecipe.expand(def).stream()).toList();
    }

    public static List<HeatSinkDefinition> heatSinks() {
        List<HeatSinkDefinition> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.HEAT_SINKS.get())) {
            HeatSinkDefinition def = HeatSinkLoader.parseEntry(holder.value().json(), holder.id().identifier().toString());
            if (def != null) {
                out.add(def);
            }
        }
        return out.isEmpty() ? HeatSinkLoader.getAllDefinitions() : out;
    }

    public static List<MelterRecipe> melterRecipes() {
        List<MelterRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.MELTER_RECIPES.get())) {
            MelterRecipe r = MelterRecipesLoader.parseEntry(holder.value().json(), holder.id().identifier().toString());
            if (r != null) {
                out.add(r);
            }
        }
        return out.isEmpty() ? MelterRecipesLoader.getAll() : out;
    }

    public static List<MelterHeatEntry> melterHeats() {
        List<MelterHeatEntry> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.MELTER_HEATS.get())) {
            List<MelterHeatEntry> list = MelterHeatsLoader.parseFromRoot(holder.value().json(), holder.id().identifier().toString());
            if (list != null) {
                out.addAll(list);
            }
        }
        return out.isEmpty() ? MelterHeatsLoader.getAll() : out;
    }

    public static List<ElecCoilDefinition> elecCoils() {
        List<ElecCoilDefinition> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.ELEC_COILS.get())) {
            ElecCoilDefinition def = ElecCoilLoader.parseEntry(holder.value().json(), holder.id().identifier().toString());
            if (def != null) {
                out.add(def);
            }
        }
        return out.isEmpty() ? ElecCoilLoader.getJeIDefinitions() : out;
    }

    public static List<TurbineJeiRecipe> turbineGeneration() {
        List<TurbineGenerationDefinition> defs = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.TURBINE_GENERATION.get())) {
            TurbineGenerationDefinition def =
                    TurbineGenerationLoader.parseEntry(holder.value().json(), holder.id().identifier().toString(), true);
            if (def != null) {
                defs.add(def);
            }
        }
        if (defs.isEmpty()) {
            defs = TurbineGenerationLoader.getJeIDefinitions();
        }
        return defs.stream().flatMap(def -> TurbineJeiRecipe.expand(def).stream()).toList();
    }

    public static List<HeatingCoilJeiRecipe> heatingCoils() {
        List<HeatingCoilJeiRecipe> out = new ArrayList<>();
        for (RecipeHolder<ColossalJsonRecipe> holder : holders(ModColossalRecipes.HEATING_COILS.get())) {
            for (HeatingCoilDefinition def :
                    HeatingCoilLoader.parseFromRoot(holder.value().json(), holder.id().identifier().toString())) {
                out.addAll(HeatingCoilJeiRecipe.expand(def));
            }
        }
        if (out.isEmpty()) {
            return ColossalReactorsJeiPlugin.buildHeatingCoilJeiRecipes();
        }
        return out.stream().filter(Objects::nonNull).toList();
    }
}
