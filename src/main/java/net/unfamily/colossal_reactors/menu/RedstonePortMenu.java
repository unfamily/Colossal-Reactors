package net.unfamily.colossal_reactors.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.blockentity.RedstonePortBlockEntity;

/**
 * Menu for Redstone Port. No slots; only ContainerData for redstone mode and block pos.
 */
public class RedstonePortMenu extends AbstractContainerMenu {

    private final ContainerLevelAccess levelAccess;
    private final ContainerData containerData;

    private static final int REDSTONE_MODE_INDEX = 0;
    private static final int POS_X_INDEX = 1;
    private static final int POS_Y_INDEX = 2;
    private static final int POS_Z_INDEX = 3;
    private static final int DATA_COUNT = 4;

    public RedstonePortMenu(int containerId, Inventory playerInventory, RedstonePortBlockEntity blockEntity) {
        super(ModMenuTypes.REDSTONE_PORT_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.containerData = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case REDSTONE_MODE_INDEX -> blockEntity.getRedstoneMode();
                    case POS_X_INDEX -> blockEntity.getBlockPos().getX();
                    case POS_Y_INDEX -> blockEntity.getBlockPos().getY();
                    case POS_Z_INDEX -> blockEntity.getBlockPos().getZ();
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
        addDataSlots(containerData);
    }

    public RedstonePortMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.REDSTONE_PORT_MENU.get(), containerId);
        this.levelAccess = ContainerLevelAccess.NULL;
        this.containerData = new SimpleContainerData(DATA_COUNT);
        addDataSlots(containerData);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(levelAccess, player, ModBlocks.REDSTONE_PORT.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    public int getRedstoneMode() {
        return containerData.get(REDSTONE_MODE_INDEX);
    }

    public BlockPos getSyncedBlockPos() {
        return new BlockPos(containerData.get(POS_X_INDEX), containerData.get(POS_Y_INDEX), containerData.get(POS_Z_INDEX));
    }
}
