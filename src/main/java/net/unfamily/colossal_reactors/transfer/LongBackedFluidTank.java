package net.unfamily.colossal_reactors.transfer;

import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.util.ValueIOSerializable;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;

/**
 * Single-tank fluid storage with {@code long} capacity and amount. {@link IFluidHandler} API remains {@code int}
 * per operation; values above {@link Integer#MAX_VALUE} are clamped per fill/drain call.
 */
public class LongBackedFluidTank implements IFluidHandler, ValueIOSerializable {

    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_AMOUNT = "Amount";
    private static final String TAG_AMOUNT_LONG = "AmountL";
    private static final String TAG_CAPACITY_LONG = "CapacityL";

    private FluidStack fluid = FluidStack.EMPTY;
    private long capacityMb;
    private long amountMb;
    private final Runnable onChange;

    public LongBackedFluidTank(long capacityMb) {
        this(capacityMb, () -> {});
    }

    public LongBackedFluidTank(long capacityMb, Runnable onChange) {
        if (capacityMb < 0) {
            throw new IllegalArgumentException("capacity must be non-negative");
        }
        this.capacityMb = capacityMb;
        this.onChange = onChange != null ? onChange : () -> {};
    }

    public long getCapacityLong() {
        return capacityMb;
    }

    public long getFluidAmountLong() {
        return amountMb;
    }

    public FluidStack getFluid() {
        if (fluid.isEmpty() || amountMb <= 0) {
            return FluidStack.EMPTY;
        }
        int stackAmount = amountMb > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) amountMb;
        return new FluidStack(fluid.getFluid(), stackAmount);
    }

    public void setFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            fluid = FluidStack.EMPTY;
            amountMb = 0L;
        } else {
            fluid = new FluidStack(stack.getFluid(), 1);
            amountMb = Math.min(capacityMb, Math.max(0L, stack.getAmount()));
        }
        onChange.run();
    }

    /**
     * Resizes capacity; clamps stored amount. Caller should pass a capacity already resolved by
     * {@link net.unfamily.colossal_reactors.multiblock.PortCapacityPolicy}.
     */
    public void resize(long newCapacityMb) {
        if (newCapacityMb < 0) {
            throw new IllegalArgumentException("capacity must be non-negative");
        }
        capacityMb = newCapacityMb;
        if (amountMb > capacityMb) {
            amountMb = capacityMb;
        }
        onChange.run();
    }

    @Override
    public void serialize(ValueOutput output) {
        output.putLong(TAG_CAPACITY_LONG, capacityMb);
        if (!fluid.isEmpty() && amountMb > 0) {
            output.putLong(TAG_AMOUNT_LONG, amountMb);
            FluidStack save = new FluidStack(fluid.getFluid(), (int) Math.min(amountMb, Integer.MAX_VALUE));
            output.store(TAG_FLUID, FluidStack.CODEC, save);
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        capacityMb = Math.max(0L, input.getLongOr(TAG_CAPACITY_LONG, capacityMb));
        fluid = FluidStack.EMPTY;
        amountMb = 0L;
        FluidStack loaded = input.read(TAG_FLUID, FluidStack.CODEC).orElse(FluidStack.EMPTY);
        if (!loaded.isEmpty() && loaded.getFluid() != Fluids.EMPTY) {
            fluid = new FluidStack(loaded.getFluid(), 1);
            long fallback = Math.max(0L, loaded.getAmount());
            amountMb = Math.min(capacityMb, Math.max(0L, input.getLongOr(TAG_AMOUNT_LONG, fallback)));
        }
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    @NotNull
    public FluidStack getFluidInTank(int tank) {
        return getFluid();
    }

    @Override
    public int getTankCapacity(int tank) {
        return (int) Math.min(capacityMb, Integer.MAX_VALUE);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || amountMb >= capacityMb) {
            return 0;
        }
        if (!fluid.isEmpty() && fluid.getFluid() != resource.getFluid()) {
            return 0;
        }
        long space = capacityMb - amountMb;
        long toFill = Math.min(space, resource.getAmount());
        if (toFill <= 0) {
            return 0;
        }
        int filled = toFill > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) toFill;
        if (!action.simulate()) {
            if (fluid.isEmpty()) {
                fluid = new FluidStack(resource.getFluid(), 1);
            }
            amountMb += filled;
            onChange.run();
        }
        return filled;
    }

    /** Accepts reactor/pipe push without per-call {@code int} truncation on the accepted total. */
    public long fillLong(FluidStack resource, boolean simulate) {
        if (resource.isEmpty() || amountMb >= capacityMb) {
            return 0L;
        }
        if (!fluid.isEmpty() && fluid.getFluid() != resource.getFluid()) {
            return 0L;
        }
        long space = capacityMb - amountMb;
        long toFill = Math.min(space, resource.getAmount());
        if (toFill <= 0) {
            return 0L;
        }
        if (!simulate) {
            if (fluid.isEmpty()) {
                fluid = new FluidStack(resource.getFluid(), 1);
            }
            amountMb += toFill;
            onChange.run();
        }
        return toFill;
    }

    @Override
    @NotNull
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || fluid.isEmpty() || fluid.getFluid() != resource.getFluid()) {
            return FluidStack.EMPTY;
        }
        return drain((int) Math.min(resource.getAmount(), Integer.MAX_VALUE), action);
    }

    @Override
    @NotNull
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (maxDrain <= 0 || fluid.isEmpty() || amountMb <= 0) {
            return FluidStack.EMPTY;
        }
        long drained = Math.min(amountMb, maxDrain);
        int out = (int) drained;
        if (!action.simulate()) {
            amountMb -= drained;
            if (amountMb <= 0) {
                fluid = FluidStack.EMPTY;
                amountMb = 0L;
            }
            onChange.run();
        }
        return new FluidStack(fluid.getFluid(), out);
    }

    public long drainLong(long maxDrain, boolean simulate) {
        if (maxDrain <= 0 || fluid.isEmpty() || amountMb <= 0) {
            return 0L;
        }
        long drained = Math.min(amountMb, maxDrain);
        if (!simulate) {
            amountMb -= drained;
            if (amountMb <= 0) {
                fluid = FluidStack.EMPTY;
                amountMb = 0L;
            }
            onChange.run();
        }
        return drained;
    }
}
