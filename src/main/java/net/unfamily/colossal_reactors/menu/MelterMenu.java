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
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.MelterBlockEntity;

/**
 * Melter container. Input slot at (43, 32), player inventory at (8, 84), hotbar at (8, 142).
 */
public class MelterMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    public MelterMenu(int containerId, Inventory playerInventory, MelterBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.MELTER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = data;
        addDataSlots(data);

        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 43, 32));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public MelterMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.MELTER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.NULL;
        this.data = new SimpleContainerData(8);
        addDataSlots(data);

        addSlot(new SlotItemHandler(new net.neoforged.neoforge.items.ItemStackHandler(1), 0, 43, 32));

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
        return stillValid(levelAccess, player, ModBlocks.MELTER.get());
    }

    public int getProgress() {
        return data.get(3);
    }

    public int getMaxProgress() {
        return data.get(4);
    }

    public int getFluidAmount() {
        return data.get(0);
    }

    public int getFluidCapacity() {
        return data.get(1);
    }

    public int getFluidId() {
        return data.get(2);
    }

    public BlockPos getBlockPos() {
        return new BlockPos(data.get(5), data.get(6), data.get(7));
    }

    public boolean isForPosition(BlockPos pos) {
        return data.get(5) == pos.getX() && data.get(6) == pos.getY() && data.get(7) == pos.getZ();
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
            } else {
                if (!moveItemStackTo(stackInSlot, 0, 1, false)) return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return stack;
    }
}
