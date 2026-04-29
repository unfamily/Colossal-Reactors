package net.unfamily.colossal_reactors.client.model;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.resources.model.UnbakedModel;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.model.UnbakedModelLoader;

/**
 * Model loader that falls back to a vanilla cube model using a *_single texture
 * when the Fusion connected texture mod isn't present.
 *
 * <p>The model JSON must provide:
 * <ul>
 *   <li>{@code single_texture}: string resource location for the vanilla cube texture</li>
 *   <li>{@code connected_model}: Json object containing the Fusion model JSON</li>
 * </ul>
 */
public final class ConnectedTextureFallbackModelLoader implements UnbakedModelLoader<UnbakedModel> {

    @Override
    public UnbakedModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
        JsonElement singleTextureEl = jsonObject.get("single_texture");
        if (!(singleTextureEl instanceof JsonPrimitive prim) || !prim.isString()) {
            throw new JsonParseException("ConnectedTextureFallbackModelLoader requires string \"single_texture\".");
        }
        String singleTexture = prim.getAsString();

        JsonElement connectedModelEl = jsonObject.get("connected_model");
        if (!(connectedModelEl instanceof JsonObject connectedModelObj)) {
            throw new JsonParseException("ConnectedTextureFallbackModelLoader requires JSON object \"connected_model\".");
        }

        boolean fusionLoaded = ModList.get().isLoaded("fusion");
        if (fusionLoaded) {
            // Let Fusion's loader parse the connected model JSON.
            return deserializationContext.deserialize(connectedModelObj, UnbakedModel.class);
        }

        // Vanilla fallback: cube_all with the single texture.
        JsonObject singleModelJson = new JsonObject();
        singleModelJson.addProperty("parent", "minecraft:block/cube_all");
        JsonObject textures = new JsonObject();
        textures.addProperty("all", singleTexture);
        singleModelJson.add("textures", textures);

        return deserializationContext.deserialize(singleModelJson, UnbakedModel.class);
    }
}

