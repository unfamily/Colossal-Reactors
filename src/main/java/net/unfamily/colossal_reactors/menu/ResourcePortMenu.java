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
import net.unfamily.colossal_reactors.blockentity.PortFilter;
import net.unfamily.colossal_reactors.blockentity.PortMode;
import net.unfamily.colossal_reactors.blockentity.ResourcePortBlockEntity;

/**
 * Container for Resource Port GUI. One slot at (37, 33), player inventory at (8, 84) + hotbar at (8, 142).
 */
public class ResourcePortMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess levelAccess;
    private final ContainerData fluidData;

    public ResourcePortMenu(int containerId, Inventory playerInventory, ResourcePortBlockEntity blockEntity, ContainerData fluidData) {
        super(ModMenuTypes.RESOURCE_PORT_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.fluidData = fluidData;
        addDataSlots(fluidData);

        addSlot(new SlotItemHandler(blockEntity.getItemHandler(), 0, 37, 33));

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    public ResourcePortMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.RESOURCE_PORT_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.NULL;
        this.fluidData = new SimpleContainerData(8);
        addDataSlots(fluidData);

        addSlot(new SlotItemHandler(new net.neoforged.neoforge.items.ItemStackHandler(1), 0, 37, 33));

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
        return stillValid(levelAccess, player, ModBlocks.RESOURCE_PORT.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            stack = stackInSlot.copy();
            if (index == 0) {
                if (!moveItemStackTo(stackInSlot, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stackInSlot, 0, 1, false)) {
                return ItemStack.EMPTY;
            }
            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return stack;
    }

    public int getFluidAmount() {
        return fluidData.get(0);
    }

    public int getFluidCapacity() {
        return fluidData.get(1);
    }

    /** Fluid registry id for GUI tooltip (client); -1 if empty. */
    public int getFluidId() {
        return fluidData.get(2);
    }

    public PortMode getPortMode() {
        return PortMode.fromId(fluidData.get(3));
    }

    public PortFilter getPortFilter() {
        return PortFilter.fromId(fluidData.get(7));
    }

    /** Block pos synced for client (e.g. packet). */
    public BlockPos getSyncedBlockPos() {
        return new BlockPos(fluidData.get(4), fluidData.get(5), fluidData.get(6));
    }
}
