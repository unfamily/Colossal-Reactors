package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
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
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.unfamily.colossal_reactors.menu.ReactorControllerMenu;
import net.unfamily.colossal_reactors.reactor.ReactorValidation;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
            lastInteractingPlayer.sendSystemMessage(message, true);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (cachedResult != null && cachedResult.valid()) {
            output.putInt("val_minX", cachedResult.minX());
            output.putInt("val_minY", cachedResult.minY());
            output.putInt("val_minZ", cachedResult.minZ());
            output.putInt("val_maxX", cachedResult.maxX());
            output.putInt("val_maxY", cachedResult.maxY());
            output.putInt("val_maxZ", cachedResult.maxZ());
            output.putInt("val_rodCount", cachedResult.rodCount());
            output.putInt("val_rodColumns", cachedResult.rodColumns());
            output.putInt("val_coolantCount", cachedResult.coolantCount());
        }
        output.putInt("stability", stabilityPermille);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Optional<Integer> minX = input.getInt("val_minX");
        if (minX.isPresent()) {
            cachedResult = new ReactorValidation.Result(
                    true,
                    minX.get(),
                    input.getIntOr("val_minY", 0),
                    input.getIntOr("val_minZ", 0),
                    input.getIntOr("val_maxX", 0),
                    input.getIntOr("val_maxY", 0),
                    input.getIntOr("val_maxZ", 0),
                    input.getIntOr("val_rodCount", 0),
                    input.getIntOr("val_rodColumns", 0),
                    input.getIntOr("val_coolantCount", 0)
            );
        } else {
            cachedResult = null;
        }
        input.getInt("stability").ifPresent(v -> {
            stabilityPermille = (v <= 100) ? Math.min(1000, v * 10) : Math.max(0, Math.min(1000, v));
        });
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
