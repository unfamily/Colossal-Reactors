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
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineBuilderBlock;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;
import net.unfamily.colossal_reactors.turbine.ElecCoilLoader;
import net.unfamily.colossal_reactors.turbine.TurbineRodControllerLayout;
import net.unfamily.colossal_reactors.turbine.TurbineRodPatternLogic;
import net.unfamily.colossal_reactors.turbine.TurbineRotorLayout;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;

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
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.level();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (!(be instanceof TurbineBuilderBlockEntity builder)) return;
            BlockState state = level.getBlockState(packet.pos());
            if (!(state.getBlock() instanceof TurbineBuilderBlock)) return;
            var facing = state.getValue(TurbineBuilderBlock.FACING);
            var aabb = TurbineBuilderBlockEntity.getTurbineVolumeAABB(
                    packet.pos(), facing, builder.getSizeLeft(), builder.getSizeRight(),
                    builder.getSizeHeight(), builder.getSizeDepth());
            int minX = (int) Math.floor(aabb.minX);
            int minY = (int) Math.floor(aabb.minY);
            int minZ = (int) Math.floor(aabb.minZ);
            int maxX = (int) Math.floor(aabb.maxX - 1e-6);
            int maxY = (int) Math.floor(aabb.maxY - 1e-6);
            int maxZ = (int) Math.floor(aabb.maxZ - 1e-6);
            int w = maxX - minX + 1;
            int h = maxY - minY + 1;
            int d = maxZ - minZ + 1;
            int inset = 1;
            int coilLayers = builder.getAppliedCoilLayerCount();
            var growthAxis = builder.getPlacementAxis();
            TurbineRotorLayout layout = TurbineRotorLayout.from(
                    minX, minY, minZ, maxX, maxY, maxZ, w, h, d, coilLayers, growthAxis);
            int rw = layout.crossSizeA();
            int rh = layout.rodExtent();
            int rd = layout.crossSizeB();
            int pattern = builder.getRodPattern();
            int colorFree = 0x80FF00FF;
            int colorOccupied = 0xE0FF0000;
            int colorRod = 0xE0FFFF00;
            int colorClosureDeck = 0xE070D8FF;
            int colorRodController = 0xE0FFFFFF;
            int durationTicks = 6000;
            var registryAccess = level.registryAccess();
            TurbineRodControllerLayout.Center rodCtrlCenter = layout.primaryCenter();

            for (int lx = inset; lx < w - inset; lx++) {
                for (int ly = inset; ly < h - inset; ly++) {
                    for (int lz = inset; lz < d - inset; lz++) {
                        int wx = minX + lx;
                        int wy = minY + ly;
                        int wz = minZ + lz;
                        if (!layout.isInRodZone(wx, wy, wz)) {
                            continue;
                        }
                        int rx = layout.crossAFromWorld(wx, wy, wz);
                        int rz = layout.crossBFromWorld(wx, wy, wz);
                        if (rx < 0 || rx >= rw || rz < 0 || rz >= rd) {
                            continue;
                        }
                        if (TurbineRodPatternLogic.isRodColumn(rx, rz, rw, rd, pattern)) {
                            ModPayloads.sendPreviewMarker(player, new BlockPos(wx, wy, wz), colorRod, durationTicks);
                        }
                    }
                }
            }

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        BlockState blockState = level.getBlockState(pos);
                        boolean onBorder = (x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ);
                        boolean hasBlock = !blockState.isAir() && !blockState.canBeReplaced();

                        if (onBorder) {
                            if (layout.isOpenEndCapWorld(x, y, z) && builder.isOpenTop() && blockState.isAir()) {
                                continue;
                            }
                            boolean validFrame = TurbineValidation.isShellBlock(blockState)
                                    || (blockState.is(ModBlocks.TURBINE_ROD_CONTROLLER.get())
                                    && layout.isRodControllerAt(x, y, z, rodCtrlCenter));
                            if (hasBlock && !validFrame) {
                                ModPayloads.sendPreviewMarker(player, pos, colorOccupied, durationTicks);
                            } else {
                                boolean onEdge = ((x == minX || x == maxX) && (y == minY || y == maxY))
                                        || ((x == minX || x == maxX) && (z == minZ || z == maxZ))
                                        || ((y == minY || y == maxY) && (z == minZ || z == maxZ));
                                if (onEdge) {
                                    int edgeColor = layout.isClosureDeckWorld(x, y, z) ? colorClosureDeck : colorFree;
                                    ModPayloads.sendPreviewMarker(player, pos, edgeColor, durationTicks);
                                }
                            }
                        } else {
                            if (layout.isInRodZone(x, y, z)) {
                                int rx = layout.crossAFromWorld(x, y, z);
                                int rz = layout.crossBFromWorld(x, y, z);
                                boolean isRodCol = rx >= 0 && rx < rw && rz >= 0 && rz < rd
                                        && TurbineRodPatternLogic.isRodColumn(rx, rz, rw, rd, pattern);
                                if (isRodCol) {
                                    if (hasBlock && !blockState.is(ModBlocks.TURBINE_ROD.get())
                                            && !blockState.is(ModBlocks.TURBINE_BLADE.get())) {
                                        ModPayloads.sendPreviewMarker(player, pos, colorOccupied, durationTicks);
                                    }
                                } else if (hasBlock && !blockState.is(ModBlocks.TURBINE_BLADE.get())
                                        && !blockState.is(ModBlocks.TURBINE_CASING.get())
                                        && !blockState.is(ModBlocks.TURBINE_GLASS.get())) {
                                    ModPayloads.sendPreviewMarker(player, pos, colorOccupied, durationTicks);
                                }
                            } else if (layout.isClosureDeckWorld(x, y, z)) {
                                if (layout.isRodControllerAt(x, y, z, rodCtrlCenter)) {
                                    continue;
                                }
                                if (hasBlock && !blockState.is(ModBlocks.TURBINE_CASING.get())
                                        && !blockState.is(ModBlocks.TURBINE_GLASS.get())
                                        && !blockState.isAir()) {
                                    ModPayloads.sendPreviewMarker(player, pos, colorOccupied, durationTicks);
                                } else {
                                    ModPayloads.sendPreviewMarker(player, pos, colorClosureDeck, durationTicks);
                                }
                            } else if (layout.isCoilZoneWorld(x, y, z)) {
                                if (hasBlock && !ElecCoilLoader.isCoilBlock(blockState, registryAccess)
                                        && !blockState.is(ModBlocks.TURBINE_CASING.get())
                                        && !blockState.is(ModBlocks.TURBINE_GLASS.get())) {
                                    ModPayloads.sendPreviewMarker(player, pos, colorOccupied, durationTicks);
                                }
                            } else if (hasBlock && !blockState.is(ModBlocks.TURBINE_CASING.get())
                                    && !blockState.is(ModBlocks.TURBINE_GLASS.get())) {
                                ModPayloads.sendPreviewMarker(player, pos, colorOccupied, durationTicks);
                            }
                        }
                    }
                }
            }

            ModPayloads.sendPreviewMarker(player, layout.controllerPos(rodCtrlCenter), colorRodController, durationTicks);
        });
    }
}
