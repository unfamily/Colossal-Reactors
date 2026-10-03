package net.unfamily.colossal_reactors.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.stream.Stream;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Data-only datapack recipe: holds the raw JSON entry (after Library bundle split).
 * Not craftable in a crafting table; used so RecipeManager / KubeJS / viewers see native types.
 */
public final class ColossalJsonRecipe implements Recipe<CraftingInput> {
    private final JsonObject json;
    private final RecipeType<ColossalJsonRecipe> type;

    public ColossalJsonRecipe(JsonObject json, RecipeType<ColossalJsonRecipe> type) {
        this.json = json;
        this.type = type;
    }

    public JsonObject json() {
        return json;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<? extends Recipe<CraftingInput>> getSerializer() {
        return ModColossalRecipes.serializerFor(type);
    }

    @Override
    public RecipeType<? extends Recipe<CraftingInput>> getType() {
        return type;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    static MapCodec<ColossalJsonRecipe> mapCodec(RecipeType<ColossalJsonRecipe> recipeType) {
        return new MapCodec<>() {
            @Override
            public <T> DataResult<ColossalJsonRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
                JsonElement el = ops.convertTo(JsonOps.INSTANCE, ops.createMap(input.entries()));
                if (!el.isJsonObject()) {
                    return DataResult.error(() -> "Colossal recipe must be a JSON object");
                }
                return DataResult.success(new ColossalJsonRecipe(el.getAsJsonObject(), recipeType));
            }

            @Override
            public <T> RecordBuilder<T> encode(ColossalJsonRecipe recipe, DynamicOps<T> ops, RecordBuilder<T> prefix) {
                T encoded = JsonOps.INSTANCE.convertTo(ops, recipe.json());
                DataResult<MapLike<T>> asMap = ops.getMap(encoded);
                asMap.result().ifPresent(m -> m.entries().forEach(e -> prefix.add(e.getFirst(), e.getSecond())));
                return prefix.withErrorsFrom(asMap);
            }

            @Override
            public <T> Stream<T> keys(DynamicOps<T> ops) {
                return Stream.empty();
            }
        };
    }

    static StreamCodec<RegistryFriendlyByteBuf, ColossalJsonRecipe> streamCodec(MapCodec<ColossalJsonRecipe> mapCodec) {
        return ByteBufCodecs.fromCodecWithRegistries(mapCodec.codec());
    }
}
