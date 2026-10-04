package net.unfamily.colossal_reactors.crafting;

import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.iskalib.crafting.RecipeBundleSplitter;

/**
 * Native RecipeType + serializer registration for Colossal datapack data recipes.
 */
public final class ModColossalRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, ColossalReactors.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, ColossalReactors.MODID);

    private static final Map<RecipeType<ColossalJsonRecipe>, RecipeSerializer<ColossalJsonRecipe>> SERIALIZER_BY_TYPE =
            new HashMap<>();

    public static final DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> FUEL =
            registerType("fuel");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>> FUEL_SERIALIZER =
            registerSerializer("fuel", FUEL);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> COOLANT =
            registerType("coolant");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>> COOLANT_SERIALIZER =
            registerSerializer("coolant", COOLANT);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> HEAT_SINKS =
            registerType("heat_sinks");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>> HEAT_SINKS_SERIALIZER =
            registerSerializer("heat_sinks", HEAT_SINKS);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> TURBINE_GENERATION =
            registerType("turbine_generation");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>>
            TURBINE_GENERATION_SERIALIZER = registerSerializer("turbine_generation", TURBINE_GENERATION);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> ELEC_COILS =
            registerType("elec_coils");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>> ELEC_COILS_SERIALIZER =
            registerSerializer("elec_coils", ELEC_COILS);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> MELTER_RECIPES =
            registerType("melter_recipes");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>>
            MELTER_RECIPES_SERIALIZER = registerSerializer("melter_recipes", MELTER_RECIPES);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> MELTER_HEATS =
            registerType("melter_heats");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>>
            MELTER_HEATS_SERIALIZER = registerSerializer("melter_heats", MELTER_HEATS);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> RADIATION_SCRUBBER_CATALYSTS =
            registerType("radiation_scrubber_catalysts");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>>
            RADIATION_SCRUBBER_CATALYSTS_SERIALIZER =
                    registerSerializer("radiation_scrubber_catalysts", RADIATION_SCRUBBER_CATALYSTS);

    public static final DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> HEATING_COILS =
            registerType("heating_coils");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>>
            HEATING_COILS_SERIALIZER = registerSerializer("heating_coils", HEATING_COILS);

    private ModColossalRecipes() {}

    static RecipeSerializer<ColossalJsonRecipe> serializerFor(RecipeType<ColossalJsonRecipe> type) {
        RecipeSerializer<ColossalJsonRecipe> serializer = SERIALIZER_BY_TYPE.get(type);
        if (serializer == null) {
            throw new IllegalStateException("No Colossal recipe serializer registered for type " + type);
        }
        return serializer;
    }

    private static DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> registerType(String path) {
        return RECIPE_TYPES.register(
                path,
                () -> RecipeType.simple(Identifier.fromNamespaceAndPath(ColossalReactors.MODID, path)));
    }

    private static DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ColossalJsonRecipe>> registerSerializer(
            String path, DeferredHolder<RecipeType<?>, RecipeType<ColossalJsonRecipe>> type) {
        return SERIALIZERS.register(path, () -> {
            RecipeType<ColossalJsonRecipe> recipeType = type.get();
            var mapCodec = ColossalJsonRecipe.mapCodec(recipeType);
            var streamCodec = ColossalJsonRecipe.streamCodec(mapCodec);
            RecipeSerializer<ColossalJsonRecipe> serializer = new RecipeSerializer<>(mapCodec, streamCodec);
            SERIALIZER_BY_TYPE.put(recipeType, serializer);
            return serializer;
        });
    }

    static {
        // Register as early as class load so ModifyRecipeJsonsEvent sees these types.
        registerBundleTypes();
    }

    public static void register(IEventBus modEventBus) {
        RECIPE_TYPES.register(modEventBus);
        SERIALIZERS.register(modEventBus);
        registerBundleTypes();
    }

    /** Tell Library to split {@code entries} bundles before KubeJS. */
    public static void registerBundleTypes() {
        RecipeBundleSplitter.registerType(ColossalReactors.MODID + ":fuel");
        RecipeBundleSplitter.registerType(ColossalReactors.MODID + ":coolant");
        RecipeBundleSplitter.registerType(ColossalReactors.MODID + ":heat_sinks");
        RecipeBundleSplitter.registerType(ColossalReactors.MODID + ":turbine_generation");
        RecipeBundleSplitter.registerType(ColossalReactors.MODID + ":elec_coils");
        RecipeBundleSplitter.registerType(ColossalReactors.MODID + ":melter_recipes");
        RecipeBundleSplitter.registerType(ColossalReactors.MODID + ":melter_heats");
        RecipeBundleSplitter.registerType(ColossalReactors.MODID + ":radiation_scrubber_catalysts");
        RecipeBundleSplitter.registerType(ColossalReactors.MODID + ":heating_coils");
    }
}
