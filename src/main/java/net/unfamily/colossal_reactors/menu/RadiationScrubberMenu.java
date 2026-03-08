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
import net.unfamily.colossal_reactors.blockentity.RadiationScrubberBlockEntity;

/**
 * Radiation Scrubber container. Slot 0 (catalyst) at (44, 33), Slot 1 at (80, 33), player inventory at (8, 84), hotbar at (8, 142).
 * Data indices 3,4 = energy, energyCapacity.
 */
public class RadiationScrubberMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    public RadiationScrubberMenu(int containerId, Inventory playerInventory, RadiationScrubberBlockEntity blockEntity, ContainerData data) {
        super(ModMenuTypes.RADIATION_SCRUBBER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = data;
        addDataSlots(data);

        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 44, 33));
        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 1, 80, 33));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public RadiationScrubberMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.RADIATION_SCRUBBER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.NULL;
        this.data = new SimpleContainerData(5);
        addDataSlots(data);

        var emptyHandler = new net.neoforged.neoforge.items.ItemStackHandler(2);
        addSlot(new SlotItemHandler(emptyHandler, 0, 44, 33));
        addSlot(new SlotItemHandler(emptyHandler, 1, 80, 33));

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
        return stillValid(levelAccess, player, ModBlocks.RADIATION_SCRUBBER.get());
    }

    public BlockPos getBlockPos() {
        return new BlockPos(data.get(0), data.get(1), data.get(2));
    }

    public int getEnergy() {
        return data.get(3);
    }

    public int getEnergyCapacity() {
        return data.get(4);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            stack = stackInSlot.copy();
            if (index < 2) {
                if (!moveItemStackTo(stackInSlot, 2, 38, true)) return ItemStack.EMPTY;
            } else {
                if (!moveItemStackTo(stackInSlot, 0, 2, false)) return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
        }
        return stack;
    }
}
