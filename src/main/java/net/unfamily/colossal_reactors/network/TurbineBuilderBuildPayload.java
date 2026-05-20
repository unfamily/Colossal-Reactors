package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;

/**
 * C2S: toggle Reactor Builder build/stop. If building → stop; else → start (only if no red zone).
 */
public record TurbineBuilderBuildPayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<TurbineBuilderBuildPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "turbine_builder_build"));

    public static final StreamCodec<FriendlyByteBuf, TurbineBuilderBuildPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TurbineBuilderBuildPayload::pos,
            TurbineBuilderBuildPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TurbineBuilderBuildPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer)) return;
            ServerLevel level = ((ServerPlayer) context.player()).level();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (!(be instanceof TurbineBuilderBlockEntity builder)) return;
            if (builder.isBuilding()) {
                builder.stopBuild();
                builder.clearInvalidBlocksFlag();
            } else {
                builder.startBuild();
            }
        });
    }
}
