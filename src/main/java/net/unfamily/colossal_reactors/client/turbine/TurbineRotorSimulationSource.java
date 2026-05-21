package net.unfamily.colossal_reactors.client.turbine;

import net.minecraft.client.Minecraft;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.unfamily.colossal_reactors.blockentity.TurbineControllerBlockEntity;

/**
 * Resolves the block entity that owns authoritative simulation stats for rotor rendering.
 * In integrated singleplayer the client copy often lags behind the server copy.
 */
public final class TurbineRotorSimulationSource {

    private TurbineRotorSimulationSource() {}

    public static TurbineControllerBlockEntity forRendering(TurbineControllerBlockEntity clientController) {
        Level clientLevel = clientController.getLevel();
        if (clientLevel == null || !clientLevel.isClientSide()) {
            return clientController;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.hasSingleplayerServer()) {
            return clientController;
        }
        var server = minecraft.getSingleplayerServer();
        if (server == null) {
            return clientController;
        }
        ServerLevel serverLevel = server.getLevel(clientLevel.dimension());
        if (serverLevel == null) {
            return clientController;
        }
        BlockEntity serverBe = serverLevel.getBlockEntity(clientController.getBlockPos());
        if (serverBe instanceof TurbineControllerBlockEntity serverController) {
            clientController.mirrorRuntimeStatsFrom(serverController);
            return serverController;
        }
        return clientController;
    }
}
