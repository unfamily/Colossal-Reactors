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
import net.unfamily.colossal_reactors.blockentity.RedstoneMode;

/**
 * C2S: cycle Heating Coil redstone mode (NONE, LOW, HIGH, PULSE, DISABLED).
 */
public record HeatingCoilRedstoneModePayload(BlockPos pos) implements CustomPacketPayload {

    public static final Type<HeatingCoilRedstoneModePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(ColossalReactors.MODID, "heating_coil_redstone_mode"));

    public static final StreamCodec<FriendlyByteBuf, HeatingCoilRedstoneModePayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            HeatingCoilRedstoneModePayload::pos,
            HeatingCoilRedstoneModePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(HeatingCoilRedstoneModePayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (be instanceof HeatingCoilBlockEntity coil) {
                RedstoneMode current = RedstoneMode.fromId(coil.getRedstoneMode());
                coil.setRedstoneMode(current.next().getId());
                level.playSound(null, packet.pos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.3f, 1.0f);
            }
        });
    }
}
