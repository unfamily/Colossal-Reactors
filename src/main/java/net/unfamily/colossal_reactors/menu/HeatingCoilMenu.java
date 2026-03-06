package net.unfamily.colossal_reactors.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.unfamily.colossal_reactors.block.HeatingCoilBlock;
import net.unfamily.colossal_reactors.blockentity.HeatingCoilBlockEntity;

import javax.annotation.Nullable;

/**
 * Menu for heating coil GUI (port-style). One input slot, fluid/energy synced via ContainerData.
 */
public class HeatingCoilMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;
    /** Block pos when opened from a block entity (server-side); used to close GUI on state change. */
    @Nullable
    private final BlockPos menuBlockPos;

    public HeatingCoilMenu(int containerId, Inventory playerInventory, HeatingCoilBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.HEATING_COIL_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = data;
        this.menuBlockPos = blockEntity.getBlockPos();
        addDataSlots(data);
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 37, 33));
        addPlayerSlots(playerInventory);
    }

    public HeatingCoilMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.HEATING_COIL_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.NULL;
        this.data = new SimpleContainerData(15);
        this.menuBlockPos = null;
        addDataSlots(data);
        addSlot(new SlotItemHandler(new net.neoforged.neoforge.items.ItemStackHandler(1), 0, 37, 33));
        addPlayerSlots(playerInventory);
    }

    /** True if this menu is for the given block pos (used to close it when coil state changes). */
    public boolean isForPosition(BlockPos pos) {
        return menuBlockPos != null && menuBlockPos.equals(pos);
    }

    /** Block pos when opened from block entity (null for client fallback menu). Used for redstone payload. */
    @Nullable
    public BlockPos getBlockPos() {
        return menuBlockPos;
    }

    private void addPlayerSlots(Inventory playerInventory) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return levelAccess.evaluate((level, pos) -> {
            if (!(level.getBlockState(pos).getBlock() instanceof HeatingCoilBlock)) return false;
            return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
        }).orElse(false);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            stack = stackInSlot.copy();
            if (index == 0) {
                if (!moveItemStackTo(stackInSlot, 1, 37, true)) return ItemStack.EMPTY;
            } else if (showItemInGui() && !moveItemStackTo(stackInSlot, 0, 1, false)) return ItemStack.EMPTY;
            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return stack;
    }

    public int getFluidAmount() { return data.get(0); }
    public int getFluidCapacity() { return data.get(1); }
    public int getFluidId() { return data.get(2); }
    public int getEnergy() { return data.get(3); }
    public int getEnergyCapacity() { return data.get(4); }
    public int getBurnableTicks() { return data.get(5); }
    public boolean showFluidInGui() { return data.get(11) != 0; }
    public boolean showEnergyInGui() { return data.get(12) != 0; }
    public boolean showItemInGui() { return data.get(13) != 0; }

    public int getRedstoneMode() { return data.get(14); }
}
