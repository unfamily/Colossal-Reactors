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
import net.unfamily.colossal_reactors.reactor.RodPatternLogic;

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

            int colorFree = 0x80FF00FF; // transparent purple (visible against blue sky)
            int colorOccupied = 0xE0FF0000; // red tint for occupied blocks
            int colorRod = 0xE0FFFF00; // yellow tint for rod positions
            int durationTicks = 200;

            int pattern = builder.getRodPattern();
            int patternMode = builder.getPatternMode();
            int w = maxX - minX + 1;
            int h = maxY - minY + 1;
            int d = maxZ - minZ + 1;
            int rw = RodPatternLogic.rodSpaceWidth(w, patternMode);
            int rh = RodPatternLogic.rodSpaceHeight(h, patternMode);
            int rd = RodPatternLogic.rodSpaceDepth(d, patternMode);
            int inset = (patternMode == RodPatternLogic.MODE_PRODUCTION) ? 0 : 1;

            // Rod positions (interior): yellow markers
            for (int lx = inset; lx < w - inset; lx++) {
                for (int ly = inset; ly < h - inset; ly++) {
                    for (int lz = inset; lz < d - inset; lz++) {
                        int rx = lx - inset;
                        int ry = ly - inset;
                        int rz = lz - inset;
                        if (RodPatternLogic.isRod(rx, ry, rz, rw, rh, rd, pattern, true)) {
                            ModPayloads.sendPreviewMarker(player, new BlockPos(minX + lx, minY + ly, minZ + lz), colorRod, durationTicks);
                        }
                    }
                }
            }

            // Border only (like fan): top/bottom, front/back, left/right faces edges; red if block occupied
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean edge = (x == minX || x == maxX || z == minZ || z == maxZ);
                    if (edge) {
                        int cMin = isBlockOccupied(level, new BlockPos(x, minY, z)) ? colorOccupied : colorFree;
                        int cMax = isBlockOccupied(level, new BlockPos(x, maxY, z)) ? colorOccupied : colorFree;
                        ModPayloads.sendPreviewMarker(player, new BlockPos(x, minY, z), cMin, durationTicks);
                        ModPayloads.sendPreviewMarker(player, new BlockPos(x, maxY, z), cMax, durationTicks);
                    }
                }
            }
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    boolean edge = (x == minX || x == maxX || y == minY || y == maxY);
                    if (edge) {
                        int cMin = isBlockOccupied(level, new BlockPos(x, y, minZ)) ? colorOccupied : colorFree;
                        int cMax = isBlockOccupied(level, new BlockPos(x, y, maxZ)) ? colorOccupied : colorFree;
                        ModPayloads.sendPreviewMarker(player, new BlockPos(x, y, minZ), cMin, durationTicks);
                        ModPayloads.sendPreviewMarker(player, new BlockPos(x, y, maxZ), cMax, durationTicks);
                    }
                }
            }
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    boolean edge = (z == minZ || z == maxZ || y == minY || y == maxY);
                    if (edge) {
                        int cMin = isBlockOccupied(level, new BlockPos(minX, y, z)) ? colorOccupied : colorFree;
                        int cMax = isBlockOccupied(level, new BlockPos(maxX, y, z)) ? colorOccupied : colorFree;
                        ModPayloads.sendPreviewMarker(player, new BlockPos(minX, y, z), cMin, durationTicks);
                        ModPayloads.sendPreviewMarker(player, new BlockPos(maxX, y, z), cMax, durationTicks);
                    }
                }
            }
        });
    }

    /** True if the block at pos would block reactor construction (not air, not replaceable). */
    private static boolean isBlockOccupied(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return !state.isAir() && !state.canBeReplaced();
    }
}
