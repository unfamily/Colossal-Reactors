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
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;

/**
 * C2S: cycle heat sink option in Reactor Builder. next=true = next, next=false = previous (left=prev, right=next).
 */
public record ReactorBuilderHeatSinkPayload(BlockPos pos, boolean next) implements CustomPacketPayload {

    public static final Type<ReactorBuilderHeatSinkPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_builder_heat_sink"));

    public static final StreamCodec<FriendlyByteBuf, ReactorBuilderHeatSinkPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ReactorBuilderHeatSinkPayload::pos,
            net.minecraft.network.codec.ByteBufCodecs.BOOL,
            ReactorBuilderHeatSinkPayload::next,
            ReactorBuilderHeatSinkPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReactorBuilderHeatSinkPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof ReactorBuilderBlockEntity builder) {
                builder.cycleHeatSink(packet.next());
            }
        });
    }
}
