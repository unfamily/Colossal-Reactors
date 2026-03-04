package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.PacketDistributor;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModPayloads {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModPayloads::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(ColossalReactors.MODID).versioned("1");
        registrar.playToServer(
                ResourcePortModePayload.TYPE,
                ResourcePortModePayload.STREAM_CODEC,
                ResourcePortModePayload::handle
        );
        registrar.playToServer(
                ResourcePortFilterPayload.TYPE,
                ResourcePortFilterPayload.STREAM_CODEC,
                ResourcePortFilterPayload::handle
        );
        registrar.playToServer(
                RedstonePortRedstoneModePayload.TYPE,
                RedstonePortRedstoneModePayload.STREAM_CODEC,
                RedstonePortRedstoneModePayload::handle
        );
        registrar.playToServer(
                ReactorControllerRefreshPayload.TYPE,
                ReactorControllerRefreshPayload.STREAM_CODEC,
                ReactorControllerRefreshPayload::handle
        );
        registrar.playToServer(
                ReactorBuilderSizePayload.TYPE,
                ReactorBuilderSizePayload.STREAM_CODEC,
                ReactorBuilderSizePayload::handle
        );
        registrar.playToServer(
                ReactorPreviewPayload.TYPE,
                ReactorPreviewPayload.STREAM_CODEC,
                ReactorPreviewPayload::handle
        );
        registrar.playToServer(
                ReactorBuilderHeatSinkPayload.TYPE,
                ReactorBuilderHeatSinkPayload.STREAM_CODEC,
                ReactorBuilderHeatSinkPayload::handle
        );
    }

    /** S2C: send one preview marker to the player (called from server in ReactorPreviewPayload handler). */
    public static void sendPreviewMarker(ServerPlayer player, BlockPos pos, int color, int durationTicks) {
        PacketDistributor.sendToPlayer(player, new ReactorPreviewMarkerPayload(pos, color, durationTicks));
    }
}
