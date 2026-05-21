package net.unfamily.colossal_reactors.client;

import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.unfamily.colossal_reactors.client.gui.HeatingCoilScreen;
import net.unfamily.colossal_reactors.client.gui.MelterScreen;
import net.unfamily.colossal_reactors.client.gui.RadiationScrubberScreen;
import net.unfamily.colossal_reactors.client.gui.ReactorBuilderScreen;
import net.unfamily.colossal_reactors.client.gui.ReactorControllerScreen;
import net.unfamily.colossal_reactors.client.gui.TurbineBuilderScreen;
import net.unfamily.colossal_reactors.client.gui.TurbineControllerScreen;
import net.unfamily.colossal_reactors.client.gui.RedstonePortScreen;
import net.unfamily.colossal_reactors.client.gui.ResourcePortScreen;
import net.unfamily.colossal_reactors.menu.ModMenuTypes;

/**
 * Client-only menu ↔ screen binding (NeoForge 26: {@link RegisterMenuScreensEvent}).
 * S2C payloads are registered in {@link net.unfamily.colossal_reactors.network.ModPayloads}.
 */
public final class ColossalClientSetup {

    private ColossalClientSetup() {}

    public static void registerMenuScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.RESOURCE_PORT_MENU.get(), ResourcePortScreen::new);
        event.register(ModMenuTypes.REDSTONE_PORT_MENU.get(), RedstonePortScreen::new);
        event.register(ModMenuTypes.REACTOR_CONTROLLER_MENU.get(), ReactorControllerScreen::new);
        event.register(ModMenuTypes.REACTOR_BUILDER_MENU.get(), ReactorBuilderScreen::new);
        event.register(ModMenuTypes.TURBINE_CONTROLLER_MENU.get(), TurbineControllerScreen::new);
        event.register(ModMenuTypes.TURBINE_BUILDER_MENU.get(), TurbineBuilderScreen::new);
        event.register(ModMenuTypes.HEATING_COIL_MENU.get(), HeatingCoilScreen::new);
        event.register(ModMenuTypes.MELTER_MENU.get(), MelterScreen::new);
        event.register(ModMenuTypes.RADIATION_SCRUBBER_MENU.get(), RadiationScrubberScreen::new);
    }
}
