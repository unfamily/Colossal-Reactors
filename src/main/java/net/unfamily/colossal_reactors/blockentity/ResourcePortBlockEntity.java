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
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.unfamily.colossal_reactors.menu.ResourcePortMenu;
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
    public static final int TANK_CAPACITY_MB = 16000;

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

    private final FluidTank fluidTank = new FluidTank(TANK_CAPACITY_MB) {
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
                    fluidTank.setFluid(new FluidStack(current.getFluid(), Math.min(value, TANK_CAPACITY_MB)));
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

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    public IFluidHandler getFluidHandler() {
        return fluidTank;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
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
}
