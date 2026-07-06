package net.unfamily.colossal_reactors.network;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.unfamily.colossal_reactors.block.ReactorBuilderBlock;
import net.unfamily.colossal_reactors.block.TurbineBuilderBlock;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineBuilderBlockEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Server-side per-player builder preview sessions; pushes footprint refresh on world block changes. */
public final class BuilderPreviewServerTracker {

    private static final int REFRESH_INTERVAL_TICKS = 2;

    private static final Map<BlockPos, Set<UUID>> playersByBuilder = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<BlockPos>> buildersByPlayer = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Boolean> reactorBuilderByOrigin = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Map<UUID, Long>> lastRefreshGameTime = new ConcurrentHashMap<>();
    private static final Map<BlockPos, Integer> footprintGenerationByBuilder = new ConcurrentHashMap<>();

    private BuilderPreviewServerTracker() {}

    public static int nextFootprintGeneration(BlockPos builderPos) {
        return footprintGenerationByBuilder.merge(builderPos.immutable(), 1, Integer::sum);
    }

    public static void track(ServerPlayer player, BlockPos builderPos, boolean reactorBuilder) {
        if (player == null || builderPos == null) {
            return;
        }
        BlockPos key = builderPos.immutable();
        UUID playerId = player.getUUID();
        playersByBuilder.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet()).add(playerId);
        buildersByPlayer.computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet()).add(key);
        reactorBuilderByOrigin.put(key, reactorBuilder);
    }

    public static void untrack(ServerPlayer player, BlockPos builderPos) {
        if (player == null || builderPos == null) {
            return;
        }
        BlockPos key = builderPos.immutable();
        UUID playerId = player.getUUID();
        Set<BlockPos> builders = buildersByPlayer.get(playerId);
        if (builders != null) {
            builders.remove(key);
            if (builders.isEmpty()) {
                buildersByPlayer.remove(playerId);
            }
        }
        Set<UUID> players = playersByBuilder.get(key);
        if (players != null) {
            players.remove(playerId);
            if (players.isEmpty()) {
                playersByBuilder.remove(key);
                reactorBuilderByOrigin.remove(key);
                lastRefreshGameTime.remove(key);
            }
        }
        Map<UUID, Long> refreshTimes = lastRefreshGameTime.get(key);
        if (refreshTimes != null) {
            refreshTimes.remove(playerId);
        }
    }

    public static void clearBuilder(BlockPos builderPos) {
        if (builderPos == null) {
            return;
        }
        BlockPos key = builderPos.immutable();
        Set<UUID> players = playersByBuilder.remove(key);
        reactorBuilderByOrigin.remove(key);
        lastRefreshGameTime.remove(key);
        footprintGenerationByBuilder.remove(key);
        if (players == null) {
            return;
        }
        for (UUID playerId : players) {
            Set<BlockPos> builders = buildersByPlayer.get(playerId);
            if (builders != null) {
                builders.remove(key);
                if (builders.isEmpty()) {
                    buildersByPlayer.remove(playerId);
                }
            }
        }
    }

    public static void clearPlayer(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Set<BlockPos> builders = buildersByPlayer.remove(player.getUUID());
        if (builders == null) {
            return;
        }
        for (BlockPos builder : builders) {
            Set<UUID> players = playersByBuilder.get(builder);
            if (players != null) {
                players.remove(player.getUUID());
                if (players.isEmpty()) {
                    playersByBuilder.remove(builder);
                    reactorBuilderByOrigin.remove(builder);
                    lastRefreshGameTime.remove(builder);
                    footprintGenerationByBuilder.remove(builder);
                }
            }
            Map<UUID, Long> refreshTimes = lastRefreshGameTime.get(builder);
            if (refreshTimes != null) {
                refreshTimes.remove(player.getUUID());
            }
        }
    }

    public static void onFootprintBlockChanged(ServerLevel level, BlockPos changedPos) {
        if (level == null || changedPos == null || playersByBuilder.isEmpty()) {
            return;
        }
        Vec3 point = Vec3.atCenterOf(changedPos);
        for (Map.Entry<BlockPos, Set<UUID>> entry : playersByBuilder.entrySet()) {
            BlockPos builderPos = entry.getKey();
            if (!containsBlock(level, builderPos, point)) {
                continue;
            }
            for (UUID playerId : Set.copyOf(entry.getValue())) {
                ServerPlayer player = level.getServer().getPlayerList().getPlayer(playerId);
                if (player == null || player.level() != level) {
                    continue;
                }
                if (!canRefresh(level, builderPos, playerId)) {
                    continue;
                }
                refreshFootprint(player, builderPos);
            }
        }
    }

    private static boolean canRefresh(ServerLevel level, BlockPos builderPos, UUID playerId) {
        long now = level.getGameTime();
        Map<UUID, Long> perPlayer = lastRefreshGameTime.computeIfAbsent(builderPos.immutable(), ignored -> new ConcurrentHashMap<>());
        Long last = perPlayer.get(playerId);
        if (last != null && now - last < REFRESH_INTERVAL_TICKS) {
            return false;
        }
        perPlayer.put(playerId, now);
        return true;
    }

    private static void refreshFootprint(ServerPlayer player, BlockPos builderPos) {
        BlockEntity be = player.level().getBlockEntity(builderPos);
        Boolean reactorBuilder = reactorBuilderByOrigin.get(builderPos);
        if (reactorBuilder == null) {
            return;
        }
        if (Boolean.TRUE.equals(reactorBuilder) && be instanceof ReactorBuilderBlockEntity reactor) {
            ReactorPreviewPayload.sendFootprint(player, reactor, builderPos);
        } else if (Boolean.FALSE.equals(reactorBuilder) && be instanceof TurbineBuilderBlockEntity turbine) {
            TurbinePreviewPayload.sendFootprint(player, turbine, builderPos);
        }
    }

    private static boolean containsBlock(ServerLevel level, BlockPos builderPos, Vec3 worldPoint) {
        BlockEntity be = level.getBlockEntity(builderPos);
        BlockState builderState = level.getBlockState(builderPos);
        AABB volume = null;
        if (be instanceof ReactorBuilderBlockEntity reactor && builderState.getBlock() instanceof ReactorBuilderBlock) {
            Direction facing = builderState.getValue(ReactorBuilderBlock.FACING);
            volume = ReactorBuilderBlockEntity.getReactorVolumeAABB(
                    builderPos, facing,
                    reactor.getSizeLeft(), reactor.getSizeRight(),
                    reactor.getSizeHeight(), reactor.getSizeDepth());
        } else if (be instanceof TurbineBuilderBlockEntity turbine && builderState.getBlock() instanceof TurbineBuilderBlock) {
            Direction facing = builderState.getValue(TurbineBuilderBlock.FACING);
            volume = TurbineBuilderBlockEntity.getTurbineVolumeAABB(
                    builderPos, facing,
                    turbine.getSizeLeft(), turbine.getSizeRight(),
                    turbine.getSizeHeight(), turbine.getSizeDepth());
        }
        return volume != null && volume.contains(worldPoint);
    }
}
