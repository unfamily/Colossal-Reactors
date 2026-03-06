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
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.colossal_reactors.melter.MelterHeatsLoader;
import net.unfamily.colossal_reactors.melter.MelterRecipe;
import net.unfamily.colossal_reactors.melter.MelterRecipesLoader;
import net.unfamily.colossal_reactors.menu.MelterMenu;
import org.jetbrains.annotations.Nullable;

/**
 * Melter: no direct fuel; heated by adjacent blocks/fluids (melter_heats).
 * final_time = time / (UP * DOWN * EAST * WEST * NORTH * SOUTH); sides without heat = 1.
 */
public class MelterBlockEntity extends BlockEntity implements MenuProvider {

    private static final int TANK_CAPACITY_MB = 10_000;
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
            return 8;
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
        ItemStack input = itemHandler.getStackInSlot(0);
        MelterRecipe recipe = MelterRecipesLoader.getRecipeFor(input, level.registryAccess());
        if (recipe == null) {
            progress = 0;
            return;
        }
        if (input.getCount() < recipe.count()) {
            progress = 0;
            return;
        }
        Fluid fluid = level.registryAccess().registry(net.minecraft.core.registries.Registries.FLUID)
                .map(r -> r.get(recipe.outputFluidId())).orElse(null);
        if (fluid == null || fluid.equals(Fluids.EMPTY)) {
            progress = 0;
            return;
        }
        FluidStack current = fluidTank.getFluid();
        if (!current.isEmpty() && !current.getFluid().equals(fluid)) {
            progress = 0;
            return;
        }
        if (fluidTank.getFluidAmount() + recipe.amountMb() > fluidTank.getCapacity()) {
            progress = 0;
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
            return;
        }
        int finalTime = (int) Math.max(1, Math.floor((double) recipe.timeTicks() / product));

        progress++;
        if (progress >= finalTime) {
            progress = 0;
            itemHandler.extractItem(0, recipe.count(), false);
            fluidTank.fill(new FluidStack(fluid, recipe.amountMb()), IFluidHandler.FluidAction.EXECUTE);
            setChanged();
        }
    }

    /** Max progress (final time) for current recipe; 0 if none. */
    private int getCurrentMaxProgress() {
        Level level = getLevel();
        if (level == null) return 0;
        MelterRecipe recipe = MelterRecipesLoader.getRecipeFor(itemHandler.getStackInSlot(0), level.registryAccess());
        if (recipe == null) return 0;
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

    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    public ContainerData getData() {
        return data;
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
    }
}
