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
import net.unfamily.colossal_reactors.integration.brandonscore.BrandonScoreIntegration;
import net.unfamily.colossal_reactors.multiblock.PortCapacityPolicy;
import net.unfamily.colossal_reactors.multiblock.PortScalingConstants;
import net.unfamily.colossal_reactors.transfer.FluxNetworksLongEnergyBridge;
import net.unfamily.colossal_reactors.transfer.LongBackedEnergyHandler;

/**
 * High-conduction power port: {@code long} buffer and transfer rates.
 * Pushes to native OP when Brandon's Core is present, otherwise Flux long API, then standard energy API.
 */
public class HighCondPowerPortBlockEntity extends BlockEntity implements ReactorPowerPort {

    private long maxExtractPerTick;
    private LongBackedEnergyHandler core;
    private EnergyHandler capabilityView;
    private Object opOutput;

    public HighCondPowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.HIGH_COND_POWER_PORT_BE.get(), pos, state);
        applyEnergyCapacity(PortScalingConstants.MIN_ENERGY_BUFFER_RF);
    }

    public void applyEnergyCapacity(long targetCapacity) {
        long cap = Math.min(PortScalingConstants.LONG_ENERGY_CAP,
                Math.max(PortScalingConstants.MIN_ENERGY_BUFFER_RF, targetCapacity));
        if (core != null) {
            cap = PortCapacityPolicy.resolveEnergyCapacity(cap, core.getCapacityAsLong(), core.getAmountAsLong());
        }
        if (core != null && core.getCapacityAsLong() == cap && maxExtractPerTick == cap) {
            return;
        }
        maxExtractPerTick = cap;
        if (core == null) {
            core = new LongBackedEnergyHandler(cap, 0L, cap, 0L, this::setChanged);
            capabilityView = new LimitingEnergyHandler(core, 0, Integer.MAX_VALUE);
        } else {
            core.resize(cap, 0L, cap);
        }
        setChanged();
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        long budget = Math.min(maxExtractPerTick, core.getAmountAsLong());
        if (budget <= 0) return;

        if (BrandonScoreIntegration.isBrandonScoreLoaded()) {
            Object op = getOpStorageForCapability();
            if (op == null) {
                return;
            }
            for (Direction direction : Direction.values()) {
                if (budget <= 0) break;
                BlockPos neighborPos = worldPosition.relative(direction);
                Direction intoNeighbor = direction.getOpposite();
                long offer = Math.min(budget, core.getAmountAsLong());
                if (offer <= 0) continue;
                long moved = BrandonScoreIntegration.tryPushOpToNeighbor(level, neighborPos, intoNeighbor, op, offer);
                if (moved > 0) {
                    budget -= moved;
                    setChanged();
                }
            }
            return;
        }

        for (Direction direction : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighborPos = worldPosition.relative(direction);
            Direction intoNeighbor = direction.getOpposite();
            long offer = Math.min(budget, core.getAmountAsLong());
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

    public EnergyHandler getEnergyHandlerForCapability() {
        return capabilityView;
    }

    public LongBackedEnergyHandler getEnergyCore() {
        return core;
    }

    /** OP capability when Brandon's Core is loaded. */
    public Object getOpStorageForCapability() {
        if (!BrandonScoreIntegration.isBrandonScoreLoaded()) {
            return null;
        }
        if (opOutput == null) {
            opOutput = BrandonScoreIntegration.createOpStorage(core);
        }
        return opOutput;
    }

    @Override
    public long getStoredEnergyLong() {
        return core.getAmountAsLong();
    }

    @Override
    public long getMaxEnergyLong() {
        return core.getCapacityAsLong();
    }

    @Override
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
