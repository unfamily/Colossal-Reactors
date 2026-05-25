package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.client.BuilderPreviewTracker;

/** S2C: clear footprint preview markers for one builder only. */
public record ClearPreviewForBuilderPayload(BlockPos builderPos) implements CustomPacketPayload {

    public static final Type<ClearPreviewForBuilderPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "clear_preview_for_builder"));

    public static final StreamCodec<FriendlyByteBuf, ClearPreviewForBuilderPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ClearPreviewForBuilderPayload::builderPos,
            ClearPreviewForBuilderPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearPreviewForBuilderPayload packet, IPayloadContext context) {
        context.enqueueWork(() ->
                BuilderPreviewTracker.clearForBuilder(packet.builderPos()));
    }
}
