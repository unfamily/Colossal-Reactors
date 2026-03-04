package net.unfamily.colossal_reactors.network;

import net.neoforged.bus.api.IEventBus;
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
    }
}
