package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.menu.RedstonePortMenu;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for Redstone Port. Only redstone mode (NONE, LOW, HIGH, DISABLED); no PULSE.
 * Same semantics as iskandert_utilities: NONE = ignore redstone; LOW = active when signal off; HIGH = active when signal on; DISABLED = never active.
 */
public class RedstonePortBlockEntity extends BlockEntity implements MenuProvider {

    private static final String TAG_REDSTONE_MODE = "RedstoneMode";

    private int redstoneMode = RedstoneMode.NONE.getId();

    public RedstonePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_PORT_BE.get(), pos, state);
    }

    public int getRedstoneMode() {
        return redstoneMode;
    }

    /**
     * True if this port allows the reactor to run this tick, based on neighbor redstone signal and mode.
     * NONE: always true; LOW: true when signal is 0; HIGH: true when signal &gt; 0; DISABLED: false.
     */
    public boolean isRedstoneActive(Level level) {
        if (level == null || level.isClientSide()) return false;
        int power = level.getBestNeighborSignal(worldPosition);
        boolean hasSignal = power > 0;
        return switch (RedstoneMode.fromId(redstoneMode)) {
            case NONE -> true;
            case LOW -> !hasSignal;
            case HIGH -> hasSignal;
            case PULSE -> true; // Port does not use PULSE; treat as NONE if ever set
            case DISABLED -> false;
        };
    }

    public void setRedstoneMode(int mode) {
        // Keep only NONE, LOW, HIGH, DISABLED (no PULSE for port)
        RedstoneMode m = RedstoneMode.fromId(mode);
        this.redstoneMode = (m == RedstoneMode.PULSE ? RedstoneMode.NONE : m).getId();
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.redstone_port");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new RedstonePortMenu(containerId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt(TAG_REDSTONE_MODE, redstoneMode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        redstoneMode = tag.getInt(TAG_REDSTONE_MODE);
        redstoneMode = RedstoneMode.fromId(redstoneMode).getId();
    }
}
