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
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;

/**
 * C2S: cycle turbine builder simulation steam-generation recipe.
 */
public record TurbineBuilderSimSettingsPayload(BlockPos pos, boolean next) implements CustomPacketPayload {

    public static final Type<TurbineBuilderSimSettingsPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "turbine_builder_sim_settings"));

    public static final StreamCodec<FriendlyByteBuf, TurbineBuilderSimSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TurbineBuilderSimSettingsPayload::pos,
            ByteBufCodecs.BOOL,
            TurbineBuilderSimSettingsPayload::next,
            TurbineBuilderSimSettingsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TurbineBuilderSimSettingsPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            BlockEntity be = player.level().getBlockEntity(packet.pos());
            if (be instanceof TurbineBuilderBlockEntity builder) {
                builder.cycleSimGeneration(packet.next());
            }
        });
    }
}
