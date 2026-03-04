package net.unfamily.colossal_reactors.client;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.network.ReactorPreviewMarkerPayload;

/**
 * Client-only payload handlers (S2C). Called when the client receives the payload.
 */
public final class ClientPayloadHandlers {

    public static void handlePreviewMarker(ReactorPreviewMarkerPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                PreviewMarkRenderer.getInstance().addMarker(payload.pos(), payload.color(), payload.durationTicks()));
    }
}
