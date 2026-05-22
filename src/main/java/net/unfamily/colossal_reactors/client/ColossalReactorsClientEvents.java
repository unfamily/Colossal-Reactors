package net.unfamily.colossal_reactors.client;

import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.unfamily.colossal_reactors.client.turbine.TurbineRotorAnimationManager;
import net.unfamily.colossal_reactors.compat.jei.JeiDatapackRecipeSync;
import net.unfamily.colossal_reactors.datapack.LoadDataReloadListener;
import net.unfamily.colossal_reactors.datapack.ReactorDataReloadListener;
import net.unfamily.colossal_reactors.melter.MelterHeatsLoader;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import net.unfamily.colossal_reactors.turbine.ElecCoilLoader;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;

public final class ColossalReactorsClientEvents {

    private static boolean reappliedReactorDataForLevel;

    private ColossalReactorsClientEvents() {}

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        TurbineRotorAnimationManager.clientTick();
        Minecraft mc = Minecraft.getInstance();
        if (!reappliedReactorDataForLevel && mc.level != null) {
            reappliedReactorDataForLevel = true;
            refreshDatapackForWorld();
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.client.player.LocalPlayer)) {
            return;
        }
        refreshDatapackForWorld();
    }

    private static void refreshDatapackForWorld() {
        ReactorDataReloadListener.refreshFromLastLoaded();
        LoadDataReloadListener.refreshFromLastLoaded();
        MelterRecipesLoader.rebuild();
        MelterHeatsLoader.rebuild();
        ElecCoilLoader.rebuildDefinitions();
        TurbineGenerationLoader.rebuildDefinitions();
        JeiDatapackRecipeSync.syncWhenWorldReady();
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof net.minecraft.client.player.LocalPlayer) {
            reappliedReactorDataForLevel = false;
        }
    }
}
