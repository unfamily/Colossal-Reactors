package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.HeatingCoilBlockEntity;
import net.unfamily.colossal_reactors.blockentity.MelterBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;

/**
 * C2S: discard all fluid in the block's internal tank (GUI dump button).
 */
public record FluidTankDumpPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<FluidTankDumpPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "fluid_tank_dump"));

    public static final StreamCodec<FriendlyByteBuf, FluidTankDumpPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            FluidTankDumpPayload::pos,
            FluidTankDumpPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(FluidTankDumpPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(packet.pos());
            boolean emptied = false;
            if (be instanceof ReactorBuilderBlockEntity builder) {
                emptied = builder.dumpFluidTankContents();
            } else if (be instanceof TurbineBuilderBlockEntity turbineBuilder) {
                emptied = turbineBuilder.dumpFluidTankContents();
            } else if (be instanceof ResourcePortBlockEntity port) {
                emptied = port.dumpFluidTankContents();
            } else if (be instanceof MelterBlockEntity melter) {
                emptied = melter.dumpFluidTankContents();
            } else if (be instanceof HeatingCoilBlockEntity coil) {
                emptied = coil.dumpFluidTankContents();
            }
            if (emptied) {
                level.playSound(null, packet.pos(), SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.25f, 1.0f);
            }
        });
    }
}
