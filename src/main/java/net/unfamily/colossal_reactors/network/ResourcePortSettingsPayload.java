package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;

/**
 * C2S: update resource port mode or a medium toggle.
 */
public record ResourcePortSettingsPayload(BlockPos pos, byte kind, int value) implements CustomPacketPayload {

    public static final byte KIND_SOLID = 1;
    public static final byte KIND_LIQUID = 2;
    public static final byte KIND_GAS = 3;

    public static final Type<ResourcePortSettingsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "resource_port_settings"));

    public static final StreamCodec<FriendlyByteBuf, ResourcePortSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ResourcePortSettingsPayload::pos,
            ByteBufCodecs.BYTE,
            ResourcePortSettingsPayload::kind,
            ByteBufCodecs.VAR_INT,
            ResourcePortSettingsPayload::value,
            ResourcePortSettingsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResourcePortSettingsPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (!(be instanceof ResourcePortBlockEntity port)) return;
            switch (packet.kind()) {
                case KIND_SOLID -> port.setAllowSolid(packet.value() != 0);
                case KIND_LIQUID -> port.setAllowLiquid(packet.value() != 0);
                case KIND_GAS -> port.setAllowGas(packet.value() != 0);
                default -> { }
            }
        });
    }
}
