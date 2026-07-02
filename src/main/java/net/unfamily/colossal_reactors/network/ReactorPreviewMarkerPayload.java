package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.client.BuilderPreviewTracker;

/** S2C: add one preview marker at the given position (reactor/turbine footprint preview). */
public record ReactorPreviewMarkerPayload(BlockPos builderOrigin, BlockPos pos, int color, int durationTicks)
        implements CustomPacketPayload {

    public static final Type<ReactorPreviewMarkerPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_preview_marker"));

    public static final StreamCodec<FriendlyByteBuf, ReactorPreviewMarkerPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ReactorPreviewMarkerPayload::builderOrigin,
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

    public static void handle(ReactorPreviewMarkerPayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                BuilderPreviewTracker.addMarker(
                        payload.builderOrigin(), payload.pos(), payload.color(), payload.durationTicks()));
    }
}
