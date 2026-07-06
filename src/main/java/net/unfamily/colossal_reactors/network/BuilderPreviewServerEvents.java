package net.unfamily.colossal_reactors.network;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/** Pushes builder footprint preview refresh when blocks inside an active preview volume change. */
public final class BuilderPreviewServerEvents {

    private BuilderPreviewServerEvents() {}

    @SubscribeEvent
    public static void onBlockBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BuilderPreviewServerTracker.onFootprintBlockChanged(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() instanceof ServerLevel level) {
            BuilderPreviewServerTracker.onFootprintBlockChanged(level, event.getPos());
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            BuilderPreviewServerTracker.clearPlayer(player);
        }
    }
}
