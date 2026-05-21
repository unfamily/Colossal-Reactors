package net.unfamily.colossal_reactors.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.colossal_reactors.client.turbine.TurbineRotorAnimationManager;
import net.unfamily.colossal_reactors.datapack.ReactorDataReloadListener;

public final class ColossalReactorsClientEvents {

    private static boolean reappliedReactorDataForLevel;

    private ColossalReactorsClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        TurbineRotorAnimationManager.clientTick();
        Minecraft mc = Minecraft.getInstance();
        if (!reappliedReactorDataForLevel && mc.level != null) {
            reappliedReactorDataForLevel = true;
            ReactorDataReloadListener.refreshFromLastLoaded();
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.client.player.LocalPlayer)) {
            return;
        }
        ReactorDataReloadListener.refreshFromLastLoaded();
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.client.player.LocalPlayer) {
            reappliedReactorDataForLevel = false;
        }
    }
}
