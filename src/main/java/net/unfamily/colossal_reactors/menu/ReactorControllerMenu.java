package net.unfamily.colossal_reactors.menu;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ControllerState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.ReactorControllerBlock;
import net.unfamily.colossal_reactors.blockentity.ReactorControllerBlockEntity;

/**
 * Menu for Reactor OS GUI when controller is ON. Syncs reactor stats and state for the dark panel.
 */
public class ReactorControllerMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess levelAccess;
    private final ContainerData data;

    private static final int INDEX_STATE = 0;
    private static final int INDEX_ROD_COUNT = 1;
    private static final int INDEX_ROD_COLUMNS = 2;
    private static final int INDEX_COOLANT = 3;
    private static final int INDEX_HAS_FUEL = 4;
    private static final int DATA_COUNT = 5;

    public ReactorControllerMenu(int containerId, Inventory playerInventory, ReactorControllerBlockEntity blockEntity) {
        super(ModMenuTypes.REACTOR_CONTROLLER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                var result = blockEntity.getCachedResult();
                var state = blockEntity.getLevel() != null
                        ? blockEntity.getLevel().getBlockState(blockEntity.getBlockPos()).getValue(ReactorControllerBlock.STATE)
                        : ControllerState.OFF;
                return switch (index) {
                    case INDEX_STATE -> state.ordinal();
                    case INDEX_ROD_COUNT -> result != null ? result.rodCount() : 0;
                    case INDEX_ROD_COLUMNS -> result != null ? result.rodColumns() : 0;
                    case INDEX_COOLANT -> result != null ? result.coolantCount() : 0;
                    case INDEX_HAS_FUEL -> 0; // TODO when rod fuel is implemented
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
        addDataSlots(data);
    }

    public ReactorControllerMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.REACTOR_CONTROLLER_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.NULL;
        this.data = new SimpleContainerData(DATA_COUNT);
        addDataSlots(data);
    }

    public int getControllerStateId() { return data.get(INDEX_STATE); }
    public int getRodCount() { return data.get(INDEX_ROD_COUNT); }
    public int getRodColumns() { return data.get(INDEX_ROD_COLUMNS); }
    public int getCoolantCount() { return data.get(INDEX_COOLANT); }
    public boolean hasFuel() { return data.get(INDEX_HAS_FUEL) != 0; }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.REACTOR_CONTROLLER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
