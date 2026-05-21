package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.unfamily.colossal_reactors.block.ModBlocks;
import net.unfamily.colossal_reactors.block.TurbineControllerBlock;
import net.unfamily.colossal_reactors.block.TurbineVisualState;
import net.unfamily.colossal_reactors.menu.TurbineControllerMenu;
import net.unfamily.colossal_reactors.turbine.TurbineSimulation;
import net.unfamily.colossal_reactors.turbine.TurbineValidation;
import org.jetbrains.annotations.Nullable;

/**
 * Turbine controller: caches validation and runtime stats for GUI.
 */
public class TurbineControllerBlockEntity extends BlockEntity implements MenuProvider {

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

    /** Called from block tick when VALIDATING -> ON/OFF; shows message in action bar. */
    public void notifyValidationResult() {
        if (lastInteractingPlayer instanceof ServerPlayer sp) {
            TurbineValidation.Result result = getCachedResult();
            sp.displayClientMessage(TurbineValidation.failureMessage(result), true);
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
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Powered", powered);
        tag.putLong("LastRf", lastRfPerTick);
        tag.putDouble("LastSteam", lastSteamPerTick);
        tag.putInt("LastCoilEffMilli", (int) (lastCoilEff * 1000));
        tag.putInt("LastBladeEffMilli", (int) (lastBladeEff * 1000));
        if (cachedResult.valid()) {
            tag.putBoolean("val_valid", true);
            tag.putInt("val_minX", cachedResult.minX());
            tag.putInt("val_minY", cachedResult.minY());
            tag.putInt("val_minZ", cachedResult.minZ());
            tag.putInt("val_maxX", cachedResult.maxX());
            tag.putInt("val_maxY", cachedResult.maxY());
            tag.putInt("val_maxZ", cachedResult.maxZ());
            tag.putInt("val_bladeCount", cachedResult.bladeCount());
            tag.putInt("val_validBladeCount", cachedResult.validBladeCount());
            tag.putInt("val_coilBlockCount", cachedResult.coilBlockCount());
            tag.putInt("val_coilEffMilli", (int) (cachedResult.coilEfficiency() * 1000));
            tag.putInt("val_bladeEffMilli", (int) (cachedResult.bladeEfficiency() * 1000));
            tag.putDouble("val_steamCap", cachedResult.maxSteamMbPerTick());
            tag.putDouble("val_rfEst", cachedResult.estimatedRfPerTick());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        powered = tag.getBoolean("Powered");
        lastRfPerTick = tag.getLong("LastRf");
        lastSteamPerTick = tag.getDouble("LastSteam");
        if (tag.contains("LastCoilEffMilli")) {
            lastCoilEff = tag.getInt("LastCoilEffMilli") / 1000.0;
        }
        if (tag.contains("LastBladeEffMilli")) {
            lastBladeEff = tag.getInt("LastBladeEffMilli") / 1000.0;
        }
        if (tag.getBoolean("val_valid")) {
            cachedResult = new TurbineValidation.Result(
                    true,
                    null,
                    null,
                    TurbineValidation.ValidationReport.empty(),
                    tag.getInt("val_minX"),
                    tag.getInt("val_minY"),
                    tag.getInt("val_minZ"),
                    tag.getInt("val_maxX"),
                    tag.getInt("val_maxY"),
                    tag.getInt("val_maxZ"),
                    tag.getInt("val_bladeCount"),
                    tag.getInt("val_validBladeCount"),
                    tag.getInt("val_coilBlockCount"),
                    tag.getInt("val_coilEffMilli") / 1000.0,
                    tag.getInt("val_bladeEffMilli") / 1000.0,
                    tag.getDouble("val_steamCap"),
                    tag.getDouble("val_rfEst"));
        } else {
            cachedResult = TurbineValidation.Result.invalid();
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

    /** Refreshes validation when the multiblock cache was lost (e.g. chunk reload). Returns the new result. */
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

    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }
}
