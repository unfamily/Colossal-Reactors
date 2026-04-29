package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.block.ControllerState;
import net.unfamily.colossal_reactors.block.ReactorControllerBlock;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;

/**
 * C2S: request reactor re-validation (refresh). Puts controller into VALIDATING and schedules tick.
 */
public record ReactorControllerRefreshPayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ReactorControllerRefreshPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "reactor_controller_refresh"));

    public static final StreamCodec<FriendlyByteBuf, ReactorControllerRefreshPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            ReactorControllerRefreshPayload::pos,
            ReactorControllerRefreshPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ReactorControllerRefreshPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level() instanceof ServerLevel level)) return;
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (!(be instanceof ReactorControllerBlockEntity controller)) return;
            BlockState state = level.getBlockState(packet.pos());
            if (!state.hasProperty(ReactorControllerBlock.STATE)) return;
            if (state.getValue(ReactorControllerBlock.STATE) != ControllerState.ON) return;
            level.setBlock(packet.pos(), state.setValue(ReactorControllerBlock.STATE, ControllerState.VALIDATING),
                    net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS | net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            controller.setChanged();
            level.scheduleTick(packet.pos(), state.getBlock(), 1);
        });
    }
}
