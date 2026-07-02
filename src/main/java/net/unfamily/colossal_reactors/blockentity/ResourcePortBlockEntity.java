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
import net.minecraft.world.level.block.entity.BlockEntityType;
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
import net.minecraft.core.RegistryAccess;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;
import net.unfamily.colossal_reactors.menu.ResourcePortMenu;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

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

    protected int tankCapacityMb() {
        return Config.RESOURCE_PORT_TANK_CAPACITY_MB.get();
    }

    private static final int DATA_MODE = 3;
    private static final int DATA_POS_X = 4;
    private static final int DATA_POS_Y = 5;
    private static final int DATA_POS_Z = 6;
    private static final int DATA_ALLOW_SOLID = 7;
    private static final int DATA_ALLOW_LIQUID = 8;
    private static final int DATA_ALLOW_GAS = 9;
    private static final int DATA_GAS_AMOUNT = 10;
    private static final int DATA_GAS_CAPACITY = 11;
    private static final int DATA_GAS_TYPE_LENGTH = 12;
    private static final int DATA_GAS_TYPE_START = 13;
    private static final int DATA_GAS_TYPE_INTS = 16;
    private static final int DATA_PORT_FILTER = DATA_GAS_TYPE_START + DATA_GAS_TYPE_INTS;
    private static final int DATA_IS_TURBINE = DATA_PORT_FILTER + 1;
    private static final int DATA_COUNT = DATA_IS_TURBINE + 1;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private FluidStacksResourceHandler fluidStorage = createFluidStorage(tankCapacityMb());

    private FluidStacksResourceHandler createFluidStorage(int capacityMb) {
        return new FluidStacksResourceHandler(1, capacityMb) {
            @Override
            protected void onContentsChanged(int index, FluidStack previousContents) {
                setChanged();
            }
        };
    }

    private PortMode portMode = PortMode.INSERT;
    private PortFilter portFilter = PortFilter.BOTH;
    private final PortMediumFlags mediumFlags = new PortMediumFlags();

    @Nullable
    private ResourceHandler<ItemResource> cachedItemCapability;

    @Nullable
    private ResourceHandler<FluidResource> cachedFluidCapability;

    /** Mek chemical tank handler (lazy). */
    @Nullable
    private Object chemicalHandler;

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
                case DATA_ALLOW_SOLID -> mediumFlags.isAllowSolid() ? 1 : 0;
                case DATA_ALLOW_LIQUID -> mediumFlags.isAllowLiquid() ? 1 : 0;
                case DATA_ALLOW_GAS -> mediumFlags.isAllowGas() ? 1 : 0;
                case DATA_GAS_AMOUNT -> getGasAmountMb();
                case DATA_GAS_CAPACITY -> getGasCapacityMb();
                case DATA_PORT_FILTER -> portFilter.getId();
                case DATA_IS_TURBINE -> isTurbineResourcePort() ? 1 : 0;
                default -> {
                    if (index == DATA_GAS_TYPE_LENGTH) {
                        String name = getGasTypeRegistryName();
                        yield name != null ? name.length() : 0;
                    }
                    if (index >= DATA_GAS_TYPE_START && index < DATA_GAS_TYPE_START + DATA_GAS_TYPE_INTS) {
                        String name = getGasTypeRegistryName();
                        if (name == null || name.isEmpty()) yield 0;
                        int base = (index - DATA_GAS_TYPE_START) * 4;
                        int v = 0;
                        for (int i = 0; i < 4 && base + i < name.length(); i++) {
                            v |= (name.charAt(base + i) & 0xFF) << (i * 8);
                        }
                        yield v;
                    }
                    yield 0;
                }
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
        this(ModBlockEntities.RESOURCE_PORT_BE.get(), pos, state);
    }

    protected ResourcePortBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
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

    /** Server: discard all fluid in the internal tank (GUI dump). @return true if any fluid was removed */
    public boolean dumpFluidTankContents() {
        if (level == null || level.isClientSide()) return false;
        if (fluidStorage.getAmountAsInt(0) <= 0) return false;
        fluidStorage.set(0, FluidResource.EMPTY, 0);
        setChanged();
        return true;
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
        if (!mediumFlags.isAllowSolid()) return stack;
        return itemHandler.insertItem(0, stack, false);
    }

    /**
     * Called by the reactor to push fluid (e.g. steam) into this port (only when mode is EXTRACT or EJECT and tank has space).
     * Returns the amount in mB that was accepted.
     */
    public int receiveFluidFromReactor(FluidStack stack) {
        if (stack.isEmpty() || (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT)) return 0;
        if (!mediumFlags.isAllowLiquid() || mediumFlags.isAllowGas()) return 0;
        FluidResource fr = FluidResource.of(stack);
        try (var tx = Transaction.openRoot()) {
            int inserted = fluidStorage.insert(0, fr, stack.getAmount(), tx);
            tx.commit();
            return inserted;
        }
    }

    /** Push Mek gas into port (EXTRACT/EJECT, gas toggle on). Returns mB accepted. */
    public int receiveGasFromReactor(Object chemicalStack) {
        if (!MekChemicalHelper.isLoaded() || MekChemicalHelper.isEmpty(chemicalStack)) return 0;
        if (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT) return 0;
        if (!mediumFlags.isAllowGas() || mediumFlags.isAllowLiquid()) return 0;
        Object handler = getChemicalHandler();
        if (handler == null) return 0;
        return MekChemicalHelper.fill(handler, chemicalStack, false);
    }

    @Nullable
    public Object getChemicalHandlerForCapability() {
        if (!MekChemicalHelper.isLoaded()) return null;
        return wrapFilteredChemicalHandler(getChemicalHandler());
    }

    @Nullable
    public Object getChemicalHandler() {
        if (!MekChemicalHelper.isLoaded()) return null;
        if (chemicalHandler == null) {
            Object tank = MekChemicalHelper.createBasicTank(tankCapacityMb());
            chemicalHandler = tank != null ? MekChemicalHelper.wrapAsHandler(tank) : null;
        }
        return chemicalHandler;
    }

    public int getGasAmountMb() {
        Object h = getChemicalHandler();
        return h != null ? MekChemicalHelper.getTankAmount(h) : 0;
    }

    public int getGasCapacityMb() {
        Object h = getChemicalHandler();
        return h != null ? MekChemicalHelper.getTankCapacity(h) : 0;
    }

    @Nullable
    public String getGasTypeRegistryName() {
        Object h = getChemicalHandler();
        if (h == null) return null;
        try {
            Object stack = h.getClass().getMethod("getChemicalInTank", int.class).invoke(h, 0);
            return MekChemicalHelper.getTypeRegistryName(stack);
        } catch (Throwable e) {
            return null;
        }
    }

    public boolean canAcceptGasFromReactor() {
        if (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT) return false;
        if (!mediumFlags.isAllowGas() || mediumFlags.isAllowLiquid()) return false;
        return getGasAmountMb() < getGasCapacityMb();
    }

    public long getGasSpaceMb() {
        return Math.max(0, (long) getGasCapacityMb() - getGasAmountMb());
    }

    /** True if this port is in EXTRACT or EJECT and can accept items from the reactor (slot not full). */
    public boolean canAcceptItemFromReactor() {
        if (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT) return false;
        if (!mediumFlags.isAllowSolid()) return false;
        ItemStack inSlot = itemHandler.getStackInSlot(0);
        return inSlot.isEmpty() || (inSlot.getCount() < inSlot.getMaxStackSize());
    }

    /** True if this port is in EXTRACT or EJECT and has fluid tank space. */
    public boolean canAcceptFluidFromReactor() {
        if (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT) return false;
        if (!mediumFlags.isAllowLiquid() || mediumFlags.isAllowGas()) return false;
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

    /** Drain gas from port tank for reactor/turbine (INSERT mode). Medium toggles gate pipes, not multiblock pull. */
    public int takeGasForReactor(Object templateStack, int amountMb) {
        if (!MekChemicalHelper.isLoaded() || amountMb <= 0 || MekChemicalHelper.isEmpty(templateStack)) {
            return 0;
        }
        if (portMode != PortMode.INSERT) {
            return 0;
        }
        Object handler = getChemicalHandler();
        if (handler == null) {
            return 0;
        }
        int drained = MekChemicalHelper.extractFromTank(handler, 0, amountMb, templateStack);
        if (drained > 0) {
            setChanged();
        }
        return drained;
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
     * Drops items and vents gas tank contents when the block is broken (radioactive gas → Mek radiation).
     */
    public void dropAllContents() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        BlockPos pos = getBlockPos();
        releaseGasTankOnBreak(level, pos);
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                Block.popResource(level, pos, stack);
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    private void releaseGasTankOnBreak(Level level, BlockPos pos) {
        if (!MekChemicalHelper.isLoaded()) {
            return;
        }
        Object handler = getChemicalHandler();
        if (handler == null || MekChemicalHelper.getTankAmount(handler) <= 0) {
            return;
        }
        MekChemicalHelper.dumpRadiationFromHandler(level, pos, handler, true);
    }

    public PortMode getPortMode() {
        return portMode;
    }

    public void setPortMode(PortMode mode) {
        this.portMode = mode;
        setChanged();
    }

    /** True for {@link TurbineResourcePortBlockEntity}; synced to the client for GUI layout. */
    protected boolean isTurbineResourcePort() {
        return false;
    }

    public PortFilter getPortFilter() {
        return portFilter;
    }

    public void setPortFilter(PortFilter filter) {
        if (filter == null) {
            return;
        }
        this.portFilter = filter;
        setChanged();
    }

    public boolean isAllowSolid() {
        return mediumFlags.isAllowSolid();
    }

    public boolean isAllowLiquid() {
        return mediumFlags.isAllowLiquid();
    }

    public boolean isAllowGas() {
        return mediumFlags.isAllowGas();
    }

    public void setAllowSolid(boolean allow) {
        mediumFlags.setAllowSolid(allow);
        setChanged();
    }

    public void setAllowLiquid(boolean allow) {
        mediumFlags.setAllowLiquid(allow);
        setChanged();
    }

    public void setAllowGas(boolean allow) {
        mediumFlags.setAllowGas(allow);
        setChanged();
    }

    /** Server: resize fluid tank (clamps existing contents). */
    public void applyTankCapacityMb(int capacityMb) {
        if (capacityMb <= 0 || level == null || level.isClientSide()) {
            return;
        }
        int fluidCap = fluidStorage.getCapacityAsInt(0, FluidResource.EMPTY);
        if (fluidCap != capacityMb) {
            FluidStack stored = FluidUtil.getStack(fluidStorage, 0);
            fluidStorage = createFluidStorage(capacityMb);
            cachedFluidCapability = null;
            if (!stored.isEmpty()) {
                fluidStorage.set(0, FluidResource.of(stored), Math.min(stored.getAmount(), capacityMb));
            }
        }
        if (MekChemicalHelper.isLoaded()) {
            int gasCap = getGasCapacityMb();
            if (gasCap != capacityMb) {
                Object savedStack = null;
                Object existing = getChemicalHandler();
                if (existing != null) {
                    try {
                        Object inTank = existing.getClass().getMethod("getChemicalInTank", int.class).invoke(existing, 0);
                        if (!MekChemicalHelper.isEmpty(inTank)) {
                            savedStack = inTank;
                        }
                    } catch (Throwable ignored) {
                    }
                }
                chemicalHandler = MekChemicalHelper.createBasicTank(capacityMb);
                if (chemicalHandler != null) {
                    chemicalHandler = MekChemicalHelper.wrapAsHandler(chemicalHandler);
                    if (savedStack != null) {
                        MekChemicalHelper.fill(chemicalHandler, savedStack, false);
                    }
                }
            }
        }
        setChanged();
    }

    /** True when the gas tank holds a Mek chemical with {@code isRadioactive()} (dump disabled in GUI). */
    public boolean isStoredGasRadioactive() {
        if (!MekChemicalHelper.isLoaded()) {
            return false;
        }
        Object handler = getChemicalHandler();
        return handler != null && MekChemicalHelper.isRadioactiveInTank(handler);
    }

    /** GUI dump is allowed only for non-empty, non-radioactive gas. */
    public boolean canDumpGasTankContents() {
        if (level == null || level.isClientSide() || !MekChemicalHelper.isLoaded()) {
            return false;
        }
        Object handler = getChemicalHandler();
        if (handler == null || MekChemicalHelper.getTankAmount(handler) <= 0) {
            return false;
        }
        return !MekChemicalHelper.isRadioactiveInTank(handler);
    }

    public boolean dumpGasTankContents() {
        if (!canDumpGasTankContents()) {
            return false;
        }
        Object handler = getChemicalHandler();
        if (handler == null) {
            return false;
        }
        boolean ok = MekChemicalHelper.dumpTank(handler);
        if (ok) {
            setChanged();
        }
        return ok;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.colossal_reactors.resource_port.title");
    }

    public ContainerData getContainerData() {
        return fluidData;
    }

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
        mediumFlags.write(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input);
        fluidStorage.deserialize(input);
        portMode = PortMode.fromId(input.getIntOr(TAG_PORT_MODE, portMode.getId()));
        int savedFilterId = input.getIntOr(TAG_PORT_FILTER, -1);
        if (savedFilterId >= 0) {
            portFilter = PortFilter.fromId(savedFilterId);
        }
        mediumFlags.read(input);
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
            if (!allowInsert() || !acceptsItemForCapability(stack)) {
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
            return allowInsert() && acceptsItemForCapability(stack) && itemHandler.isItemValid(slot, stack);
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
            if (!allowFill() || !acceptsFluidForCapability(resource)) return 0;
            return super.insert(index, resource, amount, transaction);
        }

        @Override
        public int insert(FluidResource resource, int amount, TransactionContext transaction) {
            if (!allowFill() || !acceptsFluidForCapability(resource)) return 0;
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

    private boolean acceptsItemForCapability(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        RegistryAccess registries = level.registryAccess();
        boolean isFuel = FuelLoader.getDefinitionForItem(stack, registries) != null;
        return portFilter.acceptsFuelRole() && isFuel;
    }

    private boolean acceptsFluidForCapability(FluidResource resource) {
        if (resource == null || resource.isEmpty() || level == null) {
            return false;
        }
        FluidStack stack = resource.toStack(1);
        if (stack.isEmpty()) {
            return false;
        }
        RegistryAccess registries = level.registryAccess();
        Fluid fluid = stack.getFluid();
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        boolean isFuel = FuelLoader.getDefinitionForFluid(fluid, registries) != null;
        boolean isCoolant = CoolantLoader.getDefinitionForFluid(fluid, registries) != null;
        if (portFilter.acceptsFuelRole() && isFuel) {
            return true;
        }
        return portFilter.acceptsCoolantRole() && isCoolant;
    }

    private boolean acceptsChemicalForCapability(@Nullable Object chemicalStack) {
        if (!MekChemicalHelper.isLoaded() || MekChemicalHelper.isEmpty(chemicalStack) || level == null) {
            return false;
        }
        RegistryAccess registries = level.registryAccess();
        boolean isFuel = FuelLoader.getDefinitionForChemical(chemicalStack) != null;
        boolean isCoolant = CoolantLoader.getDefinitionForChemical(chemicalStack, registries) != null;
        if (portFilter.acceptsFuelRole() && isFuel) {
            return true;
        }
        return portFilter.acceptsCoolantRole() && isCoolant;
    }

    /** EXTRACT/EJECT: chemical waste produced by reactor fuel recipes. */
    private boolean acceptsChemicalWasteForCapability(@Nullable Object chemicalStack) {
        return portFilter.acceptsFuelRole() && FuelLoader.isChemicalWasteOutput(chemicalStack);
    }

    @Nullable
    private Object wrapFilteredChemicalHandler(@Nullable Object inner) {
        if (inner == null) return null;
        try {
            Class<?> handlerClass = Class.forName("mekanism.api.chemical.IChemicalHandler");
            InvocationHandler h = (proxy, method, args) -> {
                String name = method.getName();
                if ("insertChemical".equals(name)) {
                    if (!allowChemicalFill()) {
                        return MekChemicalHelper.rejectedInsertReturn(args);
                    }
                    Object stack = MekChemicalHelper.findChemicalStackInArgs(args);
                    if (stack != null && !MekChemicalHelper.isEmpty(stack)
                            && !acceptsChemicalForCapability(stack)) {
                        return MekChemicalHelper.rejectedInsertReturn(args);
                    }
                }
                if ("isChemicalValid".equals(name)) {
                    Object stack = MekChemicalHelper.findChemicalStackInArgs(args);
                    if (allowChemicalFill()) {
                        if (stack != null && !MekChemicalHelper.isEmpty(stack)
                                && !acceptsChemicalForCapability(stack)) {
                            return false;
                        }
                    } else if (allowChemicalDrain()) {
                        if (stack != null && !MekChemicalHelper.isEmpty(stack)
                                && !acceptsChemicalWasteForCapability(stack)) {
                            return false;
                        }
                    } else {
                        return false;
                    }
                }
                if ("extractChemical".equals(name) && !allowChemicalDrain()) {
                    Class<?> stackClass = Class.forName("mekanism.api.chemical.ChemicalStack");
                    return stackClass.getField("EMPTY").get(null);
                }
                return method.invoke(inner, args);
            };
            return Proxy.newProxyInstance(handlerClass.getClassLoader(), new Class<?>[]{handlerClass}, h);
        } catch (Throwable e) {
            return inner;
        }
    }

    private boolean allowChemicalFill() {
        return portMode == PortMode.INSERT && mediumFlags.isAllowGas() && !mediumFlags.isAllowLiquid();
    }

    private boolean allowChemicalDrain() {
        return (portMode == PortMode.EXTRACT || portMode == PortMode.EJECT)
                && mediumFlags.isAllowGas() && !mediumFlags.isAllowLiquid();
    }
}
