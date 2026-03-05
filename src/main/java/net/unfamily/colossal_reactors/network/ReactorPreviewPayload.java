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
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.ReactorBuilderBlock;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.reactor.ReactorValidation;
import net.unfamily.colossal_reactors.reactor.RodPatternLogic;

/**
 * C2S: request reactor footprint preview. Server computes AABB and sends marker payloads to client.
 * <p>
 * Build (not yet implemented) must use the same logic so placement matches the preview:
 * <ul>
 *   <li>Rod space: {@link net.unfamily.colossal_reactors.reactor.RodPatternLogic#rodSpaceWidth rodSpaceWidth/Height/Depth}
 *   and {@link net.unfamily.colossal_reactors.reactor.RodPatternLogic#rodSpaceInsetXZ rodSpaceInsetXZ} (frame -2 on X/Z, then mode -2 on X/Z for Optimized/Economy; no -2 on Y).</li>
 *   <li>Rod positions: iterate interior with insetXZ on X/Z only, full height on Y; use {@link net.unfamily.colossal_reactors.reactor.RodPatternLogic#isRodForPreview isRodForPreview}
 *   with expansion variant from {@link net.unfamily.colossal_reactors.reactor.RodPatternLogic#getExpansionRodAtCenterForPreview getExpansionRodAtCenterForPreview} for Frame.</li>
 *   <li>Rod controllers: one block above the top of each rod column, at Y = minY + rh (same (rx,rz) as rod columns via isRodColumnForPreview).</li>
 * </ul>
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
            int colorRodController = 0xE0FFFFFF; // white tint for rod controllers (above rod columns, part of frame)
            int durationTicks = 200;

            int pattern = builder.getRodPattern();
            int patternMode = builder.getPatternMode();
            int w = maxX - minX + 1;
            int h = maxY - minY + 1;
            int d = maxZ - minZ + 1;
            int rw = RodPatternLogic.rodSpaceWidth(w, patternMode);
            int rh = RodPatternLogic.rodSpaceHeight(h, patternMode);
            int rd = RodPatternLogic.rodSpaceDepth(d, patternMode);
            int insetXZ = RodPatternLogic.rodSpaceInsetXZ(patternMode); // -2 only on X and Z: frame then mode

            // For Frame (EXPANSION): compute both variants (rod at center vs heat sink at center), use the one with more rod columns
            boolean expansionRodAtCenter = (pattern == RodPatternLogic.PATTERN_EXPANSION)
                    ? RodPatternLogic.getExpansionRodAtCenterForPreview(rw, rd)
                    : false;

            // Rod positions (interior): yellow markers; rod space has inset X/Z and inset Y (1 top, 1 bottom for frame)
            for (int lx = insetXZ; lx < w - insetXZ; lx++) {
                for (int ly = 1; ly < h - 1; ly++) { // skip bottom (minY) and top (maxY) frame
                    for (int lz = insetXZ; lz < d - insetXZ; lz++) {
                        int rx = lx - insetXZ;
                        int ry = ly - 1; // rod space Y 0..rh-1
                        int rz = lz - insetXZ;
                        if (RodPatternLogic.isRodForPreview(rx, ry, rz, rw, rh, rd, pattern, expansionRodAtCenter)) {
                            ModPayloads.sendPreviewMarker(player, new BlockPos(minX + lx, minY + ly, minZ + lz), colorRod, durationTicks);
                        }
                    }
                }
            }

            // Rod controllers: in the top frame (same layer we draw in purple), white
            int rodControllerY = minY + h - 1; // maxY = top frame
            for (int rx = 0; rx < rw; rx++) {
                for (int rz = 0; rz < rd; rz++) {
                    if (RodPatternLogic.isRodColumnForPreview(rx, rz, rw, rd, pattern, expansionRodAtCenter)) {
                        ModPayloads.sendPreviewMarker(player, new BlockPos(minX + insetXZ + rx, rodControllerY, minZ + insetXZ + rz), colorRodController, durationTicks);
                    }
                }
            }

            // Full volume: red only where there is a block that doesn't belong (air = empty, not a problem)
            var registryAccess = level.registryAccess();
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState blockState = level.getBlockState(pos);
                        int lx = x - minX, ly = y - minY, lz = z - minZ;
                        boolean onBorder = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                        boolean hasBlock = !blockState.isAir() && !blockState.canBeReplaced();

                        if (onBorder) {
                            boolean validFrame = ReactorValidation.isShellBlock(blockState)
                                    || (blockState.is(ModBlocks.ROD_CONTROLLER.get()) && y == maxY && isRodControllerPosition(x, z, minX, minZ, maxY, insetXZ, rw, rd, pattern, expansionRodAtCenter));
                            if (hasBlock && !validFrame) {
                                ModPayloads.sendPreviewMarker(player, pos, colorOccupied, durationTicks);
                            } else {
                                // Purple only on the frame outline (12 edges), not on every face
                                boolean onEdge = ((x == minX || x == maxX) && (y == minY || y == maxY))
                                        || ((x == minX || x == maxX) && (z == minZ || z == maxZ))
                                        || ((y == minY || y == maxY) && (z == minZ || z == maxZ));
                                if (onEdge) {
                                    ModPayloads.sendPreviewMarker(player, pos, colorFree, durationTicks);
                                }
                            }
                        } else {
                            boolean isRodPos = (lx >= insetXZ && lx < w - insetXZ && ly >= 1 && ly < h - 1 && lz >= insetXZ && lz < d - insetXZ)
                                    && RodPatternLogic.isRodForPreview(lx - insetXZ, ly - 1, lz - insetXZ, rw, rh, rd, pattern, expansionRodAtCenter);
                            if (isRodPos) {
                                if (hasBlock && !blockState.is(ModBlocks.REACTOR_ROD.get())) {
                                    ModPayloads.sendPreviewMarker(player, pos, colorOccupied, durationTicks);
                                }
                            } else {
                                if (hasBlock && !HeatSinkLoader.isHeatSinkBlock(blockState, registryAccess)) {
                                    ModPayloads.sendPreviewMarker(player, pos, colorOccupied, durationTicks);
                                }
                            }
                        }
                    }
                }
            }
        });
    }

    /** True if (x, z) at y=maxY is a valid rod controller position (top face, above a rod column). */
    private static boolean isRodControllerPosition(int x, int z, int minX, int minZ, int maxY, int insetXZ, int rw, int rd, int pattern, boolean expansionRodAtCenter) {
        int rx = x - minX - insetXZ;
        int rz = z - minZ - insetXZ;
        if (rx < 0 || rx >= rw || rz < 0 || rz >= rd) return false;
        return RodPatternLogic.isRodColumnForPreview(rx, rz, rw, rd, pattern, expansionRodAtCenter);
    }
}
