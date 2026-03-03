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
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.menu.RedstonePortMenu;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for Redstone Port. Only redstone mode (NONE, LOW, HIGH, DISABLED); no PULSE.
 * Same data/save level as iskandert_utilities redstone blocks.
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

    public void setRedstoneMode(int mode) {
        this.redstoneMode = RedstoneMode.fromId(mode).getId();
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
