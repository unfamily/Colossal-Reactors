package net.unfamily.colossal_reactors.client;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.colossal_reactors.datapack.ReactorDataReloadListener;

public final class ColossalReactorsClientEvents {

    private ColossalReactorsClientEvents() {}

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.client.player.LocalPlayer)) {
            return;
        }
        ReactorDataReloadListener.refreshFromLastLoaded();
    }
}
