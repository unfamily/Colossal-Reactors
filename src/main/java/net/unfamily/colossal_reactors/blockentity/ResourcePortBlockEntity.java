package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.menu.ResourcePortMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for Resource Port. One item slot (insert/remove) and a fluid tank that accepts
 * and provides fluids via capability. Port mode: insert / extract / eject.
 */
public class ResourcePortBlockEntity extends BlockEntity implements MenuProvider {

    private static final String TAG_ITEMS = "Items";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_FLUID_ID = "FluidId";
    private static final String TAG_FLUID_AMOUNT = "Amount";
    private static final String TAG_PORT_MODE = "PortMode";
    private static final String TAG_PORT_FILTER = "PortFilter";
    private static final int SLOT_SIZE = 1;

    private static int getTankCapacityMb() {
        return Config.RESOURCE_PORT_TANK_CAPACITY_MB.get();
    }

    private static final int DATA_MODE = 3;
    private static final int DATA_POS_X = 4;
    private static final int DATA_POS_Y = 5;
    private static final int DATA_POS_Z = 6;
    private static final int DATA_FILTER = 7;
    private static final int DATA_COUNT = 8;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FluidTank fluidTank = new FluidTank(getTankCapacityMb()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private PortMode portMode = PortMode.INSERT;
    private PortFilter portFilter = PortFilter.BOTH;

    private final ContainerData fluidData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fluidTank.getFluidAmount();
                case 1 -> fluidTank.getCapacity();
                case 2 -> fluidTank.getFluid().isEmpty()
                        ? -1
                        : BuiltInRegistries.FLUID.getId(fluidTank.getFluid().getFluid());
                case DATA_MODE -> portMode.getId();
                case DATA_POS_X -> worldPosition.getX();
                case DATA_POS_Y -> worldPosition.getY();
                case DATA_POS_Z -> worldPosition.getZ();
                case DATA_FILTER -> portFilter.getId();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                FluidStack current = fluidTank.getFluid();
                if (value <= 0) {
                    fluidTank.setFluid(FluidStack.EMPTY);
                } else if (!current.isEmpty()) {
                    fluidTank.setFluid(new FluidStack(current.getFluid(), Math.min(value, fluidTank.getCapacity())));
                }
            }
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public ResourcePortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RESOURCE_PORT_BE.get(), pos, state);
    }

    /** Raw handler for menu and internal use (no mode/filter restriction). */
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /** Raw handler for menu and bucket interaction (no mode/filter restriction). */
    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    /**
     * Called by the reactor to push solid waste into this port (only when mode is EXTRACT or EJECT and slot is not full).
     * Returns the stack that could not be inserted (remaining).
     */
    @NotNull
    public ItemStack receiveItemFromReactor(ItemStack stack) {
        if (stack.isEmpty() || (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT)) return stack;
        return itemHandler.insertItem(0, stack, false);
    }

    /**
     * Called by the reactor to push fluid (e.g. steam) into this port (only when mode is EXTRACT or EJECT and tank has space).
     * Returns the amount in mB that was accepted.
     */
    public int receiveFluidFromReactor(FluidStack stack) {
        if (stack.isEmpty() || (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT)) return 0;
        return fluidTank.fill(stack, IFluidHandler.FluidAction.EXECUTE);
    }

    /** True if this port is in EXTRACT or EJECT and can accept items from the reactor (slot not full). */
    public boolean canAcceptItemFromReactor() {
        if (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT) return false;
        ItemStack inSlot = itemHandler.getStackInSlot(0);
        return inSlot.isEmpty() || (inSlot.getCount() < inSlot.getMaxStackSize());
    }

    /** True if this port is in EXTRACT or EJECT and has fluid tank space. */
    public boolean canAcceptFluidFromReactor() {
        if (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT) return false;
        return fluidTank.getFluidAmount() < fluidTank.getCapacity();
    }

    /**
     * Drains fluid from this port's tank for reactor coolant consumption. Only when mode is INSERT and filter allows fluid.
     * Returns amount actually drained (caller uses this for steam production).
     */
    public int takeFluidForReactor(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        if (portMode != PortMode.INSERT) return 0;
        if (portFilter != PortFilter.BOTH && portFilter != PortFilter.ONLY_COOLANT_LIQUID) return 0;
        FluidStack inTank = fluidTank.getFluid();
        if (inTank.isEmpty() || inTank.getFluid() != fluid) return 0;
        int drain = Math.min(amountMb, inTank.getAmount());
        if (drain <= 0) return 0;
        fluidTank.drain(drain, IFluidHandler.FluidAction.EXECUTE);
        setChanged();
        return drain;
    }

    /**
     * Handles bucket/fluid container click: fill block from item or drain block to item.
     * Uses the same fill/drain rules as the port (INSERT = fill allowed, EXTRACT/EJECT = drain allowed).
     * Returns true if a transfer occurred (caller must then update player hand with itemHandler.getContainer()).
     */
    public boolean interactWithItemFluidHandler(IFluidHandlerItem itemHandler, Player player) {
        if (itemHandler.getTanks() == 0) return false;
        IFluidHandler blockHandler = getFluidHandler();
        FluidStack inItem = itemHandler.getFluidInTank(0);
        if (!inItem.isEmpty()) {
            // Item has fluid: try to fill block (allowed in INSERT mode)
            if (blockHandler.fill(inItem.copy(), IFluidHandler.FluidAction.SIMULATE) > 0) {
                int filled = blockHandler.fill(inItem.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    inItem.getFluid().getPickupSound().ifPresent(player::playSound);
                    return true;
                }
            }
        } else {
            // Item empty: try to drain block to item (allowed in EXTRACT/EJECT mode)
            FluidStack inBlock = blockHandler.getFluidInTank(0);
            if (!inBlock.isEmpty() && itemHandler.isFluidValid(0, inBlock)) {
                int capacity = itemHandler.getTankCapacity(0);
                FluidStack toFill = inBlock.copy();
                toFill.setAmount(Math.min(inBlock.getAmount(), capacity));
                int filled = itemHandler.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    blockHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    var soundEvent = inBlock.getFluid().getFluidType().getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_EMPTY);
                    if (soundEvent != null) player.playSound(soundEvent);
                    return true;
                }
            }
        }
        return false;
    }

    /** Item handler for capability (hoppers/pipes): respects port mode and filter. */
    public IItemHandler getItemHandlerForCapability() {
        return new FilteredItemHandler();
    }

    /** Fluid handler for capability (hoppers/pipes): respects port mode and filter. */
    public IFluidHandler getFluidHandlerForCapability() {
        return new FilteredFluidHandler();
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    /**
     * Drops all items from the item handler into the world. Call when the block is broken.
     */
    public void dropAllContents() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        BlockPos pos = getBlockPos();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    public PortMode getPortMode() {
        return portMode;
    }

    public void setPortMode(PortMode mode) {
        this.portMode = mode;
        setChanged();
    }

    public PortFilter getPortFilter() {
        return portFilter;
    }

    public void setPortFilter(PortFilter filter) {
        this.portFilter = filter;
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.resource_port");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ResourcePortMenu(containerId, playerInventory, this, fluidData);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_ITEMS, itemHandler.serializeNBT(registries));
        tag.putInt(TAG_PORT_MODE, portMode.getId());
        tag.putInt(TAG_PORT_FILTER, portFilter.getId());
        FluidStack stack = fluidTank.getFluid();
        if (!stack.isEmpty()) {
            CompoundTag fluidTag = new CompoundTag();
            fluidTag.putString(TAG_FLUID_ID, BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
            fluidTag.putInt(TAG_FLUID_AMOUNT, stack.getAmount());
            tag.put(TAG_FLUID, fluidTag);
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound(TAG_ITEMS));
        portMode = PortMode.fromId(tag.getInt(TAG_PORT_MODE));
        portFilter = PortFilter.fromId(tag.getInt(TAG_PORT_FILTER));
        fluidTank.setFluid(FluidStack.EMPTY);
        if (tag.contains(TAG_FLUID)) {
            CompoundTag fluidTag = tag.getCompound(TAG_FLUID);
            Fluid fluid = BuiltInRegistries.FLUID.get(ResourceLocation.parse(fluidTag.getString(TAG_FLUID_ID)));
            int amount = fluidTag.getInt(TAG_FLUID_AMOUNT);
            if (fluid != null && fluid != Fluids.EMPTY && amount > 0) {
                fluidTank.setFluid(new FluidStack(fluid, amount));
            }
        }
    }

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    /**
     * Item handler exposed to capability (hoppers/pipes). INSERT: allow insert only (filter allows items).
     * EXTRACT/EJECT: allow extract only.
     */
    private final class FilteredItemHandler implements IItemHandler {
        private boolean allowInsert() {
            return portMode == PortMode.INSERT
                    && (portFilter == PortFilter.BOTH || portFilter == PortFilter.ONLY_SOLID_FUEL);
        }

        private boolean allowExtract() {
            return portMode == PortMode.EXTRACT || portMode == PortMode.EJECT;
        }

        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        @NotNull
        public ItemStack getStackInSlot(int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Override
        @NotNull
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (!allowInsert()) {
                return stack;
            }
            return itemHandler.insertItem(slot, stack, simulate);
        }

        @Override
        @NotNull
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!allowExtract()) {
                return ItemStack.EMPTY;
            }
            return itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return allowInsert() && itemHandler.isItemValid(slot, stack);
        }
    }

    /**
     * Fluid handler exposed to capability (hoppers/pipes). INSERT: allow fill only (filter allows fluid).
     * EXTRACT/EJECT: allow drain only.
     */
    private final class FilteredFluidHandler implements IFluidHandler {
        private boolean allowFill() {
            return portMode == PortMode.INSERT
                    && (portFilter == PortFilter.BOTH || portFilter == PortFilter.ONLY_COOLANT_LIQUID);
        }

        private boolean allowDrain() {
            return portMode == PortMode.EXTRACT || portMode == PortMode.EJECT;
        }

        @Override
        public int getTanks() {
            return fluidTank.getTanks();
        }

        @Override
        @NotNull
        public FluidStack getFluidInTank(int tank) {
            return fluidTank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidTank.getTankCapacity(tank);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!allowFill()) {
                return 0;
            }
            return fluidTank.fill(resource, action);
        }

        @Override
        @NotNull
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (!allowDrain()) {
                return FluidStack.EMPTY;
            }
            return fluidTank.drain(resource, action);
        }

        @Override
        @NotNull
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (!allowDrain()) {
                return FluidStack.EMPTY;
            }
            return fluidTank.drain(maxDrain, action);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
            return allowFill() && fluidTank.isFluidValid(tank, stack);
        }
    }
}
