package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.blockentity.PortMedium;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;

/**
 * C2S: update resource port medium (exclusive solid / liquid / gas).
 */
public record ResourcePortSettingsPayload(BlockPos pos, byte kind, int value) implements CustomPacketPayload {

    /** Cycle to the next medium, or set explicitly when {@code value} is a valid {@link PortMedium} id. */
    public static final byte KIND_MEDIUM = 1;
    /** Cycle forward (Solid → Liquid → Gas → …). */
    public static final int VALUE_CYCLE_FORWARD = -1;
    /** Cycle backward (right click). */
    public static final int VALUE_CYCLE_BACK = -2;

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
            ServerLevel level = player.level();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (!(be instanceof ResourcePortBlockEntity port)) return;
            if (packet.kind() != KIND_MEDIUM) {
                return;
            }
            if (packet.value() >= 0 && packet.value() <= PortMedium.GAS.getId()) {
                PortMedium explicit = PortMedium.fromId(packet.value());
                if (explicit == PortMedium.GAS && !MekChemicalHelper.isLoaded()) {
                    explicit = PortMedium.LIQUID;
                }
                port.setPortMedium(explicit);
            } else if (packet.value() == VALUE_CYCLE_BACK) {
                port.cyclePortMediumBack();
            } else {
                port.cyclePortMedium();
            }
        });
    }
}
