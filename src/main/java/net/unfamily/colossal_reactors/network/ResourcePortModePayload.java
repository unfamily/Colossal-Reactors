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
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;

/**
 * C2S packet: set Resource Port mode (insert / extract / eject).
 */
public record ResourcePortModePayload(BlockPos pos, int mode) implements CustomPacketPayload {

    public static final Type<ResourcePortModePayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "resource_port_mode"));

    public static final StreamCodec<FriendlyByteBuf, ResourcePortModePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ResourcePortModePayload::pos,
            ByteBufCodecs.INT,
            ResourcePortModePayload::mode,
            ResourcePortModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResourcePortModePayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.level();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof ResourcePortBlockEntity port) {
                port.setPortMode(PortMode.fromId(packet.mode()));
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
            }
        });
    }
}
