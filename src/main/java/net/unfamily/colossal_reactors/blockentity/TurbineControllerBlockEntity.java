package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.menu.TurbineControllerMenu;
import net.unfamily.colossal_reactors.turbine.TurbineSimulation;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;
import org.jetbrains.annotations.Nullable;

/**
 * Turbine controller: caches validation and runtime stats for GUI.
 */
public class TurbineControllerBlockEntity extends BlockEntity implements MenuProvider {

    private TurbineValidation.Result cachedResult = TurbineValidation.Result.invalid();
    private boolean powered;
    private long lastRfPerTick;
    private double lastSteamPerTick;
    private double lastCoilEff;
    private double lastBladeEff;
    @Nullable
    private Player lastInteractingPlayer;

    public TurbineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_CONTROLLER_BE.get(), pos, state);
    }

    public TurbineValidation.Result getCachedResult() {
        return cachedResult != null ? cachedResult : TurbineValidation.Result.invalid();
    }

    public boolean isPowered() { return powered; }
    public long getLastRfPerTick() { return lastRfPerTick; }
    public double getLastSteamPerTick() { return lastSteamPerTick; }
    public double getLastCoilEff() { return lastCoilEff; }
    public double getLastBladeEff() { return lastBladeEff; }

    public void setLastInteractingPlayer(@Nullable Player player) {
        this.lastInteractingPlayer = player;
    }

    public void setCachedResult(TurbineValidation.Result result) {
        this.cachedResult = result != null ? result : TurbineValidation.Result.invalid();
        setChanged();
    }

    /** Called from block tick when VALIDATING -> ON/OFF; shows message in action bar. */
    public void notifyValidationResult() {
        if (lastInteractingPlayer instanceof ServerPlayer sp) {
            Component msg = getCachedResult().valid()
                    ? Component.translatable("message.colossal_reactors.turbine_valid")
                    : Component.translatable("message.colossal_reactors.turbine_invalid");
            sp.displayClientMessage(msg, true);
        }
        lastInteractingPlayer = null;
    }

    public void tickSimulation(ServerLevel level) {
        if (!cachedResult.valid()) {
            lastRfPerTick = 0;
            lastSteamPerTick = 0;
            return;
        }
        TurbineSimulation.RuntimeResult r = TurbineSimulation.tickRuntime(level, cachedResult);
        lastRfPerTick = r.rfPerTick();
        lastSteamPerTick = r.steamMbPerTick();
        lastCoilEff = r.coilEfficiency();
        lastBladeEff = r.bladeEfficiency();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.turbine_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TurbineControllerMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Powered", powered);
        tag.putLong("LastRf", lastRfPerTick);
        tag.putDouble("LastSteam", lastSteamPerTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        powered = tag.getBoolean("Powered");
        lastRfPerTick = tag.getLong("LastRf");
        lastSteamPerTick = tag.getDouble("LastSteam");
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
