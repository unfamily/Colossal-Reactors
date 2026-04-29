package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.transfer.DelegatingResourceHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.menu.ResourcePortMenu;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for Resource Port. One item slot (insert/remove) and a fluid tank that accepts
 * and provides fluids via capability. Port mode: insert / extract / eject.
 */
public class ResourcePortBlockEntity extends BlockEntity implements MenuProvider {

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

    private final FluidStacksResourceHandler fluidStorage = new FluidStacksResourceHandler(1, getTankCapacityMb()) {
        @Override
        protected void onContentsChanged(int index, FluidStack previousContents) {
            setChanged();
        }
    };

    private PortMode portMode = PortMode.INSERT;
    private PortFilter portFilter = PortFilter.BOTH;

    @Nullable
    private ResourceHandler<ItemResource> cachedItemCapability;

    @Nullable
    private ResourceHandler<FluidResource> cachedFluidCapability;

    private final ContainerData fluidData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fluidStorage.getAmountAsInt(0);
                case 1 -> fluidStorage.getCapacityAsInt(0, FluidResource.EMPTY);
                case 2 -> {
                    FluidStack fs = FluidUtil.getStack(fluidStorage, 0);
                    yield fs.isEmpty()
                            ? -1
                            : BuiltInRegistries.FLUID.getId(fs.getFluid());
                }
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
                FluidStack current = FluidUtil.getStack(fluidStorage, 0);
                if (value <= 0) {
                    fluidStorage.set(0, FluidResource.EMPTY, 0);
                } else if (!current.isEmpty()) {
                    int cap = fluidStorage.getCapacityAsInt(0, FluidResource.EMPTY);
                    fluidStorage.set(0, FluidResource.of(current), Math.min(value, cap));
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
    public ItemStackHandler getItemStackHandler() {
        return itemHandler;
    }

    /** Legacy bucket/menu compatibility via NeoForge {@link IFluidHandler#of(ResourceHandler)}. */
    public IFluidHandler getFluidHandler() {
        return IFluidHandler.of(fluidStorage);
    }

    public FluidStacksResourceHandler getFluidStorage() {
        return fluidStorage;
    }

    /** Stored fluid for simulation logic (single tank). */
    public FluidStack getStoredFluid() {
        return FluidUtil.getStack(fluidStorage, 0);
    }

    public int getFluidAmountMb() {
        return fluidStorage.getAmountAsInt(0);
    }

    public int getFluidCapacityMb() {
        return fluidStorage.getCapacityAsInt(0, FluidResource.EMPTY);
    }

    public ResourceHandler<ItemResource> getItemResourceHandlerForCapability() {
        if (cachedItemCapability == null) {
            cachedItemCapability = LegacyItemHandlerResourceHandler.wrap(new FilteredItemHandler());
        }
        return cachedItemCapability;
    }

    public ResourceHandler<FluidResource> getFluidResourceHandlerForCapability() {
        if (cachedFluidCapability == null) {
            cachedFluidCapability = new FilteredFluidResourceHandler();
        }
        return cachedFluidCapability;
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
        FluidResource fr = FluidResource.of(stack);
        try (var tx = Transaction.openRoot()) {
            int inserted = fluidStorage.insert(0, fr, stack.getAmount(), tx);
            tx.commit();
            return inserted;
        }
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
        return fluidStorage.getAmountAsLong(0) < fluidStorage.getCapacityAsLong(0, FluidResource.EMPTY);
    }

    /**
     * Drains fluid from this port's tank for reactor coolant consumption. Only when mode is INSERT.
     * Returns amount actually drained (caller uses this for steam production).
     */
    public int takeFluidForReactor(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        if (portMode != PortMode.INSERT) return 0;
        FluidStack inTank = FluidUtil.getStack(fluidStorage, 0);
        if (inTank.isEmpty() || inTank.getFluid() != fluid) return 0;
        int drain = Math.min(amountMb, inTank.getAmount());
        if (drain <= 0) return 0;
        FluidResource template = FluidResource.of(inTank);
        try (var tx = Transaction.openRoot()) {
            int taken = fluidStorage.extract(0, template, drain, tx);
            tx.commit();
            if (taken > 0) setChanged();
            return taken;
        }
    }

    /**
     * Handles bucket/fluid container click: fill block from item or drain block to item.
     * Uses the same fill/drain rules as the port (INSERT = fill allowed, EXTRACT/EJECT = drain allowed).
     * Returns true if a transfer occurred (caller must then update player hand with itemHandler.getContainer()).
     */
    public boolean interactWithItemFluidHandler(IFluidHandlerItem itemHandlerItem, Player player) {
        if (itemHandlerItem.getTanks() == 0) return false;
        IFluidHandler blockHandler = getFluidHandler();
        FluidStack inItem = itemHandlerItem.getFluidInTank(0);
        if (!inItem.isEmpty()) {
            // Item has fluid: try to fill block (allowed in INSERT mode)
            if (blockHandler.fill(inItem.copy(), IFluidHandler.FluidAction.SIMULATE) > 0) {
                int filled = blockHandler.fill(inItem.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    itemHandlerItem.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    inItem.getFluid().getPickupSound().ifPresent(player::playSound);
                    return true;
                }
            }
        } else {
            // Item empty: try to drain block to item (allowed in EXTRACT/EJECT mode)
            FluidStack inBlock = blockHandler.getFluidInTank(0);
            if (!inBlock.isEmpty() && itemHandlerItem.isFluidValid(0, inBlock)) {
                int capacity = itemHandlerItem.getTankCapacity(0);
                FluidStack toFill = inBlock.copy();
                toFill.setAmount(Math.min(inBlock.getAmount(), capacity));
                int filled = itemHandlerItem.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
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

    /** Legacy accessor — prefer {@link #getItemStackHandler()} if not wrapping capability. */
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        Level level = getLevel();
        if (level != null && !oldState.is(level.getBlockState(pos).getBlock())) {
            dropAllContents();
        }
        super.preRemoveSideEffects(pos, oldState);
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        itemHandler.serialize(output);
        fluidStorage.serialize(output);
        output.putInt(TAG_PORT_MODE, portMode.getId());
        output.putInt(TAG_PORT_FILTER, portFilter.getId());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input);
        fluidStorage.deserialize(input);
        portMode = PortMode.fromId(input.getIntOr(TAG_PORT_MODE, portMode.getId()));
        portFilter = PortFilter.fromId(input.getIntOr(TAG_PORT_FILTER, portFilter.getId()));
    }

    /**
     * Item handler exposed to capability (hoppers/pipes). INSERT: allow insert. EXTRACT/EJECT: allow extract only.
     * {@link IItemHandlerModifiable} is required for NeoForge transactional transfers / snapshot revert (e.g. Ender IO conduits + IskaLib legacy bridge).
     */
    private final class FilteredItemHandler implements IItemHandlerModifiable {
        private boolean allowInsert() {
            return portMode == PortMode.INSERT;
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

        @Override
        public void setStackInSlot(int slot, @NotNull ItemStack stack) {
            itemHandler.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
    }

    /**
     * Fluid capability view with port mode/filter matching legacy {@link IFluidHandler} wrapper behavior.
     */
    private final class FilteredFluidResourceHandler extends DelegatingResourceHandler<FluidResource> {

        FilteredFluidResourceHandler() {
            super(fluidStorage);
        }

        private boolean allowFill() {
            return portMode == PortMode.INSERT;
        }

        private boolean allowDrain() {
            return portMode == PortMode.EXTRACT || portMode == PortMode.EJECT;
        }

        @Override
        public int insert(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (!allowFill()) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            if (!allowFill()) return 0;
            return super.insert(resource, amount, transaction);
        }

        @Override
        public int extract(int index, FluidResource resource, int amount, TransactionContext transaction) {
            if (!allowDrain()) return 0;
            return super.extract(index, resource, amount, transaction);
        }

        @Override
        public int extract(FluidResource resource, int amount, TransactionContext transaction) {
            if (!allowDrain()) return 0;
            return super.extract(resource, amount, transaction);
        }
    }
}
