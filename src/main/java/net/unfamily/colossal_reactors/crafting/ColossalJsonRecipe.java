package net.unfamily.colossal_reactors.crafting;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import com.mojang.serialization.RecordBuilder;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import com.mojang.serialization.JsonOps;

/**
 * Data-only datapack recipe: holds the raw JSON entry (after Library bundle split).
 * Not craftable in a crafting table; used so RecipeManager / KubeJS / viewers see native types.
 */
public final class ColossalJsonRecipe extends CustomRecipe {
    private final JsonObject json;
    private final RecipeType<ColossalJsonRecipe> type;
    private final RecipeSerializer<ColossalJsonRecipe> serializer;

    public ColossalJsonRecipe(
            JsonObject json,
            RecipeType<ColossalJsonRecipe> type,
            RecipeSerializer<ColossalJsonRecipe> serializer) {
        super(CraftingBookCategory.MISC);
        this.json = json;
        this.type = type;
        this.serializer = serializer;
    }

    public JsonObject json() {
        return json;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    @Override
    public RecipeType<?> getType() {
        return type;
    }

    public static final class Serializer implements RecipeSerializer<ColossalJsonRecipe> {
        private final RecipeType<ColossalJsonRecipe> type;
        private final MapCodec<ColossalJsonRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, ColossalJsonRecipe> streamCodec;

        public Serializer(RecipeType<ColossalJsonRecipe> type) {
            this.type = type;
            this.codec = new MapCodec<>() {
                @Override
                public <T> DataResult<ColossalJsonRecipe> decode(DynamicOps<T> ops, MapLike<T> input) {
                    JsonElement el = ops.convertTo(JsonOps.INSTANCE, ops.createMap(input.entries()));
                    if (!el.isJsonObject()) {
                        return DataResult.error(() -> "Colossal recipe must be a JSON object");
                    }
                    return DataResult.success(new ColossalJsonRecipe(el.getAsJsonObject(), type, Serializer.this));
                }

                @Override
                public <T> RecordBuilder<T> encode(
                        ColossalJsonRecipe recipe, DynamicOps<T> ops, RecordBuilder<T> prefix) {
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
            this.streamCodec = ByteBufCodecs.fromCodecWithRegistries(
                    ExtraCodecs.JSON.xmap(
                            el -> new ColossalJsonRecipe(el.getAsJsonObject(), type, this),
                            r -> r.json()));
        }

        @Override
        public MapCodec<ColossalJsonRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, ColossalJsonRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
