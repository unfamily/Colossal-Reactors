package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
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
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;

/**
 * C2S: cycle reactor builder simulation coolant (0) or fuel (1).
 */
public record ReactorBuilderSimSettingsPayload(BlockPos pos, byte setting, boolean next) implements CustomPacketPayload {

    public static final byte SETTING_COOLANT = 0;
    public static final byte SETTING_FUEL = 1;

    public static final Type<ReactorBuilderSimSettingsPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_builder_sim_settings"));

    public static final StreamCodec<FriendlyByteBuf, ReactorBuilderSimSettingsPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ReactorBuilderSimSettingsPayload::pos,
            ByteBufCodecs.BYTE,
            ReactorBuilderSimSettingsPayload::setting,
            ByteBufCodecs.BOOL,
            ReactorBuilderSimSettingsPayload::next,
            ReactorBuilderSimSettingsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReactorBuilderSimSettingsPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (!(be instanceof ReactorBuilderBlockEntity builder)) return;
            if (packet.setting() == SETTING_COOLANT) {
                builder.cycleSimCoolant(packet.next());
            } else if (packet.setting() == SETTING_FUEL) {
                builder.cycleSimFuel(packet.next());
            } else {
                return;
            }
            level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
        });
    }
}
