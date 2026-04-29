package net.unfamily.colossal_reactors.client;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.unfamily.colossal_reactors.ColossalReactors;
import net.unfamily.colossal_reactors.client.gui.HeatingCoilScreen;
import net.unfamily.colossal_reactors.client.gui.MelterScreen;
import net.unfamily.colossal_reactors.client.gui.RadiationScrubberScreen;
import net.unfamily.colossal_reactors.client.gui.ReactorBuilderScreen;
import net.unfamily.colossal_reactors.client.gui.ReactorControllerScreen;
import net.unfamily.colossal_reactors.client.gui.RedstonePortScreen;
import net.unfamily.colossal_reactors.client.gui.ResourcePortScreen;
import net.unfamily.colossal_reactors.menu.ModMenuTypes;
import net.unfamily.colossal_reactors.network.ReactorPreviewMarkerPayload;

/**
 * Client-only menu ↔ screen binding (NeoForge 26: {@link RegisterMenuScreensEvent}) and S2C payloads.
 */
public final class ColossalClientSetup {

    private ColossalClientSetup() {}

    /** S2C: reactor footprint preview markers (handler stays client-only). */
    public static void registerPayloadHandlers(RegisterPayloadHandlersEvent event) {
        event.registrar(ColossalReactors.MODID).versioned("1").playToClient(
                ReactorPreviewMarkerPayload.TYPE,
                ReactorPreviewMarkerPayload.STREAM_CODEC,
                ClientPayloadHandlers::handlePreviewMarker);
    }

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.RESOURCE_PORT_MENU.get(), ResourcePortScreen::new);
        event.register(ModMenuTypes.REDSTONE_PORT_MENU.get(), RedstonePortScreen::new);
        event.register(ModMenuTypes.REACTOR_CONTROLLER_MENU.get(), ReactorControllerScreen::new);
        event.register(ModMenuTypes.REACTOR_BUILDER_MENU.get(), ReactorBuilderScreen::new);
        event.register(ModMenuTypes.HEATING_COIL_MENU.get(), HeatingCoilScreen::new);
        event.register(ModMenuTypes.MELTER_MENU.get(), MelterScreen::new);
        event.register(ModMenuTypes.RADIATION_SCRUBBER_MENU.get(), RadiationScrubberScreen::new);
    }
}
