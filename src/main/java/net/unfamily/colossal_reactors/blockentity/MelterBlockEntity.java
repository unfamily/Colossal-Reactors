package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.block.MelterBlock;
import net.unfamily.colossal_reactors.melter.MelterHeatsLoader;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import net.unfamily.colossal_reactors.menu.MelterMenu;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Melter: no direct fuel; heated by adjacent blocks/fluids (melter_heats).
 * final_time = time / (UP * DOWN * EAST * WEST * NORTH * SOUTH); sides without heat = 1.
 */
public class MelterBlockEntity extends BlockEntity implements MenuProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MelterBlockEntity.class);
    private static final int TANK_CAPACITY_MB = 4000;
    /** Log at most every this many ticks when melter has input but does not advance (debug). */
    private static final int DEBUG_LOG_INTERVAL = 80;
    private static final String TAG_ITEMS = "Items";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_FLUID_AMOUNT = "FluidAmount";
    private static final String TAG_PROGRESS = "Progress";

    private final ItemStackHandler itemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY_MB) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int progress; // ticks toward current recipe completion
    private int redstoneMode = RedstoneMode.NONE.getId();
    private boolean lastRedstoneSignal;
    private boolean pulseAllowed; // for PULSE mode: one craft per rising edge

    private static final String TAG_REDSTONE_MODE = "RedstoneMode";
    private static final String TAG_LAST_REDSTONE = "LastRedstone";
    private static final String TAG_PULSE_ALLOWED = "PulseAllowed";

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fluidTank.getFluidAmount();
                case 1 -> fluidTank.getCapacity();
                case 2 -> fluidTank.getFluid().isEmpty()
                        ? -1
                        : BuiltInRegistries.FLUID.getId(fluidTank.getFluid().getFluid());
                case 3 -> progress;
                case 4 -> getCurrentMaxProgress();
                case 5 -> worldPosition.getX();
                case 6 -> worldPosition.getY();
                case 7 -> worldPosition.getZ();
                case 8 -> redstoneMode;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0 && value <= 0) fluidTank.setFluid(FluidStack.EMPTY);
            else if (index == 3) progress = Math.max(0, value);
        }

        @Override
        public int getCount() {
            return 9;
        }
    };

    public MelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MELTER_BE.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MelterBlockEntity be) {
        if (level.isClientSide()) return;
        be.serverTick(level, pos);
    }

    private void serverTick(Level level, BlockPos pos) {
        boolean hasSignal = level.hasNeighborSignal(pos);
        boolean shouldRun = isRedstoneRunAllowed(hasSignal);
        if (!shouldRun) {
            if (RedstoneMode.fromId(redstoneMode) == RedstoneMode.PULSE) pulseAllowed = false;
            progress = 0;
            setActiveState(level, pos, false);
            lastRedstoneSignal = hasSignal;
            return;
        }
        lastRedstoneSignal = hasSignal;

        ItemStack input = itemHandler.getStackInSlot(0);
        boolean hasInput = !input.isEmpty();
        long gt = level.getGameTime();

        MelterRecipe recipe = MelterRecipesLoader.getRecipeFor(input, level.registryAccess());
        if (recipe == null) {
            progress = 0;
            setActiveState(level, pos, false);
            if (Config.MELTER_DEBUG.get() && hasInput && gt % DEBUG_LOG_INTERVAL == 0) {
                LOGGER.info("[Melter {}] No recipe for item {} (loaded recipes: {})",
                        pos, BuiltInRegistries.ITEM.getKey(input.getItem()), MelterRecipesLoader.getAll().size());
            }
            return;
        }
        if (input.getCount() < recipe.count()) {
            progress = 0;
            setActiveState(level, pos, false);
            return;
        }
        Fluid fluid = MelterRecipesLoader.getOutputFluid(recipe, level.registryAccess());
        if (fluid == null || fluid.equals(Fluids.EMPTY)) {
            progress = 0;
            setActiveState(level, pos, false);
            if (Config.MELTER_DEBUG.get() && gt % DEBUG_LOG_INTERVAL == 0) {
                LOGGER.info("[Melter {}] Output fluid not found: {} (tag={})", pos, recipe.outputFluidId(), recipe.outputIsTag());
            }
            return;
        }
        FluidStack current = fluidTank.getFluid();
        if (!current.isEmpty() && !current.getFluid().equals(fluid)) {
            progress = 0;
            setActiveState(level, pos, false);
            return;
        }
        if (fluidTank.getFluidAmount() + recipe.amountMb() > fluidTank.getCapacity()) {
            progress = 0;
            setActiveState(level, pos, false);
            return;
        }

        boolean anyHeat =
                MelterHeatsLoader.hasHeatingSource(level, pos, Direction.UP)
                        || MelterHeatsLoader.hasHeatingSource(level, pos, Direction.DOWN)
                        || MelterHeatsLoader.hasHeatingSource(level, pos, Direction.EAST)
                        || MelterHeatsLoader.hasHeatingSource(level, pos, Direction.WEST)
                        || MelterHeatsLoader.hasHeatingSource(level, pos, Direction.NORTH)
                        || MelterHeatsLoader.hasHeatingSource(level, pos, Direction.SOUTH);
        if (!anyHeat) {
            progress = 0;
            setActiveState(level, pos, false);
            return;
        }

        double up = MelterHeatsLoader.getFactorFor(level, pos, Direction.UP);
        double down = MelterHeatsLoader.getFactorFor(level, pos, Direction.DOWN);
        double east = MelterHeatsLoader.getFactorFor(level, pos, Direction.EAST);
        double west = MelterHeatsLoader.getFactorFor(level, pos, Direction.WEST);
        double north = MelterHeatsLoader.getFactorFor(level, pos, Direction.NORTH);
        double south = MelterHeatsLoader.getFactorFor(level, pos, Direction.SOUTH);
        double product = up * down * east * west * north * south;
        if (product <= 0) {
            progress = 0;
            setActiveState(level, pos, false);
            if (Config.MELTER_DEBUG.get() && gt % DEBUG_LOG_INTERVAL == 0) {
                LOGGER.info("[Melter {}] Heat product <= 0 (up={} down={} e={} w={} n={} s={})",
                        pos, up, down, east, west, north, south);
            }
            return;
        }
        int finalTime = (int) Math.max(1, Math.floor((double) recipe.timeTicks() / product));

        setActiveState(level, pos, true);
        progress++;
        setChanged(); // so client receives progress updates (menu sync / BE update)
        if (progress >= finalTime) {
            progress = 0;
            if (RedstoneMode.fromId(redstoneMode) == RedstoneMode.PULSE) pulseAllowed = false;
            itemHandler.extractItem(0, recipe.count(), false);
            fluidTank.fill(new FluidStack(fluid, recipe.amountMb()), IFluidHandler.FluidAction.EXECUTE);
            setChanged();
        }
    }

    /** Whether the melter is allowed to run this tick based on redstone mode and current signal. */
    private boolean isRedstoneRunAllowed(boolean hasSignal) {
        switch (RedstoneMode.fromId(redstoneMode)) {
            case NONE: return true;
            case LOW: return !hasSignal;
            case HIGH: return hasSignal;
            case PULSE:
                if (!lastRedstoneSignal && hasSignal) pulseAllowed = true; // rising edge = low->high
                if (!pulseAllowed) return false;
                return true; // allow until one craft completes (then pulseAllowed cleared)
            case DISABLED: return false;
            default: return true;
        }
    }

    public int getRedstoneMode() { return redstoneMode; }
    public void setRedstoneMode(int mode) {
        this.redstoneMode = RedstoneMode.fromId(mode).getId();
        setChanged();
    }

    private void setActiveState(Level level, BlockPos pos, boolean active) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MelterBlock) || !state.hasProperty(MelterBlock.ACTIVE)) return;
        if (state.getValue(MelterBlock.ACTIVE) == active) return;
        level.setBlock(pos, state.setValue(MelterBlock.ACTIVE, active), 3);
    }

    /** Max progress (final time) for current recipe; 0 if none. */
    private int getCurrentMaxProgress() {
        Level level = getLevel();
        if (level == null) return 0;
        MelterRecipe recipe = MelterRecipesLoader.getRecipeFor(itemHandler.getStackInSlot(0), level.registryAccess());
        if (recipe == null) return 0;
        boolean anyHeat =
                MelterHeatsLoader.hasHeatingSource(level, worldPosition, Direction.UP)
                        || MelterHeatsLoader.hasHeatingSource(level, worldPosition, Direction.DOWN)
                        || MelterHeatsLoader.hasHeatingSource(level, worldPosition, Direction.EAST)
                        || MelterHeatsLoader.hasHeatingSource(level, worldPosition, Direction.WEST)
                        || MelterHeatsLoader.hasHeatingSource(level, worldPosition, Direction.NORTH)
                        || MelterHeatsLoader.hasHeatingSource(level, worldPosition, Direction.SOUTH);
        if (!anyHeat) return 0;
        double up = MelterHeatsLoader.getFactorFor(level, worldPosition, Direction.UP);
        double down = MelterHeatsLoader.getFactorFor(level, worldPosition, Direction.DOWN);
        double east = MelterHeatsLoader.getFactorFor(level, worldPosition, Direction.EAST);
        double west = MelterHeatsLoader.getFactorFor(level, worldPosition, Direction.WEST);
        double north = MelterHeatsLoader.getFactorFor(level, worldPosition, Direction.NORTH);
        double south = MelterHeatsLoader.getFactorFor(level, worldPosition, Direction.SOUTH);
        double product = up * down * east * west * north * south;
        if (product <= 0) return 0;
        return (int) Math.max(1, Math.floor((double) recipe.timeTicks() / product));
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.melter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MelterMenu(id, playerInventory, this, data);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /** Raw tank for internal use (recipe output). */
    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    /** Fluid handler for capability: drain only (bucket/pipes can extract, not insert). */
    public IFluidHandler getFluidHandlerForCapability() {
        return new DrainOnlyFluidHandler();
    }

    /**
     * Handles bucket/fluid container: drain only (empty bucket fills from tank; full bucket does not fill tank).
     * Returns true if a transfer occurred; caller must update player hand with itemHandler.getContainer().
     */
    public boolean interactWithItemFluidHandlerDrainOnly(IFluidHandlerItem itemHandler, Player player) {
        if (itemHandler.getTanks() == 0) return false;
        FluidStack inItem = itemHandler.getFluidInTank(0);
        if (!inItem.isEmpty()) {
            // Item has fluid: do not allow filling the melter
            return false;
        }
        // Item empty: drain from melter to item
        FluidStack inBlock = fluidTank.getFluid();
        if (inBlock.isEmpty() || !itemHandler.isFluidValid(0, inBlock)) return false;
        int capacity = itemHandler.getTankCapacity(0);
        FluidStack toFill = inBlock.copy();
        toFill.setAmount(Math.min(inBlock.getAmount(), capacity));
        int filled = itemHandler.fill(toFill, IFluidHandler.FluidAction.EXECUTE);
        if (filled > 0) {
            fluidTank.drain(filled, IFluidHandler.FluidAction.EXECUTE);
            var soundEvent = inBlock.getFluid().getFluidType().getSound(net.neoforged.neoforge.common.SoundActions.BUCKET_FILL);
            if (soundEvent != null) player.playSound(soundEvent);
            return true;
        }
        return false;
    }

    private final class DrainOnlyFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return fluidTank.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return fluidTank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {
            return fluidTank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return fluidTank.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return fluidTank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return fluidTank.drain(maxDrain, action);
        }
    }

    public ContainerData getData() {
        return data;
    }

    /** Drops all items from the input slot into the world. Call when the block is broken. */
    public void dropAllContents() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        BlockPos pos = getBlockPos();
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            ItemStack stack = itemHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.level.block.Block.popResource(level, pos, stack);
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put(TAG_ITEMS, itemHandler.serializeNBT(registries));
        tag.putInt(TAG_FLUID_AMOUNT, fluidTank.getFluidAmount());
        if (!fluidTank.getFluid().isEmpty()) {
            tag.putString(TAG_FLUID, BuiltInRegistries.FLUID.getKey(fluidTank.getFluid().getFluid()).toString());
        }
        tag.putInt(TAG_PROGRESS, progress);
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
            if (f != null && !f.equals(Fluids.EMPTY)) {
                fluidTank.setFluid(new FluidStack(f, tag.getInt(TAG_FLUID_AMOUNT)));
            }
        }
        progress = tag.getInt(TAG_PROGRESS);
        redstoneMode = RedstoneMode.fromId(tag.getInt(TAG_REDSTONE_MODE)).getId();
        lastRedstoneSignal = tag.getBoolean(TAG_LAST_REDSTONE);
        pulseAllowed = tag.getBoolean(TAG_PULSE_ALLOWED);
    }
}
