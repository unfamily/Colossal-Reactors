package net.unfamily.colossal_reactors.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.client.PreviewMarkRenderer;

/** S2C: clear all footprint preview markers on the client. */
public record ClearPreviewPayload() implements CustomPacketPayload {

    public static final Type<ClearPreviewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "clear_preview"));

    public static final StreamCodec<FriendlyByteBuf, ClearPreviewPayload> STREAM_CODEC =
            StreamCodec.unit(new ClearPreviewPayload());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearPreviewPayload packet, IPayloadContext context) {
        context.enqueueWork(PreviewMarkRenderer.getInstance()::clearMarkers);
    }
}
