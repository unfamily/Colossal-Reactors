package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;

/**
 * C2S: adjust reactor builder area in one direction (0=up, 1=left, 2=right, 3=behind). increment=true add, false subtract. amount=1,5,10 (Shift=10, Alt/Ctrl=5).
 */
public record ReactorBuilderSizePayload(BlockPos pos, int direction, boolean increment, int amount) implements CustomPacketPayload {

    public static final Type<ReactorBuilderSizePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_builder_size"));

    public static final StreamCodec<FriendlyByteBuf, ReactorBuilderSizePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ReactorBuilderSizePayload::pos,
            ByteBufCodecs.INT,
            ReactorBuilderSizePayload::direction,
            ByteBufCodecs.BOOL,
            ReactorBuilderSizePayload::increment,
            ByteBufCodecs.INT,
            ReactorBuilderSizePayload::amount,
            ReactorBuilderSizePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReactorBuilderSizePayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof ReactorBuilderBlockEntity builder) {
                int amt = Math.max(1, Math.min(10, packet.amount()));
                builder.adjustSize(packet.direction(), packet.increment(), amt);
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
            }
        });
    }
}
