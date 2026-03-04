package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.unfamily.colossal_reactors.ColossalReactors;

/**
 * S2C: add one preview marker at the given position (for reactor footprint preview).
 * Handler is in client package (ClientPayloadHandlers).
 */
public record ReactorPreviewMarkerPayload(BlockPos pos, int color, int durationTicks) implements CustomPacketPayload {

    public static final Type<ReactorPreviewMarkerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_preview_marker"));

    public static final StreamCodec<FriendlyByteBuf, ReactorPreviewMarkerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ReactorPreviewMarkerPayload::pos,
            net.minecraft.network.codec.ByteBufCodecs.INT,
            ReactorPreviewMarkerPayload::color,
            net.minecraft.network.codec.ByteBufCodecs.INT,
            ReactorPreviewMarkerPayload::durationTicks,
            ReactorPreviewMarkerPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
