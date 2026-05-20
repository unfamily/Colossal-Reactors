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
import net.unfamily.colossal_reactors.block.TurbineBuilderBlock;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;
import net.unfamily.colossal_reactors.turbine.ElecCoilLoader;
import net.unfamily.colossal_reactors.turbine.TurbineRodControllerLayout;
import net.unfamily.colossal_reactors.turbine.TurbineRodPatternLogic;
import net.unfamily.colossal_reactors.turbine.TurbineRodSpaceLayout;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;

/** C2S: turbine footprint preview markers. */
public record TurbinePreviewPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<TurbinePreviewPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "turbine_preview"));

    public static final StreamCodec<FriendlyByteBuf, TurbinePreviewPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, TurbinePreviewPayload::pos, TurbinePreviewPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TurbinePreviewPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
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
            int interiorH = TurbineRodSpaceLayout.interiorHeight(h);
            int coilLayers = builder.getCoilLayerCount();
            int closureWorldY = TurbineRodControllerLayout.closureWorldY(minY, interiorH, coilLayers);
            int rw = TurbineRodPatternLogic.rodSpaceWidth(w);
            int rh = TurbineRodPatternLogic.rodSpaceHeight(h, coilLayers);
            int rd = TurbineRodPatternLogic.rodSpaceDepth(d);
            int inset = TurbineRodSpaceLayout.rodSpaceInset();
            int coilStart = TurbineRodSpaceLayout.coilZoneStartY(interiorH, coilLayers);
            int pattern = builder.getRodPattern();
            int colorFree = 0x80FF00FF;
            int colorOccupied = 0xE0FF0000;
            int colorRod = 0xE0FFFF00;
            int colorRodController = 0xE0FFFFFF;
            int duration = 200;
            var reg = level.registryAccess();

            TurbineRodControllerLayout.Center rodCtrlCenter = TurbineRodControllerLayout.bestPrimaryCenter(rw, rd);
            ModPayloads.sendPreviewMarker(player,
                    new BlockPos(
                            TurbineRodControllerLayout.closureWorldX(minX, rodCtrlCenter.rx()),
                            closureWorldY,
                            TurbineRodControllerLayout.closureWorldZ(minZ, rodCtrlCenter.rz())),
                    colorRodController, duration);

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos p = new BlockPos(x, y, z);
                        BlockState st = level.getBlockState(p);
                        boolean border = x == minX || x == maxX || y == minY || y == maxY || z == minZ || z == maxZ;
                        if (border && st.isAir()) {
                            ModPayloads.sendPreviewMarker(player, p, colorFree, duration);
                        } else if (!st.isAir() && border && !TurbineValidation.isShellBlock(st)
                                && !(st.is(ModBlocks.TURBINE_ROD_CONTROLLER.get()) && y == closureWorldY
                                && isRodControllerPreviewPosition(x, z, minX, minZ, closureWorldY, rw, rd))) {
                            ModPayloads.sendPreviewMarker(player, p, colorOccupied, duration);
                        } else if (!border) {
                            int iy = y - minY - 1;
                            int rx = x - minX - 1 - inset;
                            int rz = z - minZ - 1 - inset;
                            if (iy < coilStart && rx >= 0 && rx < rw && rz >= 0 && rz < rd
                                    && TurbineRodPatternLogic.isRodColumn(rx, rz, rw, rd, pattern)) {
                                ModPayloads.sendPreviewMarker(player, p, colorRod, duration);
                            } else if (iy >= coilStart && !st.isAir()
                                    && !ElecCoilLoader.isCoilBlock(st, reg)) {
                                ModPayloads.sendPreviewMarker(player, p, colorOccupied, duration);
                            }
                        }
                    }
                }
            }
        });
    }

    private static boolean isRodControllerPreviewPosition(
            int x, int z, int minX, int minZ, int closureWorldY, int rw, int rd) {
        if (rw <= 0 || rd <= 0) {
            return false;
        }
        TurbineRodControllerLayout.Center center = TurbineRodControllerLayout.bestPrimaryCenter(rw, rd);
        return x == TurbineRodControllerLayout.closureWorldX(minX, center.rx())
                && z == TurbineRodControllerLayout.closureWorldZ(minZ, center.rz());
    }
}
