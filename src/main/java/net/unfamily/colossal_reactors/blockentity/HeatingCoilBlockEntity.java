package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.colossal_reactors.block.HeatingCoilBlock;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.heatingcoil.ConsumeOption;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilDefinition;
import net.unfamily.colossal_reactors.heatingcoil.HeatingCoilRegistry;
import net.unfamily.colossal_reactors.menu.HeatingCoilMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Heating coil block entity. Only front face accepts inputs (enforced in capability registration).
 * Tracks activation progress; when any consume option is satisfied, block switches to ON.
 * Stays ON for duration ticks; every duration ticks consumes substain; if not met, switches back to OFF.
 */
public class HeatingCoilBlockEntity extends BlockEntity implements MenuProvider {

    /** Fallback when definition is missing (e.g. registry not loaded yet). */
    private static final int DEFAULT_TANK_MB = 10_000;
    private static final int DEFAULT_ENERGY = 100_000;
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_FLUID_AMOUNT = "Amount";
    private static final String TAG_ENERGY = "Energy";
    private static final String TAG_BURNABLE_TICKS = "BurnableTicks";
    private static final String TAG_ACTIVE_OPTION_INDEX = "ActiveOption";
    private static final String TAG_TICKS_UNTIL_SUBSTAIN = "TicksUntilSubstain";
    private static final String TAG_FREE_ON = "FreeOn";
    private static final String TAG_REDSTONE_MODE = "RedstoneMode";
    private static final String TAG_LAST_REDSTONE = "LastRedstone";
    private static final String TAG_PULSE_ALLOWED = "PulseAllowed";

    private int redstoneMode = RedstoneMode.NONE.getId();
    private boolean lastRedstoneSignal;
    private boolean pulseAllowed; // PULSE mode: allow one activation per rising edge

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank fluidTank;
    private final HeatingCoilEnergyStorage energyStorage;

    /** Accumulated burn time (ticks) from burnable items; no dispersion. */
    private int burnableTicksAccumulated;
    /** While ON: which consume option index we're using for substain. */
    private int activeOptionIndex = -1;
    /** While ON: countdown ticks until next substain consumption. */
    private int ticksUntilSubstain;
    /**
     * Set to true when coil is forced OFF by redstone. When redstone allows again, it will switch back ON
     * without consuming activation (only if it was previously ON and has a valid activeOptionIndex).
     */
    private boolean freeOn;

    private final ContainerData data;

    private boolean hasItemRequirement() {
        HeatingCoilDefinition def = getDefinition();
        if (def == null) return false;
        for (ConsumeOption opt : def.consume()) {
            if (opt.item() != null || opt.burnable() != null) return true;
        }
        return false;
    }

    private boolean hasFluidRequirement() {
        HeatingCoilDefinition def = getDefinition();
        if (def == null) return false;
        for (ConsumeOption opt : def.consume()) {
            if (opt.fluid() != null) return true;
        }
        return false;
    }

    private boolean hasEnergyRequirement() {
        HeatingCoilDefinition def = getDefinition();
        if (def == null) return false;
        for (ConsumeOption opt : def.consume()) {
            if (opt.energy() != null) return true;
        }
        return false;
    }

    public HeatingCoilBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HEATING_COIL_BE.get(), pos, state);
        HeatingCoilDefinition def = getDefinition();
        int tankCap = def == null ? DEFAULT_TANK_MB : maxFluidActivation(def);
        int energyCap = def == null ? DEFAULT_ENERGY : maxEnergyActivation(def);
        this.fluidTank = new FluidTank(tankCap) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.energyStorage = new HeatingCoilEnergyStorage(energyCap);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> fluidTank.getFluidAmount();
                    case 1 -> fluidTank.getCapacity();
                    case 2 -> fluidTank.getFluid().isEmpty() ? -1 : BuiltInRegistries.FLUID.getId(fluidTank.getFluid().getFluid());
                    case 3 -> energyStorage.getEnergyStored();
                    case 4 -> energyStorage.getMaxEnergyStored();
                    case 5 -> burnableTicksAccumulated;
                    case 6 -> activeOptionIndex;
                    case 7 -> ticksUntilSubstain;
                    case 8 -> worldPosition.getX();
                    case 9 -> worldPosition.getY();
                    case 10 -> worldPosition.getZ();
                    case 11 -> hasFluidRequirement() ? 1 : 0;
                    case 12 -> hasEnergyRequirement() ? 1 : 0;
                    case 13 -> hasItemRequirement() ? 1 : 0;
                    case 14 -> redstoneMode;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0 && value <= 0) fluidTank.setFluid(FluidStack.EMPTY);
                else if (index == 3) energyStorage.setEnergy(Math.min(value, energyStorage.getMaxEnergyStored()));
            }

            @Override
            public int getCount() {
                return 15;
            }
        };
        if (def != null && !def.consume().isEmpty()) {
            ticksUntilSubstain = def.duration();
        }
    }

    /** Max fluid activation across all consume options; 0 if none have fluid. */
    private static int maxFluidActivation(HeatingCoilDefinition def) {
        int max = 0;
        for (ConsumeOption opt : def.consume()) {
            if (opt.fluid() != null && opt.fluid().activation() > max) {
                max = opt.fluid().activation();
            }
        }
        return max;
    }

    /** Max energy activation across all consume options; 0 if none have energy. */
    private static int maxEnergyActivation(HeatingCoilDefinition def) {
        int max = 0;
        for (ConsumeOption opt : def.consume()) {
            if (opt.energy() != null && opt.energy().activation() > max) {
                max = opt.energy().activation();
            }
        }
        return max;
    }

    /** Max burnable activation across all consume options; 0 if none have burnable. */
    private static int maxBurnableActivation(HeatingCoilDefinition def) {
        int max = 0;
        for (ConsumeOption opt : def.consume()) {
            if (opt.burnable() != null && opt.burnable().activation() > max) {
                max = opt.burnable().activation();
            }
        }
        return max;
    }

    @Nullable
    public HeatingCoilDefinition getDefinition() {
        if (!(getBlockState().getBlock() instanceof HeatingCoilBlock hb)) return null;
        return HeatingCoilRegistry.get(hb.getCoilId());
    }

    public static void tick(Level level, BlockPos pos, BlockState state, HeatingCoilBlockEntity be) {
        if (level.isClientSide()) return;
        boolean hasSignal = level.hasNeighborSignal(pos);
        boolean shouldRun = be.isRedstoneRunAllowed(hasSignal);
        be.lastRedstoneSignal = hasSignal;

        // When redstone says "don't run": force OFF (freeOn so it can resume for free when signal allows again).
        if (!shouldRun) {
            if (RedstoneMode.fromId(be.redstoneMode) == RedstoneMode.PULSE) be.pulseAllowed = false;
            if (((HeatingCoilBlock) state.getBlock()).isOn()) {
                be.freeOn = true;
                be.switchToOff(level, pos);
            }
            return;
        }
        if (!((HeatingCoilBlock) state.getBlock()).isOn() && be.freeOn) {
            HeatingCoilDefinition def = HeatingCoilRegistry.get(((HeatingCoilBlock) state.getBlock()).getCoilId());
            if (def != null && be.activeOptionIndex >= 0 && be.activeOptionIndex < def.consume().size()) {
                be.freeOn = false;
                be.switchToOn(level, pos);
                return;
            }
            be.freeOn = false;
        }

        HeatingCoilBlock block = (HeatingCoilBlock) state.getBlock();
        HeatingCoilDefinition def = HeatingCoilRegistry.get(block.getCoilId());
        if (def == null) return;

        if (block.isOn()) {
            be.tickOn(level, pos, def);
        } else {
            be.tickOff(level, pos, def);
        }
    }

    private void tickOn(Level level, BlockPos pos, HeatingCoilDefinition def) {
        tryAcceptOneBurnableFromSlot(def);
        ticksUntilSubstain--;
        if (ticksUntilSubstain > 0) return;
        ticksUntilSubstain = def.duration();
        if (activeOptionIndex < 0 || activeOptionIndex >= def.consume().size()) {
            switchToOff(level, pos);
            return;
        }
        ConsumeOption option = def.consume().get(activeOptionIndex);
        if (!consumeSubstain(option)) {
            switchToOff(level, pos);
        }
    }

    /** Consumes one burnable item from slot and adds its burn time when below cap (used in both ON and OFF). */
    private void tryAcceptOneBurnableFromSlot(HeatingCoilDefinition def) {
        ItemStack stack = itemHandler.getStackInSlot(0);
        if (stack.isEmpty()) return;
        int burn = stack.getBurnTime(RecipeType.SMELTING);
        if (burn <= 0) return;
        int cap = maxBurnableActivation(def);
        if (cap > 0 && burnableTicksAccumulated >= cap) return;
        burnableTicksAccumulated += burn;
        stack.shrink(1);
        if (stack.isEmpty()) itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        setChanged();
    }

    private void tickOff(Level level, BlockPos pos, HeatingCoilDefinition def) {
        tryAcceptOneBurnableFromSlot(def);
        if (RedstoneMode.fromId(redstoneMode) == RedstoneMode.PULSE && !pulseAllowed) return;
        // Check if any option is satisfied; then consume activation and switch to ON
        for (int i = 0; i < def.consume().size(); i++) {
            ConsumeOption opt = def.consume().get(i);
            if (isOptionSatisfied(opt)) {
                activeOptionIndex = i;
                consumeActivation(opt);
                if (RedstoneMode.fromId(redstoneMode) == RedstoneMode.PULSE) pulseAllowed = false;
                switchToOn(level, pos);
                return;
            }
        }
    }

    /** Whether the coil is allowed to run (tick) based on redstone mode and current signal. */
    private boolean isRedstoneRunAllowed(boolean hasSignal) {
        switch (RedstoneMode.fromId(redstoneMode)) {
            case NONE: return true;
            case LOW: return !hasSignal;
            case HIGH: return hasSignal;
            case PULSE:
                if (!lastRedstoneSignal && hasSignal) pulseAllowed = true; // rising edge
                return pulseAllowed;
            case DISABLED: return false;
            default: return true;
        }
    }

    public int getRedstoneMode() { return redstoneMode; }
    /** Heating coil has no PULSE mode; PULSE is normalized to NONE. */
    public void setRedstoneMode(int mode) {
        RedstoneMode m = RedstoneMode.fromId(mode);
        this.redstoneMode = (m == RedstoneMode.PULSE ? RedstoneMode.NONE : m).getId();
        setChanged();
    }

    private void consumeActivation(ConsumeOption opt) {
        if (opt.fluid() != null) fluidTank.drain(opt.fluid().activation(), IFluidHandler.FluidAction.EXECUTE);
        if (opt.item() != null) {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (itemMatchesRequirement(stack, opt.item())) {
                stack.shrink(opt.item().activation());
                if (stack.isEmpty()) itemHandler.setStackInSlot(0, ItemStack.EMPTY);
            }
        }
        if (opt.energy() != null) energyStorage.extractEnergy(opt.energy().activation(), false);
        if (opt.burnable() != null) burnableTicksAccumulated -= opt.burnable().activation();
        setChanged();
    }

    /** Returns true if the fluid in the tank matches the option's fluid requirement (tag or specific fluid id). */
    private boolean fluidMatchesRequirement(FluidStack stack, ConsumeOption.FluidRequirement req) {
        if (stack.isEmpty()) return false;
        Level level = getLevel();
        if (level == null) return false;
        if (req.isTag()) {
            TagKey<Fluid> tagKey = TagKey.create(Registries.FLUID, req.tagOrId());
            return stack.getFluid().is(tagKey);
        }
        Fluid required = level.registryAccess().registry(Registries.FLUID).map(r -> r.get(req.tagOrId())).orElse(null);
        if (required == null || required.equals(Fluids.EMPTY)) return false;
        ResourceLocation requiredKey = BuiltInRegistries.FLUID.getKey(required);
        ResourceLocation stackKey = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return requiredKey.equals(stackKey);
    }

    /** Returns true if the stack matches the item requirement (tag or specific item id). */
    private boolean itemMatchesRequirement(ItemStack stack, ConsumeOption.ItemRequirement req) {
        if (stack.isEmpty()) return false;
        Level level = getLevel();
        if (level == null) return false;
        if (req.isTag()) {
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, req.tagOrId());
            return stack.is(tagKey);
        }
        Item item = level.registryAccess().registry(Registries.ITEM).map(r -> r.get(req.tagOrId())).orElse(null);
        return item != null && !item.equals(net.minecraft.world.item.Items.AIR) && stack.is(item);
    }

    private boolean isOptionSatisfied(ConsumeOption opt) {
        if (opt.fluid() != null) {
            FluidStack inTank = fluidTank.getFluid();
            if (!fluidMatchesRequirement(inTank, opt.fluid())) return false;
            if (inTank.getAmount() < opt.fluid().activation()) return false;
        }
        if (opt.item() != null) {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (!itemMatchesRequirement(stack, opt.item())) return false;
            if (stack.getCount() < opt.item().activation()) return false;
        }
        if (opt.energy() != null) {
            if (energyStorage.getEnergyStored() < opt.energy().activation()) return false;
        }
        if (opt.burnable() != null) {
            if (burnableTicksAccumulated < opt.burnable().activation()) return false;
        }
        return true;
    }

    private boolean consumeSubstain(ConsumeOption opt) {
        // Validate ALL requirements first (multi-parameter: e.g. fluid + burnable must both be satisfied)
        if (opt.fluid() != null) {
            FluidStack inTank = fluidTank.getFluid();
            if (!fluidMatchesRequirement(inTank, opt.fluid())) return false;
            int need = opt.fluid().substain();
            if (fluidTank.drain(need, IFluidHandler.FluidAction.SIMULATE).getAmount() < need) return false;
        }
        if (opt.item() != null) {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (!itemMatchesRequirement(stack, opt.item())) return false;
            if (stack.getCount() < opt.item().substain()) return false;
        }
        if (opt.energy() != null) {
            int need = opt.energy().substain();
            if (energyStorage.extractEnergy(need, true) < need) return false;
        }
        if (opt.burnable() != null) {
            if (burnableTicksAccumulated < opt.burnable().substain()) return false;
        }
        // All satisfied: consume all
        if (opt.fluid() != null) {
            fluidTank.drain(opt.fluid().substain(), IFluidHandler.FluidAction.EXECUTE);
        }
        if (opt.item() != null) {
            ItemStack stack = itemHandler.getStackInSlot(0);
            if (!itemMatchesRequirement(stack, opt.item())) return false;
            stack.shrink(opt.item().substain());
            if (stack.isEmpty()) itemHandler.setStackInSlot(0, ItemStack.EMPTY);
        }
        if (opt.energy() != null) {
            energyStorage.extractEnergy(opt.energy().substain(), false);
        }
        if (opt.burnable() != null) {
            burnableTicksAccumulated -= opt.burnable().substain();
        }
        setChanged();
        return true;
    }

    private void switchToOn(Level level, BlockPos pos) {
        ResourceLocation id = getCoilId();
        if (id == null) return;
        Block other = ModBlocks.getHeatingCoilBlock(id, true);
        if (other == null) return;
        CompoundTag saved = new CompoundTag();
        saveAdditional(saved, level.registryAccess());
        BlockState newState = other.defaultBlockState().setValue(HeatingCoilBlock.FACING, getBlockState().getValue(HeatingCoilBlock.FACING));
        level.setBlock(pos, newState, 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HeatingCoilBlockEntity coil) {
            coil.loadAdditional(saved, level.registryAccess());
        }
        closeMenuForPlayersAt(level, pos);
    }

    private void switchToOff(Level level, BlockPos pos) {
        ResourceLocation id = getCoilId();
        if (id == null) return;
        Block other = ModBlocks.getHeatingCoilBlock(id, false);
        if (other == null) return;
        CompoundTag saved = new CompoundTag();
        saveAdditional(saved, level.registryAccess());
        BlockState newState = other.defaultBlockState().setValue(HeatingCoilBlock.FACING, getBlockState().getValue(HeatingCoilBlock.FACING));
        level.setBlock(pos, newState, 3);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof HeatingCoilBlockEntity coil) {
            coil.loadAdditional(saved, level.registryAccess());
        }
        closeMenuForPlayersAt(level, pos);
    }

    /** Closes the heating coil GUI for any player who has it open at this block pos (after state change). */
    private static void closeMenuForPlayersAt(Level level, BlockPos pos) {
        if (level.isClientSide()) return;
        var server = level.getServer();
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.level() == level && player.containerMenu instanceof HeatingCoilMenu menu && menu.isForPosition(pos)) {
                player.closeContainer();
            }
        }
    }

    private ResourceLocation getCoilId() {
        return getBlockState().getBlock() instanceof HeatingCoilBlock hb ? hb.getCoilId() : null;
    }

    public IItemHandler getItemHandler() {
        return hasItemRequirement() ? itemHandler : NoItemHandler.INSTANCE;
    }

    /** Drops all items from the input slot into the world. Call when the block is broken. */
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

    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    public IEnergyStorage getEnergyStorage() {
        return energyStorage;
    }

    /** True if this coil accepts item capability (pipes can connect). */
    public boolean acceptsItemCapability() {
        HeatingCoilDefinition def = getDefinition();
        return def != null && !def.noItem();
    }

    /** True if this coil accepts fluid capability (pipes can connect). */
    public boolean acceptsFluidCapability() {
        HeatingCoilDefinition def = getDefinition();
        return def != null && !def.noFluid();
    }

    /** True if this coil accepts energy capability (cables can connect). */
    public boolean acceptsEnergyCapability() {
        HeatingCoilDefinition def = getDefinition();
        return def != null && !def.noEnergy();
    }

    /**
     * True if a capability query from the given side should be allowed.
     * When definition.allSides is true, accepts from any side; otherwise only from the front face.
     */
    public boolean allowsCapabilityOnSide(@Nullable net.minecraft.core.Direction side) {
        if (side == null) return true;
        HeatingCoilDefinition def = getDefinition();
        if (def != null && def.allSides()) return true;
        return side == getBlockState().getValue(HeatingCoilBlock.FACING);
    }

    /**
     * Handles bucket/fluid container: fill tank from item or drain tank to item (same as resource port).
     * Call only when acceptsFluidCapability() is true.
     */
    public boolean interactWithItemFluidHandler(IFluidHandlerItem itemHandler, Player player) {
        if (itemHandler.getTanks() == 0) return false;
        IFluidHandler blockHandler = getFluidHandler();
        FluidStack inItem = itemHandler.getFluidInTank(0);
        if (!inItem.isEmpty()) {
            if (blockHandler.fill(inItem.copy(), IFluidHandler.FluidAction.SIMULATE) > 0) {
                int filled = blockHandler.fill(inItem.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    itemHandler.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                    inItem.getFluid().getPickupSound().ifPresent(player::playSound);
                    return true;
                }
            }
        } else {
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

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new HeatingCoilMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_ITEMS, itemHandler.serializeNBT(registries));
        tag.putInt(TAG_FLUID_AMOUNT, fluidTank.getFluidAmount());
        if (!fluidTank.getFluid().isEmpty()) {
            tag.putString(TAG_FLUID, BuiltInRegistries.FLUID.getKey(fluidTank.getFluid().getFluid()).toString());
        }
        tag.putInt(TAG_ENERGY, energyStorage.getEnergyStored());
        tag.putInt(TAG_BURNABLE_TICKS, burnableTicksAccumulated);
        tag.putInt(TAG_ACTIVE_OPTION_INDEX, activeOptionIndex);
        tag.putInt(TAG_TICKS_UNTIL_SUBSTAIN, ticksUntilSubstain);
        tag.putBoolean(TAG_FREE_ON, freeOn);
        tag.putInt(TAG_REDSTONE_MODE, redstoneMode);
        tag.putBoolean(TAG_LAST_REDSTONE, lastRedstoneSignal);
        tag.putBoolean(TAG_PULSE_ALLOWED, pulseAllowed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains(TAG_ITEMS)) itemHandler.deserializeNBT(registries, tag.getCompound(TAG_ITEMS));
        fluidTank.setFluid(FluidStack.EMPTY);
        if (tag.contains(TAG_FLUID)) {
            Fluid f = BuiltInRegistries.FLUID.get(ResourceLocation.parse(tag.getString(TAG_FLUID)));
            if (f != null && f != Fluids.EMPTY) {
                fluidTank.setFluid(new FluidStack(f, tag.getInt(TAG_FLUID_AMOUNT)));
            }
        } else if (tag.contains(TAG_FLUID_AMOUNT)) {
            fluidTank.setFluid(new FluidStack(Fluids.WATER, Math.min(tag.getInt(TAG_FLUID_AMOUNT), fluidTank.getCapacity())));
        }
        energyStorage.setEnergy(tag.getInt(TAG_ENERGY));
        burnableTicksAccumulated = tag.getInt(TAG_BURNABLE_TICKS);
        activeOptionIndex = tag.getInt(TAG_ACTIVE_OPTION_INDEX);
        ticksUntilSubstain = tag.getInt(TAG_TICKS_UNTIL_SUBSTAIN);
        freeOn = tag.getBoolean(TAG_FREE_ON);
        RedstoneMode m = RedstoneMode.fromId(tag.getInt(TAG_REDSTONE_MODE));
        redstoneMode = (m == RedstoneMode.PULSE ? RedstoneMode.NONE : m).getId();
        lastRedstoneSignal = tag.getBoolean(TAG_LAST_REDSTONE);
        pulseAllowed = tag.getBoolean(TAG_PULSE_ALLOWED);
    }

    public ContainerData getData() {
        return data;
    }

    private static final class HeatingCoilEnergyStorage extends EnergyStorage {
        HeatingCoilEnergyStorage(int capacity) {
            super(capacity, capacity, capacity);
        }
        void setEnergy(int energy) {
            this.energy = Math.max(0, Math.min(energy, capacity));
        }
    }

    /** Empty 1-slot handler used when coil has no item/burnable requirement; rejects insert and has no items. */
    private static final class NoItemHandler implements IItemHandler {
        static final NoItemHandler INSTANCE = new NoItemHandler();

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
