package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ReactorBuilderBlock;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.preview.BuilderPreviewMarkerLogic;

/**
 * C2S: request reactor footprint preview. Server computes AABB and sends marker payloads to client.
 */
public record ReactorPreviewPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<ReactorPreviewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_preview"));

    public static final StreamCodec<FriendlyByteBuf, ReactorPreviewPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ReactorPreviewPayload::pos,
            ReactorPreviewPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReactorPreviewPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockEntity be = player.serverLevel().getBlockEntity(packet.pos());
            if (be instanceof ReactorBuilderBlockEntity builder) {
                sendFootprint(player, builder, packet.pos());
            }
        });
    }

    public static void sendFootprint(ServerPlayer player, ReactorBuilderBlockEntity builder, BlockPos builderPos) {
        BuilderPreviewServerTracker.track(player, builderPos, true);
        ServerLevel level = player.serverLevel();
        BlockState state = level.getBlockState(builderPos);
        if (!(state.getBlock() instanceof ReactorBuilderBlock)) {
            return;
        }
        int footprintGeneration = BuilderPreviewServerTracker.nextFootprintGeneration(builderPos);
        BuilderPreviewNetworking.clearClientPreview(player, builderPos, footprintGeneration);
        int durationTicks = 0;
        BuilderPreviewMarkerLogic.forEachReactorMarker(level, builder, builderPos,
                (worldPos, color) -> ModPayloads.sendPreviewMarker(
                        player, builderPos, worldPos, color, durationTicks, footprintGeneration));
    }
}
