package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.menu.ReactorControllerMenu;
import net.unfamily.colossal_reactors.reactor.ReactorValidation;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for the reactor controller. Caches multiblock validation result.
 * State (OFF/VALIDATING/ON) and tick logic are in ReactorControllerBlock.
 * When ON, right-click opens the Reactor OS GUI.
 */
public class ReactorControllerBlockEntity extends BlockEntity implements MenuProvider {

    private ReactorValidation.Result cachedResult;
    private ServerPlayer lastInteractingPlayer;

    /** Last tick stats for GUI (updated by ReactorSimulation.tick). Fuel is ingots/tick * 100 (e.g. 26 = 0.26). */
    private int lastRfPerTick;
    private int lastSteamPerTick;
    private int lastWaterPerTick;
    private int lastFuelPerTickHundredths;

    /** Reactor stability 0–1000 permille (0.0%–100.0%) when reactor unstability is enabled. Default 1000 = 100%. */
    private int stabilityPermille = 1000;

    public ReactorControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_CONTROLLER_BE.get(), pos, state);
    }

    public void setLastInteractingPlayer(Player player) {
        this.lastInteractingPlayer = player instanceof ServerPlayer sp ? sp : null;
    }

    /** Called from block tick when VALIDATING -> ON/OFF; shows message in action bar. */
    public void notifyValidationResult() {
        if (lastInteractingPlayer != null && cachedResult != null) {
            Component message = cachedResult.valid()
                    ? Component.translatable("message.colossal_reactors.reactor_valid")
                    : Component.translatable("message.colossal_reactors.reactor_invalid");
            lastInteractingPlayer.displayClientMessage(message, true);
            lastInteractingPlayer = null;
        }
    }

    public ReactorValidation.Result getCachedResult() {
        return cachedResult;
    }

    /** Set from block when validation is run (e.g. on player click). */
    public void setCachedResult(ReactorValidation.Result result) {
        this.cachedResult = result;
    }

    public void invalidateCache() {
        cachedResult = null;
        if (level != null && !level.isClientSide()) {
            setChanged();
        }
    }

    /** Called by ReactorSimulation.tick at end of each tick. Fuel hundredths = fuel units/tick * 100 (e.g. 0.26 -> 26). */
    public void setLastTickStats(int rfPerTick, int steamPerTick, int waterPerTick, int fuelPerTickHundredths) {
        this.lastRfPerTick = rfPerTick;
        this.lastSteamPerTick = steamPerTick;
        this.lastWaterPerTick = waterPerTick;
        this.lastFuelPerTickHundredths = fuelPerTickHundredths;
    }

    public int getLastRfPerTick() { return lastRfPerTick; }
    public int getLastSteamPerTick() { return lastSteamPerTick; }
    public int getLastWaterPerTick() { return lastWaterPerTick; }
    public int getLastFuelPerTickHundredths() { return lastFuelPerTickHundredths; }

    /** Stability in permille 0–1000 (display as 0.0%–100.0%). */
    public int getStabilityPermille() { return stabilityPermille; }
    public void setStabilityPermille(int permille) {
        this.stabilityPermille = Math.max(0, Math.min(1000, permille));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.reactor_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorControllerMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (cachedResult != null && cachedResult.valid()) {
            tag.putInt("val_minX", cachedResult.minX());
            tag.putInt("val_minY", cachedResult.minY());
            tag.putInt("val_minZ", cachedResult.minZ());
            tag.putInt("val_maxX", cachedResult.maxX());
            tag.putInt("val_maxY", cachedResult.maxY());
            tag.putInt("val_maxZ", cachedResult.maxZ());
            tag.putInt("val_rodCount", cachedResult.rodCount());
            tag.putInt("val_rodColumns", cachedResult.rodColumns());
            tag.putInt("val_coolantCount", cachedResult.coolantCount());
        }
        tag.putInt("stability", stabilityPermille);
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("val_minX")) {
            cachedResult = new ReactorValidation.Result(
                    true,
                    tag.getInt("val_minX"), tag.getInt("val_minY"), tag.getInt("val_minZ"),
                    tag.getInt("val_maxX"), tag.getInt("val_maxY"), tag.getInt("val_maxZ"),
                    tag.getInt("val_rodCount"), tag.getInt("val_rodColumns"), tag.getInt("val_coolantCount")
            );
        } else {
            cachedResult = null;
        }
        if (tag.contains("stability")) {
            int v = tag.getInt("stability");
            // Backward compat: old saves used 0-100 percent
            stabilityPermille = (v <= 100) ? Math.min(1000, v * 10) : Math.max(0, Math.min(1000, v));
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
