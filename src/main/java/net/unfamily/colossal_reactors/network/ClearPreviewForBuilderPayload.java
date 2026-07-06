package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.client.BuilderPreviewTracker;

/** S2C: clear footprint preview markers for one builder only. */
public record ClearPreviewForBuilderPayload(BlockPos builderPos, boolean deactivate, int footprintGeneration)
        implements CustomPacketPayload {

    public ClearPreviewForBuilderPayload(BlockPos builderPos, boolean deactivate) {
        this(builderPos, deactivate, 0);
    }

    public static final Type<ClearPreviewForBuilderPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "clear_preview_for_builder"));

    public static final StreamCodec<FriendlyByteBuf, ClearPreviewForBuilderPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ClearPreviewForBuilderPayload::builderPos,
            ByteBufCodecs.BOOL,
            ClearPreviewForBuilderPayload::deactivate,
            ByteBufCodecs.VAR_INT,
            ClearPreviewForBuilderPayload::footprintGeneration,
            ClearPreviewForBuilderPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearPreviewForBuilderPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (packet.deactivate()) {
                BuilderPreviewTracker.deactivateBuilder(packet.builderPos());
            } else {
                BuilderPreviewTracker.applyFootprintClear(packet.builderPos(), packet.footprintGeneration());
            }
        });
    }
}
