package net.unfamily.colossal_reactors.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.ReactorBuilderBlockEntity;

/**
 * Menu for Reactor Builder GUI. Texture 230x230; all slot X positions +27px from left (buffer, player, hotbar).
 */
public class ReactorBuilderMenu extends AbstractContainerMenu {

    private static final int BUFFER_ROWS = 3;
    private static final int BUFFER_COLS = 9;
    private static final int BUFFER_SLOTS = BUFFER_ROWS * BUFFER_COLS;

    /** Buffer slot positions (top-left of 9x3 grid). */
    private static final int BUFFER_X = 35;
    private static final int BUFFER_Y = 82;

    /** Player inventory (3x9) and hotbar (1x9). */
    private static final int PLAYER_X = 35;
    private static final int PLAYER_Y = 148;
    private static final int HOTBAR_Y = 206;

    private final ContainerLevelAccess levelAccess;
    private final ContainerData fluidData;
    private final ContainerData sizeData;

    public ReactorBuilderMenu(int containerId, Inventory playerInventory, ReactorBuilderBlockEntity blockEntity) {
        super(ModMenuTypes.REACTOR_BUILDER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.fluidData = blockEntity.getFluidData();
        this.sizeData = blockEntity.getSizeData();
        addDataSlots(fluidData);
        addDataSlots(sizeData);

        // Buffer 9x3
        for (int row = 0; row < BUFFER_ROWS; row++) {
            for (int col = 0; col < BUFFER_COLS; col++) {
                addSlot(new SlotItemHandler(blockEntity.getBufferHandler(), col + row * BUFFER_COLS,
                        BUFFER_X + col * 18, BUFFER_Y + row * 18));
            }
        }

        // Player main inventory 3x9
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_X + col * 18, PLAYER_Y + row * 18));
            }
        }
        // Hotbar
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_X + col * 18, HOTBAR_Y));
        }
    }

    /** Client-side constructor: dummy buffer handler and fluid data (server syncs contents). */
    public ReactorBuilderMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.REACTOR_BUILDER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.NULL;
        this.fluidData = new SimpleContainerData(3);
        this.sizeData = new SimpleContainerData(7);
        addDataSlots(fluidData);
        addDataSlots(sizeData);
        ItemStackHandler dummyBuffer = new ItemStackHandler(BUFFER_SLOTS);

        for (int row = 0; row < BUFFER_ROWS; row++) {
            for (int col = 0; col < BUFFER_COLS; col++) {
                addSlot(new SlotItemHandler(dummyBuffer, col + row * BUFFER_COLS,
                        BUFFER_X + col * 18, BUFFER_Y + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, PLAYER_X + col * 18, PLAYER_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, PLAYER_X + col * 18, HOTBAR_Y));
        }
    }

    public int getFluidAmount() { return fluidData.get(0); }
    public int getFluidCapacity() { return fluidData.get(1); }
    /** Fluid registry id for GUI; -1 if empty. */
    public int getFluidId() { return fluidData.get(2); }

    public int getSizeLeft() { return sizeData.get(0); }
    public int getSizeRight() { return sizeData.get(1); }
    public int getSizeH() { return sizeData.get(2); }
    public int getSizeD() { return sizeData.get(3); }
    /** Block pos synced via sizeData indices 4,5,6 (for client button payloads). */
    public BlockPos getBlockPos() {
        return new BlockPos(sizeData.get(4), sizeData.get(5), sizeData.get(6));
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.REACTOR_BUILDER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack stack = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            stack = stackInSlot.copy();
            if (index < BUFFER_SLOTS) {
                if (!moveItemStackTo(stackInSlot, BUFFER_SLOTS, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stackInSlot, 0, BUFFER_SLOTS, false)) {
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
}
