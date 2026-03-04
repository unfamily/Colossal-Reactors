package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ReactorBuilderBlock;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;

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
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (!(be instanceof ReactorBuilderBlockEntity builder)) return;
            var state = level.getBlockState(packet.pos());
            if (!(state.getBlock() instanceof ReactorBuilderBlock block)) return;
            var facing = state.getValue(ReactorBuilderBlock.FACING);

            var aabb = ReactorBuilderBlockEntity.getReactorVolumeAABB(
                    packet.pos(), facing,
                    builder.getSizeLeft(), builder.getSizeRight(),
                    builder.getSizeHeight(), builder.getSizeDepth());

            int minX = (int) Math.floor(aabb.minX);
            int minY = (int) Math.floor(aabb.minY);
            int minZ = (int) Math.floor(aabb.minZ);
            int maxX = (int) Math.floor(aabb.maxX - 1e-6);
            int maxY = (int) Math.floor(aabb.maxY - 1e-6);
            int maxZ = (int) Math.floor(aabb.maxZ - 1e-6);

            int color = 0x80FF00FF; // transparent purple (visible against blue sky)
            int durationTicks = 200;

            // Border only (like fan): top/bottom, front/back, left/right faces edges
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = (x == minX || x == maxX || z == minZ || z == maxZ);
                    if (edge) {
                        ModPayloads.sendPreviewMarker(player, new BlockPos(x, minY, z), color, durationTicks);
                        ModPayloads.sendPreviewMarker(player, new BlockPos(x, maxY, z), color, durationTicks);
                    }
                }
            }
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    boolean edge = (x == minX || x == maxX || y == minY || y == maxY);
                    if (edge) {
                        ModPayloads.sendPreviewMarker(player, new BlockPos(x, y, minZ), color, durationTicks);
                        ModPayloads.sendPreviewMarker(player, new BlockPos(x, y, maxZ), color, durationTicks);
                    }
                }
            }
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    boolean edge = (z == minZ || z == maxZ || y == minY || y == maxY);
                    if (edge) {
                        ModPayloads.sendPreviewMarker(player, new BlockPos(minX, y, z), color, durationTicks);
                        ModPayloads.sendPreviewMarker(player, new BlockPos(maxX, y, z), color, durationTicks);
                    }
                }
            }
        });
    }
}
