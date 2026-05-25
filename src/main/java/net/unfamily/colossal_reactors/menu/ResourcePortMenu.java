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
import net.unfamily.colossal_reactors.client.gui.ResourcePortGuiLayout;
import org.jetbrains.annotations.Nullable;

/**
 * Container for Resource Port GUI. Client menu syncs {@link ContainerData} from server (builder pattern).
 */
public class ResourcePortMenu extends AbstractContainerMenu {

    private static final int DATA_ALLOW_SOLID = 7;
    private static final int DATA_ALLOW_LIQUID = 8;
    private static final int DATA_ALLOW_GAS = 9;
    private static final int DATA_PORT_FILTER = 10;
    /** Must match {@link ResourcePortBlockEntity} fluid data slot count. */
    public static final int DATA_COUNT = 11;

    private final ContainerLevelAccess levelAccess;
    private final ContainerData fluidData;
    @Nullable
    private final ResourcePortBlockEntity blockEntity;

    public ResourcePortMenu(int containerId, Inventory playerInventory, ResourcePortBlockEntity blockEntity,
                            ContainerData fluidData) {
        super(ModMenuTypes.RESOURCE_PORT_MENU.get(), containerId);
        this.blockEntity = blockEntity;
        this.levelAccess = ContainerLevelAccess.create(blockEntity.getLevel(), blockEntity.getBlockPos());
        this.fluidData = fluidData;
        addDataSlots(fluidData);
        addPortSlots(blockEntity, playerInventory);
    }

    public ResourcePortMenu(int containerId, Inventory playerInventory) {
        super(ModMenuTypes.RESOURCE_PORT_MENU.get(), containerId);
        this.blockEntity = null;
        this.levelAccess = ContainerLevelAccess.NULL;
        this.fluidData = new SimpleContainerData(DATA_COUNT);
        addDataSlots(fluidData);
        addPortSlots(null, playerInventory);
    }

    private void addPortSlots(@Nullable ResourcePortBlockEntity port, Inventory playerInventory) {
        if (port != null) {
            addSlot(new SlotItemHandler(port.getItemStackHandler(), 0, ResourcePortGuiLayout.ITEM_SLOT_X,
                    ResourcePortGuiLayout.ITEM_SLOT_Y));
        } else {
            addSlot(new SlotItemHandler(new net.neoforged.neoforge.items.ItemStackHandler(1), 0,
                    ResourcePortGuiLayout.ITEM_SLOT_X, ResourcePortGuiLayout.ITEM_SLOT_Y));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 94 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 152));
        }
    }

    @Nullable
    public ResourcePortBlockEntity getBlockEntity() {
        return blockEntity;
    }

    public BlockPos getBlockPos() {
        if (blockEntity != null) {
            return blockEntity.getBlockPos();
        }
        return getSyncedBlockPos();
    }

    public BlockPos getSyncedBlockPos() {
        return new BlockPos(fluidData.get(4), fluidData.get(5), fluidData.get(6));
    }

    @Override
    public boolean stillValid(Player player) {
        return levelAccess.evaluate((level, pos) -> {
            var state = level.getBlockState(pos);
            if (!state.is(ModBlocks.RESOURCE_PORT.get()) && !state.is(ModBlocks.TURBINE_RESOURCE_PORT.get())) {
                return false;
            }
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

    public int getFluidId() {
        return fluidData.get(2);
    }

    public PortMode getPortMode() {
        return PortMode.fromId(fluidData.get(3));
    }

    public boolean isAllowSolid() {
        return fluidData.get(DATA_ALLOW_SOLID) != 0;
    }

    public boolean isAllowLiquid() {
        return fluidData.get(DATA_ALLOW_LIQUID) != 0;
    }

    public boolean isAllowGas() {
        return fluidData.get(DATA_ALLOW_GAS) != 0;
    }

    public int getGasAmount() {
        return 0;
    }

    public int getGasCapacity() {
        return 0;
    }

    public boolean isTurbinePort() {
        if (blockEntity != null) {
            return blockEntity.getBlockState().is(ModBlocks.TURBINE_RESOURCE_PORT.get());
        }
        return levelAccess.evaluate((level, pos) ->
                level.getBlockState(pos).is(ModBlocks.TURBINE_RESOURCE_PORT.get())).orElse(false);
    }

    public PortFilter getPortFilter() {
        return PortFilter.fromId(fluidData.get(DATA_PORT_FILTER));
    }
}
