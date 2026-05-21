package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.unfamily.colossal_reactors.transfer.LegacyIFluidHandlerResourceHandler;
import net.unfamily.iskalib.transfer.LegacyItemHandlerResourceHandler;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.turbine.ElecCoilLoader;
import net.unfamily.colossal_reactors.menu.TurbineBuilderMenu;
import net.unfamily.colossal_reactors.turbine.TurbineBuildLogic;
import net.unfamily.colossal_reactors.turbine.TurbinePlacementAxis;
import net.unfamily.colossal_reactors.turbine.TurbineRodSpaceLayout;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * BlockEntity for Turbine Builder. Holds a 9x3 buffer inventory and a fluid tank for steam marking.
 */
public class TurbineBuilderBlockEntity extends BlockEntity implements MenuProvider {

    private static final String TAG_BUFFER = "Buffer";
    private static final String TAG_FLUID = "Fluid";
    private static final String TAG_FLUID_ID = "FluidId";
    private static final String TAG_FLUID_AMOUNT = "Amount";
    private static final String TAG_SIZE_L = "SizeL";
    private static final String TAG_SIZE_R = "SizeR";
    private static final String TAG_SIZE_H = "SizeH";
    private static final String TAG_SIZE_D = "SizeD";
    private static final String TAG_SIZE_W = "SizeW";
    private static final String TAG_COIL_IDX = "CoilIdx";
    private static final String TAG_OPEN_TOP = "OpenTop";
    private static final String TAG_ROD_PATTERN = "RodPattern";
    private static final String TAG_PATTERN_MODE = "PatternMode";
    private static final String TAG_BUILDING = "Building";
    private static final String TAG_INVALID_BLOCKS = "InvalidBlocks";
    private static final String TAG_BUILD_STAGE = "BuildStage";
    private static final String TAG_BUILD_FRAME_X = "BuildFrameX";
    private static final String TAG_BUILD_FRAME_Y = "BuildFrameY";
    private static final String TAG_BUILD_FRAME_Z = "BuildFrameZ";
    private static final String TAG_BUILD_RODCTRL_RX = "BuildRodCtrlRx";
    private static final String TAG_BUILD_RODCTRL_RZ = "BuildRodCtrlRz";
    private static final String TAG_BUILD_ROD_LX = "BuildRodLx";
    private static final String TAG_BUILD_ROD_LY = "BuildRodLy";
    private static final String TAG_BUILD_ROD_LZ = "BuildRodLz";
    private static final String TAG_BUILD_LIQUID_LX = "BuildLiquidLx";
    private static final String TAG_BUILD_LIQUID_LY = "BuildLiquidLy";
    private static final String TAG_BUILD_LIQUID_LZ = "BuildLiquidLz";
    private static final String TAG_BUILD_HEAT_LX = "BuildHeatLx";
    private static final String TAG_BUILD_HEAT_LY = "BuildHeatLy";
    private static final String TAG_BUILD_HEAT_LZ = "BuildHeatLz";
    private static final String TAG_BUILD_PROGRESS = "BuildProgress";
    private static final String TAG_BUILD_PROGRESS_VISIBLE = "BuildProgressVisible";
    private static final String TAG_PLACEMENT_AXIS = "PlacementAxis";
    private static final String TAG_MARK_INPUT_FILTERS = "MarkInputFilters";
    private static final int BUFFER_SLOTS = 9 * 3;

    private final List<ItemStack> markInputFilters = new ArrayList<>();
    private static final int MIN_SIZE = 1;

    private static int getTankCapacityMb() {
        return Config.TURBINE_BUILDER_TANK_CAPACITY_MB.get();
    }

    private static int getMaxWidth() {
        // Internal builder sizes store "offsets" (e.g. width displayed = (L+R)+1).
        // Config MAX_REACTOR_WIDTH is the displayed block count, so clamp offsets to (max - 1).
        return Math.max(MIN_SIZE, Config.MAX_TURBINE_WIDTH.get() - 1);
    }

    private static int getMaxHeight() {
        return Math.max(MIN_SIZE, Config.MAX_TURBINE_HEIGHT.get() - 1);
    }

    private static int getMaxDepth() {
        return Math.max(MIN_SIZE, Config.MAX_TURBINE_LENGTH.get() - 1);
    }

    private final ItemStackHandler bufferHandler = new ItemStackHandler(BUFFER_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= 0 && slot < markInputFilters.size()) {
                ItemStack filter = markInputFilters.get(slot);
                if (!filter.isEmpty()) {
                    return ItemStack.isSameItemSameComponents(stack, filter);
                }
            }
            return true;
        }
    };

    private final FluidTank fluidTank = new FluidTank(getTankCapacityMb()) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private final ResourceHandler<FluidResource> fluidResourceCapability =
            LegacyIFluidHandlerResourceHandler.wrap(fluidTank);
    private ResourceHandler<ItemResource> itemResourceCapability;

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
    /** Heat sink option index for fill: 0 = Air, 1.. = ElecCoilLoader definition index. */
    private int selectedCoilIndex = 0;
    /** When built: true = absolute top face open (no top casing); rod controller still placed. */
    private boolean openTop = false;
    private int coilLayerCount = net.unfamily.colossal_reactors.Config.TURBINE_DEFAULT_COIL_LAYER_COUNT.get();
    /** Blade growth pattern: 0=Efficient, 1=Productive. */
    private int rodPattern = 0;
    /** Rod / rod-controller facing axis (default D-U: bottom to top). */
    private Direction placementAxis = TurbinePlacementAxis.DOWN_UP.facing();
    /** Pattern mode: 0=OPTIMIZED (-2 inset), 1=PRODUCTION (no inset), 2=ECONOMY (like optimized, border fill differs). */
    /** True when build is in progress (button shows Stop). */
    private boolean building = false;
    /** True when build stopped because one or more invalid blocks were found (red zone). Cleared on start/stop. */
    private boolean invalidBlocksDetected = false;
    /** Last computed build progress (0-100). Kept visible after build completes/aborts until user stops or restarts. */
    private int buildProgressPercent = 0;
    private boolean buildProgressVisible = false;

    // Build progress cursors (NEXT position to process). These make building "forward-only" and avoid rescanning from start.
    private int buildStage = 0;
    private int buildFrameX = Integer.MIN_VALUE, buildFrameY = Integer.MIN_VALUE, buildFrameZ = Integer.MIN_VALUE;
    private int buildRodCtrlRx = Integer.MIN_VALUE, buildRodCtrlRz = Integer.MIN_VALUE;
    private int buildRodLx = Integer.MIN_VALUE, buildRodLy = Integer.MIN_VALUE, buildRodLz = Integer.MIN_VALUE;
    private int buildLiquidLx = Integer.MIN_VALUE, buildLiquidLy = Integer.MIN_VALUE, buildLiquidLz = Integer.MIN_VALUE;
    private int buildHeatLx = Integer.MIN_VALUE, buildHeatLy = Integer.MIN_VALUE, buildHeatLz = Integer.MIN_VALUE;

    private static final int ROD_PATTERN_COUNT = 2;
    private static final int COIL_LAYER_MIN = 0;
    private static final int COIL_LAYER_MAX = 32;

    public void resetBuildProgress() {
        buildStage = 0;
        buildFrameX = buildFrameY = buildFrameZ = Integer.MIN_VALUE;
        buildRodCtrlRx = buildRodCtrlRz = Integer.MIN_VALUE;
        buildRodLx = buildRodLy = buildRodLz = Integer.MIN_VALUE;
        buildLiquidLx = buildLiquidLy = buildLiquidLz = Integer.MIN_VALUE;
        buildHeatLx = buildHeatLy = buildHeatLz = Integer.MIN_VALUE;
    }

    public int getBuildStage() { return buildStage; }
    public void setBuildStage(int stage) { this.buildStage = stage; }

    public int getBuildFrameX() { return buildFrameX; }
    public int getBuildFrameY() { return buildFrameY; }
    public int getBuildFrameZ() { return buildFrameZ; }
    public void setBuildFrameCursor(int x, int y, int z) { buildFrameX = x; buildFrameY = y; buildFrameZ = z; }

    public int getBuildRodCtrlRx() { return buildRodCtrlRx; }
    public int getBuildRodCtrlRz() { return buildRodCtrlRz; }
    public void setBuildRodCtrlCursor(int rx, int rz) { buildRodCtrlRx = rx; buildRodCtrlRz = rz; }

    public int getBuildRodLx() { return buildRodLx; }
    public int getBuildRodLy() { return buildRodLy; }
    public int getBuildRodLz() { return buildRodLz; }
    public void setBuildRodCursor(int lx, int ly, int lz) { buildRodLx = lx; buildRodLy = ly; buildRodLz = lz; }

    public int getBuildLiquidLx() { return buildLiquidLx; }
    public int getBuildLiquidLy() { return buildLiquidLy; }
    public int getBuildLiquidLz() { return buildLiquidLz; }
    public void setBuildLiquidCursor(int lx, int ly, int lz) { buildLiquidLx = lx; buildLiquidLy = ly; buildLiquidLz = lz; }

    public int getBuildHeatLx() { return buildHeatLx; }
    public int getBuildHeatLy() { return buildHeatLy; }
    public int getBuildHeatLz() { return buildHeatLz; }
    public void setBuildHeatCursor(int lx, int ly, int lz) { buildHeatLx = lx; buildHeatLy = ly; buildHeatLz = lz; }

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
                case 7 -> selectedCoilIndex;
                case 8 -> coilLayerCount;
                case 9 -> rodPattern;
                case 10 -> openTop ? 1 : 0;
                case 11 -> building ? 1 : 0;
                case 12 -> invalidBlocksDetected ? 1 : 0;
                case 13 -> buildProgressPercent;
                case 14 -> buildProgressVisible ? 1 : 0;
                case 15 -> placementAxis.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index >= 4 && index != 7 && index != 8 && index != 9 && index != 10 && index != 11 && index != 12 && index != 13 && index != 14 && index != 15) {
                return;
            }
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
                case 7 -> selectedCoilIndex = Math.max(0, Math.min(ElecCoilLoader.getCoilOptionCount() - 1, value));
                case 8 -> coilLayerCount = Math.max(COIL_LAYER_MIN, Math.min(COIL_LAYER_MAX, value));
                case 9 -> rodPattern = Math.max(0, Math.min(ROD_PATTERN_COUNT - 1, value));
                case 10 -> openTop = value != 0;
                case 11 -> building = value != 0;
                case 12 -> invalidBlocksDetected = value != 0;
                case 13 -> buildProgressPercent = Math.max(0, Math.min(100, value));
                case 14 -> buildProgressVisible = value != 0;
                case 15 -> {
                    Direction[] dirs = Direction.values();
                    if (value >= 0 && value < dirs.length) {
                        placementAxis = dirs[value];
                    }
                }
                default -> {}
            }
        }

        @Override
        public int getCount() {
            return 16;
        }
    };

    public TurbineBuilderBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_BUILDER_BE.get(), pos, state);
        for (int i = 0; i < BUFFER_SLOTS; i++) {
            markInputFilters.add(ItemStack.EMPTY);
        }
    }

    public void applyMarkInputFromBuffer() {
        for (int slot = 0; slot < bufferHandler.getSlots(); slot++) {
            ItemStack current = bufferHandler.getStackInSlot(slot);
            if (!current.isEmpty()) {
                markInputFilters.set(slot, current.copyWithCount(1));
            }
        }
        setChanged();
    }

    public void clearAllMarkInputFilters() {
        for (int i = 0; i < markInputFilters.size(); i++) {
            markInputFilters.set(i, ItemStack.EMPTY);
        }
        setChanged();
    }

    public void clearMarkInputFiltersWithoutMatchingStacks() {
        for (int slot = 0; slot < markInputFilters.size(); slot++) {
            ItemStack filter = markInputFilters.get(slot);
            if (filter.isEmpty()) continue;
            ItemStack current = bufferHandler.getStackInSlot(slot);
            if (current.isEmpty() || !ItemStack.isSameItemSameComponents(current, filter)) {
                markInputFilters.set(slot, ItemStack.EMPTY);
            }
        }
        setChanged();
    }

    public ItemStack getMarkInputFilter(int slot) {
        if (slot >= 0 && slot < markInputFilters.size()) {
            return markInputFilters.get(slot);
        }
        return ItemStack.EMPTY;
    }

    public boolean hasMarkInputFilter(int slot) {
        return !getMarkInputFilter(slot).isEmpty();
    }

    public IItemHandler getBufferHandler() {
        return bufferHandler;
    }

    /** Buffer exposed to automation (hoppers/pipes). */
    public IItemHandler getItemHandlerForCapability() {
        return bufferHandler;
    }

    public ResourceHandler<ItemResource> getItemResourceHandlerForCapability() {
        if (itemResourceCapability == null) {
            itemResourceCapability = LegacyItemHandlerResourceHandler.wrap(bufferHandler);
        }
        return itemResourceCapability;
    }

    public FluidTank getFluidTank() {
        return fluidTank;
    }

    /** Server: discard all fluid in the internal tank (GUI dump). @return true if any fluid was removed */
    public boolean dumpFluidTankContents() {
        if (level == null || level.isClientSide()) return false;
        if (fluidTank.getFluid().isEmpty()) return false;
        fluidTank.setFluid(FluidStack.EMPTY);
        setChanged();
        return true;
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
    public int getSelectedCoilIndex() { return selectedCoilIndex; }
    public int getCoilLayerCount() { return coilLayerCount; }
    public int getRodPattern() { return rodPattern; }
    public Direction getPlacementAxis() { return placementAxis; }

    public int getPlacementAxisIndex() {
        return TurbinePlacementAxis.fromFacing(placementAxis).ordinal();
    }

    /** Layers used by build/simulation (GUI setting + 1, clamped). */
    public int getAppliedCoilLayerCount() {
        return TurbineRodSpaceLayout.appliedCoilLayerCount(interiorExtentAlongPlacementAxis(), coilLayerCount);
    }

    private int interiorExtentAlongPlacementAxis() {
        int w = sizeLeft + sizeRight + 1;
        int h = sizeHeight + 1;
        int d = sizeDepth + 1;
        return switch (placementAxis.getAxis()) {
            case Y -> TurbineRodSpaceLayout.interiorHeight(h);
            case Z -> TurbineRodSpaceLayout.interiorDepth(d);
            case X -> TurbineRodSpaceLayout.interiorWidth(w);
            default -> TurbineRodSpaceLayout.interiorHeight(h);
        };
    }

    private int maxCoilLayersForCurrentHeight() {
        return Math.min(COIL_LAYER_MAX, TurbineRodSpaceLayout.maxCoilLayerSettingForInterior(interiorExtentAlongPlacementAxis()));
    }

    private void clampCoilLayerCountToFit() {
        int max = maxCoilLayersForCurrentHeight();
        if (coilLayerCount > max) {
            coilLayerCount = Math.max(COIL_LAYER_MIN, max);
            setChanged();
        }
    }

    public boolean isBuilding() { return building; }
    public boolean isInvalidBlocksDetected() { return invalidBlocksDetected; }
    public int getBuildProgressPercent() { return buildProgressPercent; }
    public boolean isBuildProgressVisible() { return buildProgressVisible; }

    /** Start building: only if no red zone. Called from server when user presses Build. Sets invalidBlocksDetected when refusing so GUI shows warning. */
    public void startBuild() {
        if (level == null || level.isClientSide()) return;
        if (TurbineBuildLogic.hasRedZone((net.minecraft.server.level.ServerLevel) level, this)) {
            invalidBlocksDetected = true;
            setChanged();
            return;
        }
        building = true;
        buildProgressVisible = true;
        buildProgressPercent = 0;
        resetBuildProgress();
        invalidBlocksDetected = false;
        setChanged();
    }

    /** Stop building. Called when user presses Stop. Clears progress visibility. */
    public void stopBuild() {
        stopBuild(true);
    }

    /**
     * Stop building. When {@code resetProgress} is false, keep progress visible so the GUI can show 100% (or last %) after completion/abort.
     */
    public void stopBuild(boolean resetProgress) {
        building = false;
        if (resetProgress) {
            buildProgressVisible = false;
            buildProgressPercent = 0;
            resetBuildProgress();
        }
        setChanged();
    }

    /** Clear build warnings. Called when user presses Build (start) or Stop so the message is dismissed. */
    public void clearInvalidBlocksFlag() {
        if (invalidBlocksDetected) {
            invalidBlocksDetected = false;
            setChanged();
        }
    }

    /** Server tick: advance build one step. Called from block ticker. */
    public void serverTick() {
        if (!building || level == null || level.isClientSide()) return;
        int steps = net.unfamily.colossal_reactors.Config.TURBINE_BUILDER_BUILD_STEPS_PER_TICK.get();
        TurbineBuildLogic.tick(this, steps);
        if (level instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            buildProgressPercent = computeBuildProgressPercent(serverLevel);
        }
        if (buildStage >= TurbineBuildLogic.STAGE_DONE) {
            buildProgressPercent = 100;
            stopBuild(false);
            return;
        }
        if (!building) {
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel && TurbineBuildLogic.hasRedZone(serverLevel, this)) {
                invalidBlocksDetected = true;
            }
            stopBuild(false);
        }
    }

    private int computeBuildProgressPercent(net.minecraft.server.level.ServerLevel serverLevel) {
        if (!buildProgressVisible) return 0;
        if (buildStage >= TurbineBuildLogic.STAGE_DONE) return 100;
        var counts = net.unfamily.colossal_reactors.turbine.TurbineBuildMaterialCounter.estimate(
                serverLevel.registryAccess(),
                getSizeLeft(), getSizeRight(), getSizeHeight(), getSizeDepth(),
                getPlacementAxisIndex(),
                getRodPattern(), getSelectedCoilIndex(), getAppliedCoilLayerCount(), isOpenTop());
        long frameTotal = counts.frameShellTotal();
        long deckTotal = counts.closureDeckCasings();
        long rodCtrlTotal = counts.rodControllers();
        long rodsTotal = counts.rods();
        long bladesTotal = counts.blades();
        long coilsTotal = counts.coilBlocks();
        long total = frameTotal + deckTotal + rodCtrlTotal + rodsTotal + bladesTotal + coilsTotal;
        if (total <= 0) return buildStage >= TurbineBuildLogic.STAGE_DONE ? 100 : 0;

        long done = switch (buildStage) {
            case TurbineBuildLogic.STAGE_FRAME -> 0;
            case TurbineBuildLogic.STAGE_CLOSURE_DECK -> frameTotal;
            case TurbineBuildLogic.STAGE_ROD_CONTROLLERS -> frameTotal + deckTotal;
            case TurbineBuildLogic.STAGE_RODS -> frameTotal + deckTotal + rodCtrlTotal;
            case TurbineBuildLogic.STAGE_BLADES -> frameTotal + deckTotal + rodCtrlTotal + rodsTotal;
            case TurbineBuildLogic.STAGE_COILS -> frameTotal + deckTotal + rodCtrlTotal + rodsTotal + bladesTotal;
            default -> total;
        };
        return (int) Math.max(0, Math.min(100, (done * 100L) / total));
    }

    public boolean isOpenTop() {
        return openTop;
    }

    /** Left click = open, right click = closed (same as reactor builder). */
    public void cycleOpenTop(boolean next) {
        openTop = next;
        setChanged();
    }

    /** Cycle coil layer count (1 .. max for current turbine height). */
    public void cycleCoilLayerCount(boolean next) {
        int maxForSize = maxCoilLayersForCurrentHeight();
        if (next) {
            coilLayerCount = coilLayerCount >= maxForSize ? COIL_LAYER_MIN : coilLayerCount + 1;
        } else {
            coilLayerCount = coilLayerCount <= COIL_LAYER_MIN ? maxForSize : coilLayerCount - 1;
        }
        setChanged();
    }

    /** Cycle rod pattern: Efficient / Productive. */
    public void cycleRodPattern(boolean next) {
        if (next) rodPattern = (rodPattern + 1) % ROD_PATTERN_COUNT;
        else rodPattern = rodPattern <= 0 ? ROD_PATTERN_COUNT - 1 : rodPattern - 1;
        setChanged();
    }

    /** Cycle placement axis (rod + rod controller facing). */
    public void cyclePlacementAxis(boolean next) {
        Direction[] dirs = Direction.values();
        int idx = placementAxis.ordinal();
        if (next) {
            placementAxis = dirs[(idx + 1) % dirs.length];
        } else {
            placementAxis = dirs[(idx + dirs.length - 1) % dirs.length];
        }
        setChanged();
    }

    /** Cycle elec coil block type. */
    public void cycleCoil(boolean next) {
        int max = ElecCoilLoader.getCoilOptionCount() - 1;
        if (next) selectedCoilIndex = selectedCoilIndex >= max ? 0 : selectedCoilIndex + 1;
        else selectedCoilIndex = selectedCoilIndex <= 0 ? max : selectedCoilIndex - 1;
        setChanged();
    }

    /**
     * Returns the reactor volume AABB in world coordinates (block-aligned).
     * Reactor extends from one block behind the builder (opposite of facing), with left/right/up/depth from sizes.
     */
    public static AABB getTurbineVolumeAABB(BlockPos builderPos, Direction facing, int sizeLeft, int sizeRight, int sizeHeight, int sizeDepth) {
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
            case 0 -> {
                sizeHeight = Math.max(MIN_SIZE, Math.min(getMaxHeight(), sizeHeight + delta));
                clampCoilLayerCountToFit();
            }
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
            if (net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader.getDefinitionForFluid(
                    inItem.getFluid(), getLevel().registryAccess()) == null)
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
        output.putInt(TAG_COIL_IDX, selectedCoilIndex);
        output.putInt("CoilLayerCount", coilLayerCount);
        output.putInt(TAG_ROD_PATTERN, rodPattern);
        output.putString(TAG_PLACEMENT_AXIS, placementAxis.getName());
        output.putBoolean(TAG_OPEN_TOP, openTop);
        output.putBoolean(TAG_BUILDING, building);
        output.putBoolean(TAG_INVALID_BLOCKS, invalidBlocksDetected);
        output.putInt(TAG_BUILD_STAGE, buildStage);
        output.putInt(TAG_BUILD_FRAME_X, buildFrameX);
        output.putInt(TAG_BUILD_FRAME_Y, buildFrameY);
        output.putInt(TAG_BUILD_FRAME_Z, buildFrameZ);
        output.putInt(TAG_BUILD_RODCTRL_RX, buildRodCtrlRx);
        output.putInt(TAG_BUILD_RODCTRL_RZ, buildRodCtrlRz);
        output.putInt(TAG_BUILD_ROD_LX, buildRodLx);
        output.putInt(TAG_BUILD_ROD_LY, buildRodLy);
        output.putInt(TAG_BUILD_ROD_LZ, buildRodLz);
        output.putInt(TAG_BUILD_LIQUID_LX, buildLiquidLx);
        output.putInt(TAG_BUILD_LIQUID_LY, buildLiquidLy);
        output.putInt(TAG_BUILD_LIQUID_LZ, buildLiquidLz);
        output.putInt(TAG_BUILD_HEAT_LX, buildHeatLx);
        output.putInt(TAG_BUILD_HEAT_LY, buildHeatLy);
        output.putInt(TAG_BUILD_HEAT_LZ, buildHeatLz);
        output.putInt(TAG_BUILD_PROGRESS, buildProgressPercent);
        output.putBoolean(TAG_BUILD_PROGRESS_VISIBLE, buildProgressVisible);
        ValueOutput markOut = output.child(TAG_MARK_INPUT_FILTERS);
        for (int i = 0; i < markInputFilters.size(); i++) {
            ItemStack filter = markInputFilters.get(i);
            if (!filter.isEmpty()) {
                markOut.store("slot" + i, ItemStack.CODEC, filter);
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        // Buffer inventory is synced through the open container menu; applying BE packets on the
        // client here can overwrite slot state and make items vanish in the GUI.
        if (level == null || !level.isClientSide()) {
            bufferHandler.deserialize(input);
        }
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
        input.getInt(TAG_COIL_IDX).ifPresent(v -> selectedCoilIndex = Math.max(0, Math.min(ElecCoilLoader.getCoilOptionCount() - 1, v)));
        input.getInt("CoilLayerCount").ifPresent(v -> coilLayerCount = Math.max(COIL_LAYER_MIN, Math.min(COIL_LAYER_MAX, v)));
        clampCoilLayerCountToFit();
        input.getInt(TAG_ROD_PATTERN).ifPresent(v -> rodPattern = Math.max(0, Math.min(ROD_PATTERN_COUNT - 1, v)));
        input.getString(TAG_PLACEMENT_AXIS).ifPresent(name -> {
            Direction parsed = Direction.byName(name);
            if (parsed != null) placementAxis = parsed;
        });
        openTop = input.getBooleanOr(TAG_OPEN_TOP, openTop);
        building = input.getBooleanOr(TAG_BUILDING, building);
        invalidBlocksDetected = input.getBooleanOr(TAG_INVALID_BLOCKS, invalidBlocksDetected);
        buildProgressPercent = input.getIntOr(TAG_BUILD_PROGRESS, buildProgressPercent);
        buildProgressVisible = input.getBooleanOr(TAG_BUILD_PROGRESS_VISIBLE, buildProgressVisible);
        buildStage = input.getIntOr(TAG_BUILD_STAGE, buildStage);
        buildFrameX = input.getIntOr(TAG_BUILD_FRAME_X, buildFrameX);
        buildFrameY = input.getIntOr(TAG_BUILD_FRAME_Y, buildFrameY);
        buildFrameZ = input.getIntOr(TAG_BUILD_FRAME_Z, buildFrameZ);
        buildRodCtrlRx = input.getIntOr(TAG_BUILD_RODCTRL_RX, buildRodCtrlRx);
        buildRodCtrlRz = input.getIntOr(TAG_BUILD_RODCTRL_RZ, buildRodCtrlRz);
        buildRodLx = input.getIntOr(TAG_BUILD_ROD_LX, buildRodLx);
        buildRodLy = input.getIntOr(TAG_BUILD_ROD_LY, buildRodLy);
        buildRodLz = input.getIntOr(TAG_BUILD_ROD_LZ, buildRodLz);
        buildLiquidLx = input.getIntOr(TAG_BUILD_LIQUID_LX, buildLiquidLx);
        buildLiquidLy = input.getIntOr(TAG_BUILD_LIQUID_LY, buildLiquidLy);
        buildLiquidLz = input.getIntOr(TAG_BUILD_LIQUID_LZ, buildLiquidLz);
        buildHeatLx = input.getIntOr(TAG_BUILD_HEAT_LX, buildHeatLx);
        buildHeatLy = input.getIntOr(TAG_BUILD_HEAT_LY, buildHeatLy);
        buildHeatLz = input.getIntOr(TAG_BUILD_HEAT_LZ, buildHeatLz);
        input.child(TAG_MARK_INPUT_FILTERS).ifPresent(markIn -> {
            for (int i = 0; i < markInputFilters.size(); i++) {
                markInputFilters.set(i, ItemStack.EMPTY);
            }
            for (int slot = 0; slot < BUFFER_SLOTS; slot++) {
                final int index = slot;
                markIn.read("slot" + index, ItemStack.CODEC).ifPresent(stack -> markInputFilters.set(index, stack));
            }
        });
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection connection, ValueInput valueInput) {
        loadWithComponents(valueInput);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.turbine_builder");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new TurbineBuilderMenu(containerId, playerInventory, this);
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
