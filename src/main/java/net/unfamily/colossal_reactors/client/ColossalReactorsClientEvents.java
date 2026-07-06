package net.unfamily.colossal_reactors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.unfamily.colossal_reactors.block.ReactorBuilderBlock;
import net.unfamily.colossal_reactors.block.TurbineBuilderBlock;
import net.unfamily.colossal_reactors.client.turbine.TurbineRotorAnimationManager;
import net.unfamily.colossal_reactors.compat.jei.JeiDatapackRecipeSync;
import net.unfamily.colossal_reactors.datapack.LoadDataReloadListener;
import net.unfamily.colossal_reactors.network.ReactorPreviewPayload;
import net.unfamily.colossal_reactors.network.TurbinePreviewPayload;

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
        if (mc.level != null) {
            BuilderPreviewTracker.tickPeriodicReconcile(mc.level);
            for (BlockPos builderPos : BuilderPreviewTracker.pollBuildersNeedingWorldRefresh(mc.level)) {
                BuilderPreviewTracker.onFootprintRefreshRequested(mc.level, builderPos);
                var state = mc.level.getBlockState(builderPos);
                if (state.getBlock() instanceof ReactorBuilderBlock) {
                    PacketDistributor.sendToServer(new ReactorPreviewPayload(builderPos));
                } else if (state.getBlock() instanceof TurbineBuilderBlock) {
                    PacketDistributor.sendToServer(new TurbinePreviewPayload(builderPos));
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof LocalPlayer)) {
            return;
        }
        refreshDatapackForWorld();
    }

    private static void refreshDatapackForWorld() {
        LoadDataReloadListener.refreshFromLastLoaded();
        JeiDatapackRecipeSync.syncWhenWorldReady();
    }

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof LocalPlayer) {
            BuilderPreviewTracker.clearAll();
            reappliedReactorDataForLevel = false;
        }
    }

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getLevel() instanceof Level level) {
            BuilderPreviewTracker.onBlockInPreviewChanged(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof Level level) {
            BuilderPreviewTracker.onBlockInPreviewChanged(level, event.getPos());
        }
    }
}
