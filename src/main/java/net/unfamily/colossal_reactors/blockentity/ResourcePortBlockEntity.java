package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
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
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.colossal_reactors.multiblock.PortCapacityPolicy;
import net.unfamily.colossal_reactors.multiblock.PortScalingConstants;
import net.unfamily.colossal_reactors.transfer.LongBackedFluidTank;
import net.unfamily.colossal_reactors.transfer.LegacyIFluidHandlerResourceHandler;
import net.unfamily.colossal_reactors.coolant.CoolantLoader;
import net.unfamily.colossal_reactors.fuel.FuelLoader;
import net.unfamily.colossal_reactors.integration.mekanism.MekChemicalHelper;
import net.unfamily.colossal_reactors.integration.mekanism.MaterialSelector;
import net.unfamily.colossal_reactors.menu.ResourcePortMenu;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationDefinition;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;
import net.unfamily.colossal_reactors.util.FluidInputMatcher;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;

/**
 * BlockEntity for Resource Port. One item slot (insert/remove) and a fluid tank that accepts
 * and provides fluids via capability. Port mode: insert / extract / eject.
 */
public class ResourcePortBlockEntity extends BlockEntity implements MenuProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(ResourcePortBlockEntity.class);
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_FLUID_ID = "FluidId";
    private static final String TAG_FLUID_AMOUNT = "Amount";
    private static final String TAG_PORT_MODE = "PortMode";
    private static final String TAG_PORT_FILTER = "PortFilter";
    private static final int SLOT_SIZE = 1;

    protected long tankCapacityMb() {
        return PortScalingConstants.MIN_FLUID_TANK_MB;
    }

    private static int lowLong(long value) {
        return (int) value;
    }

    private static int highLong(long value) {
        return (int) (value >>> 32);
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
    private static final int DATA_FLUID_AMOUNT_HI = DATA_IS_TURBINE + 1;
    private static final int DATA_FLUID_CAPACITY_HI = DATA_FLUID_AMOUNT_HI + 1;
    private static final int DATA_GAS_AMOUNT_HI = DATA_FLUID_CAPACITY_HI + 1;
    private static final int DATA_GAS_CAPACITY_HI = DATA_GAS_AMOUNT_HI + 1;
    private static final int DATA_COUNT = DATA_GAS_CAPACITY_HI + 1;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_SIZE) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final LongBackedFluidTank fluidTank = new LongBackedFluidTank(tankCapacityMb(), this::setChanged);

    private PortMode portMode = PortMode.INSERT;
    private PortFilter portFilter = PortFilter.BOTH;
    private final PortMediumFlags mediumFlags = new PortMediumFlags();

    /** Mek chemical tank handler (lazy). */
    private Object chemicalHandler;

    @Nullable
    private ResourceHandler<ItemResource> cachedItemCapability;

    @Nullable
    private ResourceHandler<FluidResource> cachedFluidCapability;

    private final ContainerData fluidData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> lowLong(fluidTank.getFluidAmountLong());
                case 1 -> lowLong(fluidTank.getCapacityLong());
                case 2 -> fluidTank.getFluid().isEmpty()
                        ? -1
                        : BuiltInRegistries.FLUID.getId(fluidTank.getFluid().getFluid());
                case DATA_MODE -> portMode.getId();
                case DATA_POS_X -> worldPosition.getX();
                case DATA_POS_Y -> worldPosition.getY();
                case DATA_POS_Z -> worldPosition.getZ();
                case DATA_ALLOW_SOLID -> mediumFlags.isAllowSolid() ? 1 : 0;
                case DATA_ALLOW_LIQUID -> mediumFlags.isAllowLiquid() ? 1 : 0;
                case DATA_ALLOW_GAS -> mediumFlags.isAllowGas() ? 1 : 0;
                case DATA_GAS_AMOUNT -> lowLong(getGasAmountMbLong());
                case DATA_GAS_CAPACITY -> lowLong(getGasCapacityMbLong());
                case DATA_PORT_FILTER -> portFilter.getId();
                case DATA_IS_TURBINE -> isTurbineResourcePort() ? 1 : 0;
                case DATA_FLUID_AMOUNT_HI -> highLong(fluidTank.getFluidAmountLong());
                case DATA_FLUID_CAPACITY_HI -> highLong(fluidTank.getCapacityLong());
                case DATA_GAS_AMOUNT_HI -> highLong(getGasAmountMbLong());
                case DATA_GAS_CAPACITY_HI -> highLong(getGasCapacityMbLong());
                default -> {
                    if (index == DATA_GAS_TYPE_LENGTH) {
                        String name = getGasTypeRegistryName();
                        yield name != null ? name.length() : 0;
                    }
                    if (index >= DATA_GAS_TYPE_START && index < DATA_COUNT) {
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
                FluidStack current = fluidTank.getFluid();
                if (value <= 0) {
                    fluidTank.setFluid(FluidStack.EMPTY);
                } else if (!current.isEmpty()) {
                    long cap = fluidTank.getCapacityLong();
                    fluidTank.setFluid(new FluidStack(current.getFluid(), (int) Math.min(value, cap)));
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
    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /** Raw handler for menu and bucket interaction (no mode/filter restriction). */
    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    /** Fluid in tank (raw, no mode/filter). */
    public FluidStack getStoredFluid() {
        return fluidTank.getFluid().copy();
    }

    public int getFluidAmountMb() {
        return (int) Math.min(fluidTank.getFluidAmountLong(), Integer.MAX_VALUE);
    }

    public long getFluidAmountMbLong() {
        return fluidTank.getFluidAmountLong();
    }

    public int getFluidCapacityMb() {
        return (int) Math.min(fluidTank.getCapacityLong(), Integer.MAX_VALUE);
    }

    public long getFluidCapacityMbLong() {
        return fluidTank.getCapacityLong();
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
        if (!acceptsReactorFluidPush(stack)) return 0;
        long filled = fluidTank.fillLong(stack, false);
        return filled > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) filled;
    }

    /** Push Mek gas into port (EXTRACT/EJECT, gas toggle on). Returns mB accepted. */
    public int receiveGasFromReactor(Object chemicalStack) {
        if (!MekChemicalHelper.isLoaded() || MekChemicalHelper.isEmpty(chemicalStack)) return 0;
        if (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT) return 0;
        if (!acceptsReactorGasPush(chemicalStack)) {
            LOGGER.debug("[CR-port] receiveGasFromReactor: rejected push at {} (mode={} gas={} liq={} amountMb={} capMb={})",
                    getBlockPos(), portMode, mediumFlags.isAllowGas(), mediumFlags.isAllowLiquid(),
                    getGasAmountMb(), getGasCapacityMb());
            return 0;
        }
        if (!canAcceptChemicalFromReactor(chemicalStack)) {
            LOGGER.debug("[CR-port] receiveGasFromReactor: canAcceptChemicalFromReactor=false at {} (mode={} gas={} liq={} amountMb={} capMb={})",
                    getBlockPos(), portMode, mediumFlags.isAllowGas(), mediumFlags.isAllowLiquid(),
                    getGasAmountMb(), getGasCapacityMb());
            return 0;
        }
        Object handler = getChemicalHandler();
        if (handler == null) return 0;
        int filled = MekChemicalHelper.fill(handler, chemicalStack, false);
        if (filled <= 0) {
            LOGGER.warn("[CR-port] fill returned 0 for chemical '{}' at {} (handler={}, capMb={}, amountMb={})",
                    MekChemicalHelper.getTypeRegistryName(chemicalStack), getBlockPos(),
                    handler.getClass().getSimpleName(), getGasCapacityMb(), getGasAmountMb());
        }
        return filled;
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
        return fluidTank.getFluidAmountLong() < fluidTank.getCapacityLong();
    }

    public boolean canAcceptGasFromReactor() {
        if (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT) return false;
        if (!mediumFlags.isAllowGas() || mediumFlags.isAllowLiquid()) return false;
        return getGasAmountMbLong() < getGasCapacityMbLong();
    }

    /** True when the gas tank is empty or already holds the same Mek chemical (reactor push). */
    public boolean canAcceptChemicalFromReactor(@Nullable Object chemicalStack) {
        if (!canAcceptGasFromReactor() || !MekChemicalHelper.isLoaded()
                || MekChemicalHelper.isEmpty(chemicalStack)) {
            return false;
        }
        Object handler = getChemicalHandler();
        if (handler == null) {
            return false;
        }
        Object inTank = MekChemicalHelper.getChemicalInTank(handler, 0);
        return MekChemicalHelper.isEmpty(inTank)
                || MekChemicalHelper.chemicalsMatch(inTank, chemicalStack);
    }

    public long getGasSpaceMb() {
        return Math.max(0L, getGasCapacityMbLong() - getGasAmountMbLong());
    }

    /**
     * Drains fluid from this port's tank for reactor coolant consumption. Only when mode is INSERT.
     * Returns amount actually drained (caller uses this for steam production).
     */
    public int takeFluidForReactor(Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        if (portMode != PortMode.INSERT) return 0;
        FluidStack inTank = fluidTank.getFluid();
        if (inTank.isEmpty() || inTank.getFluid() != fluid) return 0;
        int drain = (int) Math.min(amountMb, inTank.getAmount());
        if (drain <= 0) return 0;
        fluidTank.drain(drain, IFluidHandler.FluidAction.EXECUTE);
        setChanged();
        return drain;
    }

    /** Drain gas from port tank for reactor/turbine (INSERT mode). */
    public int takeGasForReactor(Object templateStack, int amountMb) {
        if (!MekChemicalHelper.isLoaded() || amountMb <= 0 || MekChemicalHelper.isEmpty(templateStack)) return 0;
        // Internal reactor/turbine pull; medium toggles gate pipe insert, not multiblock consumption.
        if (portMode != PortMode.INSERT) return 0;
        Object handler = getChemicalHandler();
        if (handler == null) return 0;
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
    public boolean interactWithItemFluidHandler(IFluidHandlerItem itemHandler, Player player) {
        if (itemHandler.getTanks() == 0) return false;
        IFluidHandler blockHandler = getFluidHandler();
        FluidStack inItem = itemHandler.getFluidInTank(0);
        if (!inItem.isEmpty()) {
            if (portMode != PortMode.INSERT || !mediumFlags.isAllowLiquid() || mediumFlags.isAllowGas()
                    || !acceptsFluidForCapability(inItem)) {
                return false;
            }
            if (blockHandler.fill(inItem.copy(), IFluidHandler.FluidAction.SIMULATE) > 0) {
                int filled = blockHandler.fill(inItem.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    inItem.getFluid().getPickupSound().ifPresent(player::playSound);
                    return true;
                }
            }
        } else {
            if (portMode != PortMode.EXTRACT && portMode != PortMode.EJECT) {
                return false;
            }
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

    public ResourceHandler<ItemResource> getItemResourceHandlerForCapability() {
        if (cachedItemCapability == null) {
            cachedItemCapability = LegacyItemHandlerResourceHandler.wrap(getItemHandlerForCapability());
        }
        return cachedItemCapability;
    }

    public ResourceHandler<FluidResource> getFluidResourceHandlerForCapability() {
        if (cachedFluidCapability == null) {
            cachedFluidCapability = LegacyIFluidHandlerResourceHandler.wrap(getFluidHandlerForCapability());
        }
        return cachedFluidCapability;
    }

    /** Mek chemical handler for capability; null if Mek not loaded. */
    @Nullable
    public Object getChemicalHandlerForCapability() {
        if (!MekChemicalHelper.isLoaded()) return null;
        return wrapFilteredChemicalHandler(getChemicalHandler());
    }

    @Nullable
    public Object getChemicalHandler() {
        if (!MekChemicalHelper.isLoaded()) return null;
        if (chemicalHandler == null) {
            long cap = tankCapacityMb();
            Object tank = MekChemicalHelper.createBasicTank(cap);
            if (tank == null) {
                LOGGER.warn("[CR-port] createBasicTank({}) returned null at {} — Mek chemical tank unavailable", cap, getBlockPos());
            }
            chemicalHandler = tank != null ? MekChemicalHelper.wrapAsHandler(tank) : null;
        }
        return chemicalHandler;
    }

    public int getGasAmountMb() {
        return (int) Math.min(getGasAmountMbLong(), Integer.MAX_VALUE);
    }

    public long getGasAmountMbLong() {
        Object h = getChemicalHandler();
        return h != null ? MekChemicalHelper.getTankAmountLong(h) : 0L;
    }

    public int getGasCapacityMb() {
        return (int) Math.min(getGasCapacityMbLong(), Integer.MAX_VALUE);
    }

    public long getGasCapacityMbLong() {
        Object h = getChemicalHandler();
        return h != null ? MekChemicalHelper.getTankCapacityLong(h) : 0L;
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

    public LongBackedFluidTank getFluidTank() {
        return fluidTank;
    }

    /** Server: resize fluid and Mek gas tanks with deferred shrink when buffer exceeds target. */
    public void applyTankCapacity(long targetMb) {
        if (targetMb <= 0 || level == null || level.isClientSide()) {
            return;
        }
        long resolvedFluid = PortCapacityPolicy.resolveFluidCapacity(targetMb,
                fluidTank.getCapacityLong(), fluidTank.getFluidAmountLong());
        if (fluidTank.getCapacityLong() != resolvedFluid) {
            fluidTank.resize(resolvedFluid);
            cachedFluidCapability = null;
            setChanged();
        }
        if (MekChemicalHelper.isLoaded()) {
            long resolvedGas = PortCapacityPolicy.resolveFluidCapacity(targetMb,
                    getGasCapacityMbLong(), getGasAmountMbLong());
            if (getGasCapacityMbLong() != resolvedGas) {
                resizeGasTankMb(resolvedGas);
                setChanged();
            }
        }
    }

    /** @see #applyTankCapacity(long) */
    public void applyTankCapacityMb(int capacityMb) {
        applyTankCapacity(capacityMb);
    }

    private void resizeGasTankMb(long capacityMb) {
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

    /** Server: discard all fluid in the internal tank (GUI dump). @return true if any fluid was removed */
    public boolean dumpFluidTankContents() {
        if (level == null || level.isClientSide()) return false;
        if (fluidTank.getFluid().isEmpty()) return false;
        fluidTank.setFluid(FluidStack.EMPTY);
        setChanged();
        return true;
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

    /** Mek-style: dump radioactive gas into the environment before the block is removed. */
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

    public PortMedium getPortMedium() {
        return mediumFlags.getMedium();
    }

    public void setPortMedium(PortMedium medium) {
        if (medium == null) {
            return;
        }
        mediumFlags.setMedium(clampMedium(medium));
        setChanged();
    }

    public void cyclePortMedium() {
        boolean gasAvailable = MekChemicalHelper.isLoaded();
        if (isTurbineResourcePort()) {
            mediumFlags.cycleTurbine(gasAvailable);
        } else {
            mediumFlags.cycleReactor(gasAvailable);
        }
        setChanged();
    }

    public void cyclePortMediumBack() {
        boolean gasAvailable = MekChemicalHelper.isLoaded();
        if (isTurbineResourcePort()) {
            mediumFlags.cycleTurbineBack(gasAvailable);
        } else {
            mediumFlags.cycleReactorBack(gasAvailable);
        }
        setChanged();
    }

    private PortMedium clampMedium(PortMedium medium) {
        if (medium == PortMedium.GAS && !MekChemicalHelper.isLoaded()) {
            return PortMedium.LIQUID;
        }
        if (isTurbineResourcePort() && medium == PortMedium.SOLID) {
            return PortMedium.LIQUID;
        }
        return medium;
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

    @Override
    public Component getDisplayName() {
        return Component.translatable("gui.colossal_reactors.resource_port.title");
    }

    public ContainerData getContainerData() {
        return fluidData;
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
        fluidTank.serialize(output);
        output.putInt(TAG_PORT_MODE, portMode.getId());
        output.putInt(TAG_PORT_FILTER, portFilter.getId());
        mediumFlags.write(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        itemHandler.deserialize(input);
        fluidTank.deserialize(input);
        portMode = PortMode.fromId(input.getIntOr(TAG_PORT_MODE, portMode.getId()));
        int savedFilterId = input.getIntOr(TAG_PORT_FILTER, -1);
        if (savedFilterId >= 0) {
            portFilter = PortFilter.fromId(savedFilterId);
        }
        mediumFlags.read(input);
        setPortMedium(getPortMedium());
    }

    /**
     * Item handler exposed to capability (hoppers/pipes). INSERT: allow insert. EXTRACT/EJECT: allow extract only.
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
            itemHandler.setStackInSlot(slot, stack);
        }
    }

    /**
     * Fluid handler exposed to capability (hoppers/pipes). INSERT: allow fill. EXTRACT/EJECT: allow drain only.
     */
    private final class FilteredFluidHandler implements IFluidHandler {
        private boolean allowLiquidFill() {
            return portMode == PortMode.INSERT && mediumFlags.isAllowLiquid() && !mediumFlags.isAllowGas();
        }

        private boolean allowFill() {
            return allowLiquidFill();
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
            if (!allowFill() || !acceptsFluidForCapability(resource)) {
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
            return allowFill() && acceptsFluidForCapability(stack) && fluidTank.isFluidValid(tank, stack);
        }
    }

    private boolean acceptsReactorFluidPush(FluidStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        RegistryAccess registries = level.registryAccess();
        Fluid fluid = stack.getFluid();
        if (FuelLoader.matchesAnyFluidFuelOutput(fluid, registries)) {
            return portFilter.acceptsFuelRole();
        }
        if (CoolantLoader.matchesAnyCoolantLiquidOutput(fluid, registries)) {
            return portFilter.acceptsCoolantRole();
        }
        return false;
    }

    private boolean acceptsReactorGasPush(@Nullable Object chemicalStack) {
        if (!MekChemicalHelper.isLoaded() || MekChemicalHelper.isEmpty(chemicalStack) || level == null) {
            return false;
        }
        RegistryAccess registries = level.registryAccess();
        if (FuelLoader.matchesAnyChemicalFuelOutput(chemicalStack)) {
            return portFilter.acceptsFuelRole();
        }
        if (CoolantLoader.matchesAnyCoolantGasOutput(chemicalStack, registries)) {
            return portFilter.acceptsCoolantRole();
        }
        return false;
    }

    private boolean acceptsItemForCapability(ItemStack stack) {
        if (isTurbineResourcePort() || stack.isEmpty() || level == null) {
            return false;
        }
        RegistryAccess registries = level.registryAccess();
        boolean isFuel = FuelLoader.getDefinitionForItem(stack, registries) != null;
        if (portFilter.acceptsFuelRole() && isFuel) {
            return true;
        }
        return false;
    }

    private boolean acceptsFluidForCapability(FluidStack stack) {
        if (stack.isEmpty() || level == null) {
            return false;
        }
        RegistryAccess registries = level.registryAccess();
        Fluid fluid = stack.getFluid();
        if (fluid == null || fluid == Fluids.EMPTY) {
            return false;
        }
        if (isTurbineResourcePort()) {
            return acceptsTurbineFluidInput(fluid, registries);
        }
        boolean isFuel = FuelLoader.getDefinitionForFluid(fluid, registries) != null;
        boolean isCoolant = CoolantLoader.getDefinitionForFluid(fluid, registries) != null;
        if (portFilter.acceptsFuelRole() && isFuel) {
            return true;
        }
        return portFilter.acceptsCoolantRole() && isCoolant;
    }

    private boolean acceptsTurbineFluidInput(Fluid fluid, RegistryAccess registries) {
        TurbineGenerationDefinition gen = TurbineGenerationLoader.getDefault();
        if (gen == null) {
            return false;
        }
        return FluidInputMatcher.matchesAnyFluidInput(fluid, gen.inputs());
    }

    private boolean acceptsTurbineChemicalInput(@Nullable Object chemicalStack) {
        if (!MekChemicalHelper.isLoaded() || MekChemicalHelper.isEmpty(chemicalStack)) {
            return false;
        }
        TurbineGenerationDefinition gen = TurbineGenerationLoader.getDefault();
        if (gen == null) {
            return false;
        }
        for (String input : gen.inputs()) {
            if (input != null && MaterialSelector.isChemicalPrefix(input)
                    && MaterialSelector.matchesChemical(chemicalStack, input)) {
                return true;
            }
        }
        return false;
    }

    private boolean acceptsChemicalForCapability(@Nullable Object chemicalStack) {
        if (!MekChemicalHelper.isLoaded() || MekChemicalHelper.isEmpty(chemicalStack) || level == null) {
            return false;
        }
        if (isTurbineResourcePort()) {
            return acceptsTurbineChemicalInput(chemicalStack);
        }
        RegistryAccess registries = level.registryAccess();
        boolean isFuel = FuelLoader.getDefinitionForChemical(chemicalStack) != null;
        boolean isCoolant = CoolantLoader.getDefinitionForChemical(chemicalStack, registries) != null;
        if (portFilter.acceptsFuelRole() && isFuel) {
            return true;
        }
        return portFilter.acceptsCoolantRole() && isCoolant;
    }

    /** EXTRACT: chemical waste from fuel recipe {@code output} selectors (datapack JSON). */
    private boolean acceptsChemicalOutputForCapability(@Nullable Object chemicalStack) {
        if (!FuelLoader.matchesAnyChemicalFuelOutput(chemicalStack)) {
            return false;
        }
        return portFilter.acceptsFuelRole();
    }

    /**
     * EJECT drains input fuel/coolant; EXTRACT drains fuel JSON {@code output} (waste). INSERT is fill-only.
     */
    private boolean acceptsChemicalDrainForCapability(@Nullable Object chemicalStack) {
        if (MekChemicalHelper.isEmpty(chemicalStack)) {
            return true;
        }
        return switch (portMode) {
            case EJECT -> acceptsChemicalForCapability(chemicalStack);
            case EXTRACT -> acceptsChemicalOutputForCapability(chemicalStack);
            default -> false;
        };
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
                    if (allowChemicalFill()) {
                        Object stack = MekChemicalHelper.findChemicalStackInArgs(args);
                        if (stack != null && !MekChemicalHelper.isEmpty(stack)
                                && !acceptsChemicalForCapability(stack)) {
                            return false;
                        }
                    } else if (allowChemicalDrain()) {
                        Object stack = MekChemicalHelper.findChemicalStackInArgs(args);
                        if (stack != null && !MekChemicalHelper.isEmpty(stack)
                                && !acceptsChemicalDrainForCapability(stack)) {
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
        return portMode == PortMode.INSERT && mediumFlags.isAllowGas();
    }

    private boolean allowChemicalDrain() {
        return (portMode == PortMode.EXTRACT || portMode == PortMode.EJECT)
                && mediumFlags.isAllowGas() && !mediumFlags.isAllowLiquid();
    }
}
