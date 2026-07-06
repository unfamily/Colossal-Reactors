package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

/** Server-side helpers for builder footprint preview (clear / refresh). */
public final class BuilderPreviewNetworking {

    private BuilderPreviewNetworking() {}

    public static void clearClientPreview(ServerPlayer player, BlockPos builderPos, int footprintGeneration) {
        PacketDistributor.sendToPlayer(player, new ClearPreviewForBuilderPayload(builderPos, false, footprintGeneration));
    }

    /** After block break: every client may still hold markers keyed by this builder origin. */
    public static void clearPreviewForAllPlayersInLevel(Level level, BlockPos builderPos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        BuilderPreviewServerTracker.clearBuilder(builderPos);
        ClearPreviewForBuilderPayload packet = new ClearPreviewForBuilderPayload(builderPos, true);
        for (ServerPlayer player : serverLevel.players()) {
            PacketDistributor.sendToPlayer(player, packet);
        }
    }
}
