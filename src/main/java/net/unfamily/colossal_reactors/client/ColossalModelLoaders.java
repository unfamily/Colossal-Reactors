package net.unfamily.colossal_reactors.client;

import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.client.model.ConnectedTextureFallbackModelLoader;

public final class ColossalModelLoaders {
    private ColossalModelLoaders() {}

    public static void registerModelLoaders(ModelEvent.RegisterLoaders event) {
        Identifier fallbackKey = Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "connected_texture_fallback");
        event.register(fallbackKey, new ConnectedTextureFallbackModelLoader());
    }
}

