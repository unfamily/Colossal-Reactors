package net.unfamily.colossal_reactors.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandlerUtil;
import net.neoforged.neoforge.transfer.energy.LimitingEnergyHandler;
import net.unfamily.colossal_reactors.Config;
import net.unfamily.colossal_reactors.integration.brandonscore.BrandonScoreIntegration;
import net.unfamily.colossal_reactors.transfer.FluxNetworksLongEnergyBridge;
import net.unfamily.colossal_reactors.transfer.LongBackedEnergyHandler;

public class TurbineHighCondPowerPortBlockEntity extends BlockEntity implements TurbinePowerPort {

    private final long maxExtractPerTick;
    private final LongBackedEnergyHandler core;
    private final EnergyHandler capabilityView;
    private Object opOutput;

    public TurbineHighCondPowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_HIGH_COND_POWER_PORT_BE.get(), pos, state);
        long capacity = Config.TURBINE_HIGH_COND_POWER_PORT_CAPACITY.getAsLong();
        long maxExtractCfg = Config.TURBINE_HIGH_COND_POWER_PORT_MAX_EXTRACT.getAsLong();
        this.maxExtractPerTick = Math.min(capacity, maxExtractCfg);
        this.core = new LongBackedEnergyHandler(capacity, 0L, capacity, 0L, this::setChanged);
        this.capabilityView = new LimitingEnergyHandler(core, 0, Integer.MAX_VALUE);
    }

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

            long opMoved = BrandonScoreIntegration.tryPushToNeighbor(level, neighborPos, intoNeighbor, offer);
            if (opMoved > 0) {
                core.extractEnergyLong(opMoved);
                budget -= opMoved;
                setChanged();
                continue;
            }

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

    /** OP capability when Brandon's Core is loaded. */
    public Object getOpStorageForCapability() {
        if (!ModList.get().isLoaded("brandonscore")) {
            return null;
        }
        if (opOutput == null) {
            opOutput = BrandonScoreIntegration.createOpStorage(core, maxExtractPerTick);
        }
        return opOutput;
    }

    @Override
    public long receiveEnergyFromTurbine(long maxAmount) {
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
