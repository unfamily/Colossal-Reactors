package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.TurbineBuilderBlock;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;
import net.unfamily.colossal_reactors.preview.BuilderPreviewMarkerLogic;

/** C2S: turbine footprint preview markers (aligned with {@link ReactorPreviewPayload}). */
public record TurbinePreviewPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<TurbinePreviewPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "turbine_preview"));

    public static final StreamCodec<FriendlyByteBuf, TurbinePreviewPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TurbinePreviewPayload::pos, TurbinePreviewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TurbinePreviewPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof TurbineBuilderBlockEntity builder) {
                sendFootprint(player, builder, packet.pos());
            }
        });
    }

    public static void sendFootprint(ServerPlayer player, TurbineBuilderBlockEntity builder, BlockPos builderPos) {
        BuilderPreviewServerTracker.track(player, builderPos, false);
        ServerLevel level = (ServerLevel) player.level();
        BlockState state = level.getBlockState(builderPos);
        if (!(state.getBlock() instanceof TurbineBuilderBlock)) {
            return;
        }
        int footprintGeneration = BuilderPreviewServerTracker.nextFootprintGeneration(builderPos);
        BuilderPreviewNetworking.clearClientPreview(player, builderPos, footprintGeneration);
        int durationTicks = net.unfamily.colossal_reactors.client.BuilderPreviewTracker.BUILDER_PREVIEW_DURATION_TICKS;
        BuilderPreviewMarkerLogic.forEachTurbineMarker(level, builder, builderPos,
                (worldPos, color) -> ModPayloads.sendPreviewMarker(
                        player, builderPos, worldPos, color, durationTicks, footprintGeneration));
    }
}
