package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;

/** C2S: enable or disable footprint preview for one builder (per-player client state). */
public record BuilderPreviewTogglePayload(BlockPos builderPos, boolean enable, boolean reactorBuilder)
        implements CustomPacketPayload {

    public static final Type<BuilderPreviewTogglePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "builder_preview_toggle"));

    public static final StreamCodec<FriendlyByteBuf, BuilderPreviewTogglePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            BuilderPreviewTogglePayload::builderPos,
            net.minecraft.network.codec.ByteBufCodecs.BOOL,
            BuilderPreviewTogglePayload::enable,
            net.minecraft.network.codec.ByteBufCodecs.BOOL,
            BuilderPreviewTogglePayload::reactorBuilder,
            BuilderPreviewTogglePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BuilderPreviewTogglePayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockEntity be = player.serverLevel().getBlockEntity(packet.builderPos());
            if (packet.enable()) {
                BuilderPreviewServerTracker.track(player, packet.builderPos(), packet.reactorBuilder());
                if (packet.reactorBuilder() && be instanceof ReactorBuilderBlockEntity reactor) {
                    ReactorPreviewPayload.sendFootprint(player, reactor, packet.builderPos());
                } else if (!packet.reactorBuilder() && be instanceof TurbineBuilderBlockEntity turbine) {
                    TurbinePreviewPayload.sendFootprint(player, turbine, packet.builderPos());
                }
            } else {
                BuilderPreviewServerTracker.untrack(player, packet.builderPos());
                PacketDistributor.sendToPlayer(player, new ClearPreviewForBuilderPayload(packet.builderPos(), true));
            }
        });
    }
}
