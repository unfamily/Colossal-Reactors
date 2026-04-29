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
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;

/**
 * C2S: cycle a Reactor Builder option. optionType: 0=openTop (toggle; {@code next} ignored), 1=rodPattern, 2=patternMode.
 * For types 1–2: {@code next} true = next, false = previous.
 */
public record ReactorBuilderOptionPayload(BlockPos pos, int optionType, boolean next) implements CustomPacketPayload {

    public static final Type<ReactorBuilderOptionPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_builder_option"));

    public static final StreamCodec<FriendlyByteBuf, ReactorBuilderOptionPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ReactorBuilderOptionPayload::pos,
            net.minecraft.network.codec.ByteBufCodecs.INT,
            ReactorBuilderOptionPayload::optionType,
            net.minecraft.network.codec.ByteBufCodecs.BOOL,
            ReactorBuilderOptionPayload::next,
            ReactorBuilderOptionPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReactorBuilderOptionPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.level();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof ReactorBuilderBlockEntity builder) {
                switch (Math.max(0, packet.optionType())) {
                    case 0 -> builder.cycleOpenTop(packet.next());
                    case 1 -> builder.cycleRodPattern(packet.next());
                    case 2 -> builder.cyclePatternMode(packet.next());
                    default -> {}
                }
            }
        });
    }
}
