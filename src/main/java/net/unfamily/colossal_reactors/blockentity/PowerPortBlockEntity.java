package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.LimitingEnergyHandler;
import net.unfamily.colossal_reactors.transfer.FluxNetworksLongEnergyBridge;
import net.unfamily.colossal_reactors.transfer.LongBackedEnergyHandler;

/**
 * BlockEntity for Power Port. Large energy buffer; reactor pushes in via {@link #receiveEnergyFromReactor(long)}.
 * Each tick the port pushes energy out to adjacent blocks that can receive (cables, machines).
 * Capacity and max extract per tick are read from Config (ports.power). Storage uses {@code long} (per NeoForge
 * {@link EnergyHandler} path still moves at most {@code Integer.MAX_VALUE} per {@code insert}/{@code extract}).
 * When Flux Networks is loaded, neighbors exposing {@code IFNEnergyStorage} are tried first with {@code long} transfers.
 */
public class PowerPortBlockEntity extends BlockEntity {

    private final long maxExtractPerTick;

    private final LongBackedEnergyHandler core;
    /** External automation: insert blocked; extract allowed (same behavior as legacy output-only wrappers). */
    private final EnergyHandler capabilityView;

    public PowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.POWER_PORT_BE.get(), pos, state);
        long capacity = net.unfamily.colossal_reactors.Config.POWER_PORT_CAPACITY.getAsLong();
        long maxExtractCfg = net.unfamily.colossal_reactors.Config.POWER_PORT_MAX_EXTRACT.getAsLong();
        this.maxExtractPerTick = Math.min(capacity, maxExtractCfg);
        this.core = new LongBackedEnergyHandler(capacity, 0L, capacity, 0L, this::setChanged);
        this.capabilityView = new LimitingEnergyHandler(core, 0, Integer.MAX_VALUE);
    }

    /**
     * Server tick: push energy to adjacent blocks that can receive (cables, machines).
     */
    public void tick() {
        if (level == null || level.isClientSide()) return;
        long budget = Math.min(maxExtractPerTick, core.getAmountAsLong());
        if (budget <= 0) return;
        for (Direction direction : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighborPos = worldPosition.relative(direction);
            Direction intoNeighbor = direction.getOpposite();
            long stored = core.getAmountAsLong();
            long offer = Math.min(budget, stored);
            if (offer <= 0) continue;

            long fluxMoved = FluxNetworksLongEnergyBridge.tryReceiveEnergyLong(level, neighborPos, intoNeighbor, offer);
            if (fluxMoved > 0) {
                core.extractEnergyLong(fluxMoved);
                budget -= fluxMoved;
                setChanged();
                continue;
            }

            EnergyHandler neighbor = level.getCapability(Capabilities.Energy.BLOCK, neighborPos, intoNeighbor);
            if (neighbor == null) continue;
            int chunk = (int) Math.min(offer, Integer.MAX_VALUE);
            int moved = EnergyHandlerUtil.move(core, neighbor, chunk, null);
            if (moved > 0) {
                budget -= moved;
                setChanged();
            }
        }
    }

    /** Internal storage; prefer {@link #getEnergyCore()} for RF-style reads. */
    public EnergyHandler getEnergyStorage() {
        return core;
    }

    public LongBackedEnergyHandler getEnergyCore() {
        return core;
    }

    /** Output-only view for capability: cables extract; reactor uses {@link #receiveEnergyFromReactor(long)}. */
    public EnergyHandler getEnergyHandlerForCapability() {
        return capabilityView;
    }

    /**
     * Called by the reactor controller to push produced energy into this port.
     *
     * @param maxAmount max RF to accept this call
     * @return amount actually accepted
     */
    public long receiveEnergyFromReactor(long maxAmount) {
        if (maxAmount <= 0) return 0;
        return core.addEnergy(maxAmount);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        core.serialize(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        core.deserialize(input);
    }
}
