package net.unfamily.colossal_reactors.blockentity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineControllerBlock;
import net.unfamily.colossal_reactors.block.TurbineVisualState;
import net.unfamily.colossal_reactors.menu.TurbineControllerMenu;
import net.unfamily.colossal_reactors.turbine.TurbineFiller;
import net.unfamily.colossal_reactors.turbine.TurbineGenerationLoader;
import net.unfamily.colossal_reactors.turbine.TurbineSimulation;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;

/**
 * Turbine controller: caches validation and runtime stats for GUI.
 */
public class TurbineControllerBlockEntity extends BlockEntity implements MenuProvider {

    private static final String TAG_STEAM_BUFFER = "SteamBuffer";
    private static final String TAG_OUTPUT_BUFFER = "OutputBuffer";

    public record FluidBufferEntry(Identifier fluidId, int mb) {}

    private static final Codec<FluidBufferEntry> FLUID_BUFFER_CODEC = RecordCodecBuilder.create(i -> i.group(
            Identifier.CODEC.fieldOf("FluidId").forGetter(FluidBufferEntry::fluidId),
            Codec.INT.fieldOf("Mb").forGetter(FluidBufferEntry::mb)
    ).apply(i, FluidBufferEntry::new));

    private TurbineValidation.Result cachedResult = TurbineValidation.Result.invalid();
    private long[] cachedPowerPortPositions = new long[0];
    private long[] cachedResourcePortPositions = new long[0];
    private long[] cachedRedstonePortPositions = new long[0];
    private boolean powered;
    private long lastRfPerTick;
    private double lastSteamPerTick;
    private double lastCoilEff;
    private double lastBladeEff;
    @Nullable
    private Player lastInteractingPlayer;

    private final List<FluidBufferEntry> steamInputEntries = new ArrayList<>();
    private final List<FluidBufferEntry> outputReturnEntries = new ArrayList<>();
    private int cachedSteamConsumeMbPerTick = 1;
    private boolean cachedOutputReturnBuffer;

    public TurbineControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_CONTROLLER_BE.get(), pos, state);
    }

    public TurbineValidation.Result getCachedResult() {
        return cachedResult != null ? cachedResult : TurbineValidation.Result.invalid();
    }

    public boolean isPowered() { return powered; }
    public long getLastRfPerTick() { return lastRfPerTick; }
    public double getLastSteamPerTick() { return lastSteamPerTick; }
    public double getLastCoilEff() { return lastCoilEff; }
    public double getLastBladeEff() { return lastBladeEff; }

    public void setLastInteractingPlayer(@Nullable Player player) {
        this.lastInteractingPlayer = player;
    }

    public void setCachedResult(TurbineValidation.Result result) {
        this.cachedResult = result != null ? result : TurbineValidation.Result.invalid();
        setChanged();
    }

    public int getCachedSteamConsumeMbPerTick() {
        return Math.max(1, cachedSteamConsumeMbPerTick);
    }

    public int getSteamInputCapacityMb() {
        return getCachedSteamConsumeMbPerTick();
    }

    public int getOutputReturnCapacityMb() {
        return cachedOutputReturnBuffer ? getCachedSteamConsumeMbPerTick() : 0;
    }

    public int getTotalSteamInputMb() {
        return totalMbInList(steamInputEntries);
    }

    public int getTotalOutputReturnMb() {
        return totalMbInList(outputReturnEntries);
    }

    public int addSteamInput(Fluid fluid, int amountMb) {
        return addFluidToList(steamInputEntries, getSteamInputCapacityMb(), fluid, amountMb);
    }

    public int consumeSteamInput(Fluid fluid, int amountMb) {
        return consumeFluidFromList(steamInputEntries, fluid, amountMb);
    }

    public int addOutputReturn(Fluid fluid, int amountMb) {
        if (!cachedOutputReturnBuffer) return 0;
        return addFluidToList(outputReturnEntries, getOutputReturnCapacityMb(), fluid, amountMb);
    }

    public int consumeOutputReturn(Fluid fluid, int amountMb) {
        return consumeFluidFromList(outputReturnEntries, fluid, amountMb);
    }

    public List<FluidBufferEntry> getOutputReturnEntries() {
        return List.copyOf(outputReturnEntries);
    }

    public void updateFluidBufferCapacities(ServerLevel level, TurbineValidation.Result result) {
        if (result == null || !result.valid()) {
            cachedSteamConsumeMbPerTick = 1;
            cachedOutputReturnBuffer = false;
            return;
        }
        cachedSteamConsumeMbPerTick = Math.max(1, (int) Math.ceil(result.maxSteamMbPerTick()));
        var gen = TurbineGenerationLoader.getDefault();
        Fluid outputFluid = TurbineGenerationLoader.getOutputFluid(gen, level.registryAccess());
        cachedOutputReturnBuffer = outputFluid != null && outputFluid != Fluids.EMPTY;
        clampFluidBuffers();
    }

    private void clampFluidBuffers() {
        clampFluidListToCapacity(steamInputEntries, getSteamInputCapacityMb());
        clampFluidListToCapacity(outputReturnEntries, getOutputReturnCapacityMb());
    }

    private static int totalMbInList(List<FluidBufferEntry> list) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, list.stream().mapToLong(FluidBufferEntry::mb).sum()));
    }

    private int addFluidToList(List<FluidBufferEntry> list, int maxTotalMb, Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY || maxTotalMb <= 0) return 0;
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id == null) return 0;
        int total = totalMbInList(list);
        int add = Math.min(amountMb, Math.max(0, maxTotalMb - total));
        if (add <= 0) return 0;
        for (int i = 0; i < list.size(); i++) {
            FluidBufferEntry e = list.get(i);
            if (id.equals(e.fluidId())) {
                list.set(i, new FluidBufferEntry(id, e.mb() + add));
                setChanged();
                return add;
            }
        }
        list.add(new FluidBufferEntry(id, add));
        setChanged();
        return add;
    }

    private int consumeFluidFromList(List<FluidBufferEntry> list, Fluid fluid, int amountMb) {
        if (amountMb <= 0 || fluid == null || fluid == Fluids.EMPTY) return 0;
        Identifier id = BuiltInRegistries.FLUID.getKey(fluid);
        if (id == null) return 0;
        int remaining = amountMb;
        int consumed = 0;
        for (int i = 0; i < list.size() && remaining > 0; i++) {
            FluidBufferEntry e = list.get(i);
            if (!id.equals(e.fluidId())) continue;
            int take = Math.min(remaining, Math.max(0, e.mb()));
            if (take <= 0) continue;
            int left = e.mb() - take;
            consumed += take;
            remaining -= take;
            if (left <= 0) {
                list.remove(i);
                i--;
            } else {
                list.set(i, new FluidBufferEntry(id, left));
            }
        }
        if (consumed > 0) setChanged();
        return consumed;
    }

    private void clampFluidListToCapacity(List<FluidBufferEntry> list, int maxTotalMb) {
        int excess = totalMbInList(list) - maxTotalMb;
        if (excess <= 0) return;
        for (int i = list.size() - 1; i >= 0 && excess > 0; i--) {
            FluidBufferEntry e = list.get(i);
            int take = Math.min(e.mb(), excess);
            int left = e.mb() - take;
            excess -= take;
            if (left <= 0) list.remove(i);
            else list.set(i, new FluidBufferEntry(e.fluidId(), left));
        }
        setChanged();
    }

    /** Called from block tick when VALIDATING -> ON/OFF; shows message in action bar. */
    public void notifyValidationResult() {
        if (lastInteractingPlayer instanceof ServerPlayer sp) {
            TurbineValidation.Result result = getCachedResult();
            sp.sendSystemMessage(TurbineValidation.failureMessage(result), true);
            if (level != null) {
                TurbineValidation.sendFailureMarkers(sp, level, result);
            }
        }
        lastInteractingPlayer = null;
    }

    public long[] getCachedPowerPortPositions() {
        return cachedPowerPortPositions;
    }

    public long[] getCachedResourcePortPositions() {
        return cachedResourcePortPositions;
    }

    public long[] getCachedRedstonePortPositions() {
        return cachedRedstonePortPositions;
    }

    public void setRuntimeStats(long rfPerTick, double steamPerTick, boolean powered) {
        this.lastRfPerTick = rfPerTick;
        this.lastSteamPerTick = steamPerTick;
        this.powered = powered;
        setChanged();
    }

    public void rebuildPartCaches(ServerLevel level, TurbineValidation.Result result) {
        if (level == null || result == null || !result.valid()) {
            cachedPowerPortPositions = new long[0];
            cachedResourcePortPositions = new long[0];
            cachedRedstonePortPositions = new long[0];
            return;
        }
        int minX = result.minX();
        int minY = result.minY();
        int minZ = result.minZ();
        int maxX = result.maxX();
        int maxY = result.maxY();
        int maxZ = result.maxZ();

        LongArrayList powerPorts = new LongArrayList();
        LongArrayList resourcePorts = new LongArrayList();
        LongArrayList redstonePorts = new LongArrayList();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    p.set(x, y, z);
                    BlockState state = level.getBlockState(p);
                    if (TurbineValidation.isTurbinePowerPort(state)) {
                        powerPorts.add(p.asLong());
                    } else if (state.is(ModBlocks.TURBINE_RESOURCE_PORT.get())) {
                        resourcePorts.add(p.asLong());
                    } else if (state.is(ModBlocks.TURBINE_REDSTONE_PORT.get())) {
                        redstonePorts.add(p.asLong());
                    }
                }
            }
        }
        cachedPowerPortPositions = powerPorts.toLongArray();
        cachedResourcePortPositions = resourcePorts.toLongArray();
        cachedRedstonePortPositions = redstonePorts.toLongArray();
    }

    public void tickSimulation(ServerLevel level) {
        if (!cachedResult.valid()) {
            setRuntimeStats(0, 0, false);
            return;
        }
        updateFluidBufferCapacities(level, cachedResult);
        TurbineFiller.tickFill(level, this);
        TurbineSimulation.tick(level, this);
        lastCoilEff = cachedResult.coilEfficiency();
        lastBladeEff = cachedResult.bladeEfficiency();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.colossal_reactors.turbine_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new TurbineControllerMenu(id, inv, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Powered", powered);
        output.putLong("LastRf", lastRfPerTick);
        output.putDouble("LastSteam", lastSteamPerTick);
        output.putInt("LastCoilEffMilli", (int) (lastCoilEff * 1000));
        output.putInt("LastBladeEffMilli", (int) (lastBladeEff * 1000));
        if (cachedResult.valid()) {
            output.putBoolean("val_valid", true);
            output.putInt("val_minX", cachedResult.minX());
            output.putInt("val_minY", cachedResult.minY());
            output.putInt("val_minZ", cachedResult.minZ());
            output.putInt("val_maxX", cachedResult.maxX());
            output.putInt("val_maxY", cachedResult.maxY());
            output.putInt("val_maxZ", cachedResult.maxZ());
            output.putInt("val_bladeCount", cachedResult.bladeCount());
            output.putInt("val_validBladeCount", cachedResult.validBladeCount());
            output.putInt("val_coilBlockCount", cachedResult.coilBlockCount());
            output.putInt("val_coilEffMilli", (int) (cachedResult.coilEfficiency() * 1000));
            output.putInt("val_bladeEffMilli", (int) (cachedResult.bladeEfficiency() * 1000));
            output.putDouble("val_steamCap", cachedResult.maxSteamMbPerTick());
            output.putDouble("val_rfEst", cachedResult.estimatedRfPerTick());
        }
        output.putInt("SteamConsumeMb", cachedSteamConsumeMbPerTick);
        output.putBoolean("OutputReturnBuf", cachedOutputReturnBuffer);
        if (!steamInputEntries.isEmpty()) {
            output.store(TAG_STEAM_BUFFER, Codec.list(FLUID_BUFFER_CODEC), List.copyOf(steamInputEntries));
        }
        if (!outputReturnEntries.isEmpty()) {
            output.store(TAG_OUTPUT_BUFFER, Codec.list(FLUID_BUFFER_CODEC), List.copyOf(outputReturnEntries));
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        powered = input.getBooleanOr("Powered", false);
        lastRfPerTick = input.getLongOr("LastRf", 0L);
        lastSteamPerTick = input.getDoubleOr("LastSteam", 0.0);
        lastCoilEff = input.getIntOr("LastCoilEffMilli", 0) / 1000.0;
        lastBladeEff = input.getIntOr("LastBladeEffMilli", 0) / 1000.0;
        Optional<Integer> minX = input.getInt("val_minX");
        if (minX.isPresent()) {
            cachedResult = new TurbineValidation.Result(
                    true,
                    null,
                    null,
                    TurbineValidation.ValidationReport.empty(),
                    minX.get(),
                    input.getIntOr("val_minY", 0),
                    input.getIntOr("val_minZ", 0),
                    input.getIntOr("val_maxX", 0),
                    input.getIntOr("val_maxY", 0),
                    input.getIntOr("val_maxZ", 0),
                    input.getIntOr("val_bladeCount", 0),
                    input.getIntOr("val_validBladeCount", 0),
                    input.getIntOr("val_coilBlockCount", 0),
                    input.getIntOr("val_coilEffMilli", 0) / 1000.0,
                    input.getIntOr("val_bladeEffMilli", 0) / 1000.0,
                    input.getDoubleOr("val_steamCap", 0.0),
                    input.getDoubleOr("val_rfEst", 0.0));
        } else {
            cachedResult = TurbineValidation.Result.invalid();
        }
        cachedSteamConsumeMbPerTick = Math.max(1, input.getIntOr("SteamConsumeMb", 1));
        cachedOutputReturnBuffer = input.getBooleanOr("OutputReturnBuf", false);
        steamInputEntries.clear();
        for (FluidBufferEntry e : input.listOrEmpty(TAG_STEAM_BUFFER, FLUID_BUFFER_CODEC)) {
            if (e != null && e.fluidId() != null && e.mb() > 0) steamInputEntries.add(e);
        }
        outputReturnEntries.clear();
        for (FluidBufferEntry e : input.listOrEmpty(TAG_OUTPUT_BUFFER, FLUID_BUFFER_CODEC)) {
            if (e != null && e.fluidId() != null && e.mb() > 0) outputReturnEntries.add(e);
        }
        if (level instanceof ServerLevel sl && cachedResult.valid()) {
            updateFluidBufferCapacities(sl, cachedResult);
        } else {
            clampFluidBuffers();
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level == null || level.isClientSide()) {
            return;
        }
        BlockState state = getBlockState();
        if (!state.is(ModBlocks.TURBINE_CONTROLLER.get())) {
            return;
        }
        if (state.getValue(TurbineControllerBlock.VISUAL) == TurbineVisualState.ON && !getCachedResult().valid()) {
            level.scheduleTick(worldPosition, state.getBlock(), 1);
        }
    }

    /** Refreshes validation when the multiblock cache was lost (e.g. chunk reload). */
    public TurbineValidation.Result refreshValidationCache(ServerLevel level, Direction intoTurbine) {
        BlockPos start = worldPosition.relative(intoTurbine);
        TurbineValidation.Result result = TurbineValidation.validateWithRodAlignment(level, start, intoTurbine, -1);
        setCachedResult(result);
        return result;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
