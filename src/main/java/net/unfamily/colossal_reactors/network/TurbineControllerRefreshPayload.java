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
import net.unfamily.colossal_reactors.block.TurbineControllerBlock;
import net.unfamily.colossal_reactors.block.TurbineVisualState;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;

/**
 * C2S: request turbine re-validation (reboot). Puts controller into VALIDATING and schedules tick.
 */
public record TurbineControllerRefreshPayload(BlockPos pos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TurbineControllerRefreshPayload> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(ColossalReactors.MODID, "turbine_controller_refresh"));

    public static final StreamCodec<FriendlyByteBuf, TurbineControllerRefreshPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC,
            TurbineControllerRefreshPayload::pos,
            TurbineControllerRefreshPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(TurbineControllerRefreshPayload packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player().level() instanceof ServerLevel level)) {
                return;
            }
            BlockEntity be = level.getBlockEntity(packet.pos());
            if (!(be instanceof TurbineControllerBlockEntity controller)) {
                return;
            }
            BlockState state = level.getBlockState(packet.pos());
            if (!state.hasProperty(TurbineControllerBlock.VISUAL)) {
                return;
            }
            if (state.getValue(TurbineControllerBlock.VISUAL) != TurbineVisualState.ON) {
                return;
            }
            level.setBlock(packet.pos(), state.setValue(TurbineControllerBlock.VISUAL, TurbineVisualState.VALIDATING),
                    net.minecraft.world.level.block.Block.UPDATE_NEIGHBORS | net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
            controller.setChanged();
            level.scheduleTick(packet.pos(), state.getBlock(), 1);
        });
    }
}
