package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.unfamily.colossal_reactors.transfer.LegacyIFluidHandlerResourceHandler;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.heatsink.HeatSinkLoader;
import net.unfamily.colossal_reactors.menu.ReactorBuilderMenu;
import net.unfamily.colossal_reactors.reactor.ReactorBuildLogic;
import org.jetbrains.annotations.Nullable;

/**
 * BlockEntity for Reactor Builder. Holds a 9x3 buffer inventory and a fluid tank (same capacity and rules as Resource Port).
 */
public class ReactorBuilderBlockEntity extends BlockEntity implements MenuProvider {

    private static final String TAG_BUFFER = "Buffer";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_FLUID_ID = "FluidId";
    private static final String TAG_FLUID_AMOUNT = "Amount";
    private static final String TAG_SIZE_L = "SizeL";
    private static final String TAG_SIZE_R = "SizeR";
    private static final String TAG_SIZE_H = "SizeH";
    private static final String TAG_SIZE_D = "SizeD";
    private static final String TAG_SIZE_W = "SizeW";
    private static final String TAG_HEAT_SINK_IDX = "HeatSinkIdx";
    private static final String TAG_OPEN_TOP = "OpenTop";
    private static final String TAG_ROD_PATTERN = "RodPattern";
    private static final String TAG_PATTERN_MODE = "PatternMode";
    private static final String TAG_BUILDING = "Building";
    private static final String TAG_INVALID_BLOCKS = "InvalidBlocks";
    private static final int BUFFER_SLOTS = 9 * 3;
    private static final int MIN_SIZE = 1;

    private static int getTankCapacityMb() {
        return Config.RESOURCE_PORT_TANK_CAPACITY_MB.get();
    }

    private static int getMaxWidth() {
        return Config.MAX_REACTOR_WIDTH.get();
    }

    private static int getMaxHeight() {
        return Config.MAX_REACTOR_HEIGHT.get();
    }

    private static int getMaxDepth() {
        return Config.MAX_REACTOR_LENGTH.get();
    }

    private final ItemStackHandler bufferHandler = new ItemStackHandler(BUFFER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final FluidTank fluidTank = new FluidTank(getTankCapacityMb(), stack -> {
        if (stack.isEmpty()) {
            return true;
        }
        Level l = ReactorBuilderBlockEntity.this.getLevel();
        if (l == null) {
            return false;
        }
        return HeatSinkLoader.getModifiersForFluid(stack.getFluid(), l.registryAccess()) != null;
    }) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final ResourceHandler<FluidResource> fluidResourceCapability =
            LegacyIFluidHandlerResourceHandler.wrap(fluidTank);

    private final ContainerData fluidData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> fluidTank.getFluidAmount();
                case 1 -> fluidTank.getCapacity();
                case 2 -> fluidTank.getFluid().isEmpty()
                        ? -1
                        : BuiltInRegistries.FLUID.getId(fluidTank.getFluid().getFluid());
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
            return 3;
        }
    };

    /** Default 7x6x6 centered on X: 3 left + 3 right = 7 width, height 6, depth 6. */
    private int sizeLeft = 3;
    private int sizeRight = 3;
    private int sizeHeight = 6;
    private int sizeDepth = 6;
    /** Heat sink option index for fill: 0 = Air, 1.. = HeatSinkLoader definition index. */
    private int selectedHeatSinkIndex = 0;
    /** When built: true = top face open for manual edits, false = closed. */
    private boolean openTop = false;
    /** Rod column pattern: 0=DOTS, 1=CHECKERBOARD, 2=EXPANSION. */
    private int rodPattern = 0;
    /** Pattern mode: 0=OPTIMIZED (-2 inset), 1=PRODUCTION (no inset), 2=ECONOMY (like optimized, border fill differs). */
    private int patternMode = 0;
    /** True when build is in progress (button shows Stop). */
    private boolean building = false;
    /** True when build stopped because one or more invalid blocks were found (red zone). Cleared on start/stop. */
    private boolean invalidBlocksDetected = false;

    private static final int ROD_PATTERN_COUNT = 4;
    private static final int PATTERN_MODE_COUNT = 4;

    private final ContainerData sizeData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> sizeLeft;
                case 1 -> sizeRight;
                case 2 -> sizeHeight;
                case 3 -> sizeDepth;
                case 4 -> worldPosition.getX();
                case 5 -> worldPosition.getY();
                case 6 -> worldPosition.getZ();
                case 7 -> selectedHeatSinkIndex;
                case 8 -> openTop ? 1 : 0;
                case 9 -> rodPattern;
                case 10 -> patternMode;
                case 11 -> building ? 1 : 0;
                case 12 -> invalidBlocksDetected ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 4 && index != 7 && index != 8 && index != 9 && index != 10 && index != 11 && index != 12) return;
            switch (index) {
                case 0 -> {
                    sizeLeft = Math.max(0, Math.min(getMaxWidth() - sizeRight, value));
                    if (sizeLeft + sizeRight < 1) sizeRight = 1 - sizeLeft;
                }
                case 1 -> {
                    sizeRight = Math.max(0, Math.min(getMaxWidth() - sizeLeft, value));
                    if (sizeLeft + sizeRight < 1) sizeLeft = 1 - sizeRight;
                }
                case 2 -> sizeHeight = Math.max(MIN_SIZE, Math.min(getMaxHeight(), value));
                case 3 -> sizeDepth = Math.max(MIN_SIZE, Math.min(getMaxDepth(), value));
                case 7 -> selectedHeatSinkIndex = Math.max(0, Math.min(HeatSinkLoader.getHeatSinkOptionCount() - 1, value));
                case 8 -> openTop = value != 0;
                case 9 -> rodPattern = Math.max(0, Math.min(ROD_PATTERN_COUNT - 1, value));
                case 10 -> patternMode = Math.max(0, Math.min(PATTERN_MODE_COUNT - 1, value));
                case 11 -> building = value != 0;
                case 12 -> invalidBlocksDetected = value != 0;
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 13;
        }
    };

    public ReactorBuilderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REACTOR_BUILDER_BE.get(), pos, state);
    }

    public IItemHandler getBufferHandler() {
        return bufferHandler;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    public ResourceHandler<FluidResource> getFluidResourceCapability() {
        return fluidResourceCapability;
    }

    public ContainerData getFluidData() {
        return fluidData;
    }

    public ContainerData getSizeData() {
        return sizeData;
    }

    public int getSizeLeft() { return sizeLeft; }
    public int getSizeRight() { return sizeRight; }
    public int getSizeHeight() { return sizeHeight; }
    public int getSizeDepth() { return sizeDepth; }
    public int getSelectedHeatSinkIndex() { return selectedHeatSinkIndex; }
    public boolean isOpenTop() { return openTop; }
    public int getRodPattern() { return rodPattern; }
    public int getPatternMode() { return patternMode; }
    public boolean isBuilding() { return building; }
    public boolean isInvalidBlocksDetected() { return invalidBlocksDetected; }

    /** Start building: only if no red zone. Called from server when user presses Build. Sets invalidBlocksDetected when refusing so GUI shows warning. */
    public void startBuild() {
        if (level == null || level.isClientSide()) return;
        if (ReactorBuildLogic.hasRedZone((net.minecraft.server.level.ServerLevel) level, this)) {
            invalidBlocksDetected = true;
            setChanged();
            return;
        }
        building = true;
        invalidBlocksDetected = false;
        setChanged();
    }

    /** Stop building. Called when user presses Stop or when build finishes/aborts. Does not clear invalidBlocksDetected (so warning stays until next start or stop). */
    public void stopBuild() {
        building = false;
        setChanged();
    }

    /** Clear invalid-blocks warning. Called when user presses Build (start) or Stop so the message is dismissed. */
    public void clearInvalidBlocksFlag() {
        if (invalidBlocksDetected) {
            invalidBlocksDetected = false;
            setChanged();
        }
    }

    /** Server tick: advance build one step. Called from block ticker. */
    public void serverTick() {
        if (!building || level == null || level.isClientSide()) return;
        if (!ReactorBuildLogic.tick(this)) {
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && ReactorBuildLogic.hasRedZone(serverLevel, this)) {
                invalidBlocksDetected = true;
            }
            stopBuild();
        }
    }

    /** Toggle open top (one click cycles open/closed). The argument is ignored (kept for packet shape). */
    public void cycleOpenTop(boolean unusedNext) {
        openTop = !openTop;
        setChanged();
    }

    /** Cycle rod pattern: next=true next, next=false previous. */
    public void cycleRodPattern(boolean next) {
        if (next) rodPattern = (rodPattern + 1) % ROD_PATTERN_COUNT;
        else rodPattern = rodPattern <= 0 ? ROD_PATTERN_COUNT - 1 : rodPattern - 1;
        setChanged();
    }

    /** Cycle pattern mode: next=true next, next=false previous. */
    public void cyclePatternMode(boolean next) {
        if (next) patternMode = (patternMode + 1) % PATTERN_MODE_COUNT;
        else patternMode = patternMode <= 0 ? PATTERN_MODE_COUNT - 1 : patternMode - 1;
        setChanged();
    }

    /** Cycle heat sink option: next=true next, next=false previous. Left click=next, right click=previous. */
    public void cycleHeatSink(boolean next) {
        int max = HeatSinkLoader.getHeatSinkOptionCount() - 1;
        if (next) selectedHeatSinkIndex = selectedHeatSinkIndex >= max ? 0 : selectedHeatSinkIndex + 1;
        else selectedHeatSinkIndex = selectedHeatSinkIndex <= 0 ? max : selectedHeatSinkIndex - 1;
        setChanged();
    }

    /**
     * Returns the reactor volume AABB in world coordinates (block-aligned).
     * Reactor extends from one block behind the builder (opposite of facing), with left/right/up/depth from sizes.
     */
    public static AABB getReactorVolumeAABB(BlockPos builderPos, Direction facing, int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth) {
        Direction back = facing.getOpposite();
        Direction left = facing.getCounterClockWise(Direction.Axis.Y);
        Direction right = facing.getClockWise(Direction.Axis.Y);
        BlockPos base = builderPos.relative(back, 1);
        int minX = base.getX(), maxX = base.getX();
        int minY = base.getY(), maxY = base.getY();
        int minZ = base.getZ(), maxZ = base.getZ();
        for (int dl = 0; dl <= sizeLeft; dl++) {
            for (int dr = 0; dr <= sizeRight; dr++) {
                for (int dy = 0; dy <= sizeHeight; dy++) {
                    for (int dd = 0; dd <= sizeDepth; dd++) {
                        int x = base.getX() + left.getStepX() * dl + right.getStepX() * dr + back.getStepX() * dd;
                        int y = base.getY() + Direction.UP.getStepY() * dy + left.getStepY() * dl + right.getStepY() * dr + back.getStepY() * dd;
                        int z = base.getZ() + left.getStepZ() * dl + right.getStepZ() * dr + back.getStepZ() * dd;
                        minX = Math.min(minX, x); maxX = Math.max(maxX, x);
                        minY = Math.min(minY, y); maxY = Math.max(maxY, y);
                        minZ = Math.min(minZ, z); maxZ = Math.max(maxZ, z);
                    }
                }
            }
        }
        return new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
    }

    /** Direction: 0=up, 1=left button (adjusts sizeRight), 2=right button (adjusts sizeLeft), 3=behind. amount=1,5,10. */
    public void adjustSize(int direction, boolean increment, int amount) {
        int delta = increment ? amount : -amount;
        switch (direction) {
            case 0 -> sizeHeight = Math.max(MIN_SIZE, Math.min(getMaxHeight(), sizeHeight + delta));
            case 1 -> {
                int newR = Math.max(0, Math.min(getMaxWidth() - sizeLeft, sizeRight + delta));
                sizeRight = newR;
                if (sizeLeft + sizeRight < 1) sizeLeft = 1 - sizeRight;
            }
            case 2 -> {
                int newL = Math.max(0, Math.min(getMaxWidth() - sizeRight, sizeLeft + delta));
                sizeLeft = newL;
                if (sizeLeft + sizeRight < 1) sizeRight = 1 - sizeLeft;
            }
            case 3 -> sizeDepth = Math.max(MIN_SIZE, Math.min(getMaxDepth(), sizeDepth + delta));
            default -> {}
        }
        if (sizeLeft + sizeRight < 1) { sizeLeft = 1; sizeRight = 0; }
        setChanged();
    }

    /** Single step (backward compatible). */
    public void adjustSize(int direction, boolean increment) {
        adjustSize(direction, increment, 1);
    }

    /**
     * Fill tank from item or drain tank to item. Only accepts fluids that are valid reactor coolant (heat sink valid_liquids).
     */
    public boolean interactWithItemFluidHandler(IFluidHandlerItem itemHandler, Player player) {
        if (itemHandler.getTanks() == 0) return false;
        IFluidHandler blockHandler = fluidTank;
        FluidStack inItem = itemHandler.getFluidInTank(0);
        if (!inItem.isEmpty()) {
            if (getLevel() == null) return false;
            if (HeatSinkLoader.getModifiersForFluid(inItem.getFluid(), getLevel().registryAccess()) == null)
                return false;
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
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        bufferHandler.serialize(output);
        FluidStack stack = fluidTank.getFluid();
        if (!stack.isEmpty()) {
            ValueOutput fluidOut = output.child(TAG_FLUID);
            fluidOut.putString(TAG_FLUID_ID, BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
            fluidOut.putInt(TAG_FLUID_AMOUNT, stack.getAmount());
        }
        output.putInt(TAG_SIZE_L, sizeLeft);
        output.putInt(TAG_SIZE_R, sizeRight);
        output.putInt(TAG_SIZE_H, sizeHeight);
        output.putInt(TAG_SIZE_D, sizeDepth);
        output.putInt(TAG_HEAT_SINK_IDX, selectedHeatSinkIndex);
        output.putBoolean(TAG_OPEN_TOP, openTop);
        output.putInt(TAG_ROD_PATTERN, rodPattern);
        output.putInt(TAG_PATTERN_MODE, patternMode);
        output.putBoolean(TAG_BUILDING, building);
        output.putBoolean(TAG_INVALID_BLOCKS, invalidBlocksDetected);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        bufferHandler.deserialize(input);
        fluidTank.setFluid(FluidStack.EMPTY);
        input.child(TAG_FLUID).ifPresent(fluidIn -> {
            String idStr = fluidIn.getStringOr(TAG_FLUID_ID, "");
            Identifier fluidId = Identifier.tryParse(idStr);
            int amount = fluidIn.getIntOr(TAG_FLUID_AMOUNT, 0);
            if (fluidId != null && amount > 0) {
                Fluid fluid = BuiltInRegistries.FLUID.getValue(fluidId);
                if (fluid != null && fluid != Fluids.EMPTY) {
                    fluidTank.setFluid(new FluidStack(fluid, amount));
                }
            }
        });
        if (input.getInt(TAG_SIZE_L).isPresent() || input.getInt(TAG_SIZE_R).isPresent()) {
            sizeLeft = Math.max(0, Math.min(getMaxWidth(), input.getIntOr(TAG_SIZE_L, sizeLeft)));
            sizeRight = Math.max(0, Math.min(getMaxWidth(), input.getIntOr(TAG_SIZE_R, sizeRight)));
            if (sizeLeft + sizeRight > getMaxWidth()) {
                int total = sizeLeft + sizeRight;
                sizeLeft = (sizeLeft * getMaxWidth()) / total;
                sizeRight = getMaxWidth() - sizeLeft;
            }
            if (sizeLeft + sizeRight < 1) {
                sizeLeft = 1;
                sizeRight = 0;
            }
        } else if (input.getInt(TAG_SIZE_W).isPresent()) {
            int w = Math.max(MIN_SIZE, Math.min(getMaxWidth(), input.getIntOr(TAG_SIZE_W, MIN_SIZE)));
            sizeLeft = w / 2;
            sizeRight = w - sizeLeft;
        }
        input.getInt(TAG_SIZE_H).ifPresent(v -> sizeHeight = Math.max(MIN_SIZE, Math.min(getMaxHeight(), v)));
        input.getInt(TAG_SIZE_D).ifPresent(v -> sizeDepth = Math.max(MIN_SIZE, Math.min(getMaxDepth(), v)));
        input.getInt(TAG_HEAT_SINK_IDX).ifPresent(v -> selectedHeatSinkIndex = Math.max(0, Math.min(HeatSinkLoader.getHeatSinkOptionCount() - 1, v)));
        openTop = input.getBooleanOr(TAG_OPEN_TOP, openTop);
        input.getInt(TAG_ROD_PATTERN).ifPresent(v -> rodPattern = Math.max(0, Math.min(ROD_PATTERN_COUNT - 1, v)));
        input.getInt(TAG_PATTERN_MODE).ifPresent(v -> patternMode = Math.max(0, Math.min(PATTERN_MODE_COUNT - 1, v)));
        building = input.getBooleanOr(TAG_BUILDING, building);
        invalidBlocksDetected = input.getBooleanOr(TAG_INVALID_BLOCKS, invalidBlocksDetected);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.reactor_builder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ReactorBuilderMenu(containerId, playerInventory, this);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState oldState) {
        var level = getLevel();
        if (level != null && !oldState.is(level.getBlockState(pos).getBlock())) {
            dropAllContents();
        }
        super.preRemoveSideEffects(pos, oldState);
    }

    /** Drops all buffer contents into the world when the block is removed. */
    public void dropAllContents() {
        if (level == null || level.isClientSide()) return;
        Vec3 center = Vec3.atCenterOf(worldPosition);
        for (int i = 0; i < bufferHandler.getSlots(); i++) {
            var stack = bufferHandler.getStackInSlot(i);
            if (!stack.isEmpty()) {
                net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(level, center.x, center.y, center.z, stack);
                level.addFreshEntity(entity);
                bufferHandler.setStackInSlot(i, net.minecraft.world.item.ItemStack.EMPTY);
            }
        }
    }
}
