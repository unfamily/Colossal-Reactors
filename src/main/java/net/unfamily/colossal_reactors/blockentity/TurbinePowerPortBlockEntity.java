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
import net.unfamily.colossal_reactors.multiblock.PortCapacityPolicy;
import net.unfamily.colossal_reactors.multiblock.PortScalingConstants;
import net.unfamily.colossal_reactors.transfer.IntBackedEnergyHandler;

public class TurbinePowerPortBlockEntity extends BlockEntity implements TurbinePowerPort {

    private int maxExtractPerTick;
    private IntBackedEnergyHandler core;
    private EnergyHandler capabilityView;

    public TurbinePowerPortBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURBINE_POWER_PORT_BE.get(), pos, state);
        applyEnergyCapacity((int) PortScalingConstants.MIN_ENERGY_BUFFER_RF);
    }

    public void applyEnergyCapacity(int targetCapacity) {
        int cap = (int) Math.min(PortScalingConstants.INT_ENERGY_CAP,
                Math.max(PortScalingConstants.MIN_ENERGY_BUFFER_RF, targetCapacity));
        if (core != null) {
            cap = (int) PortCapacityPolicy.resolveEnergyCapacity(cap, core.getCapacity(), core.getEnergyStored());
        }
        if (core != null && core.getCapacity() == cap && maxExtractPerTick == cap) {
            return;
        }
        maxExtractPerTick = cap;
        if (core == null) {
            core = new IntBackedEnergyHandler(cap, 0, cap, 0, this::setChanged);
            capabilityView = new LimitingEnergyHandler(core, 0, Integer.MAX_VALUE);
        } else {
            core.resize(cap, 0, cap);
        }
        setChanged();
    }

    public void tick() {
        if (level == null || level.isClientSide()) return;
        int budget = Math.min(maxExtractPerTick, core.getEnergyStored());
        if (budget <= 0) return;
        for (Direction direction : Direction.values()) {
            if (budget <= 0) break;
            BlockPos neighborPos = worldPosition.relative(direction);
            Direction intoNeighbor = direction.getOpposite();
            EnergyHandler neighbor = level.getCapability(Capabilities.Energy.BLOCK, neighborPos, intoNeighbor);
            if (neighbor == null) continue;
            int moved = EnergyHandlerUtil.move(core, neighbor, budget, null);
            if (moved > 0) {
                budget -= moved;
                setChanged();
            }
        }
    }

    public EnergyHandler getEnergyHandlerForCapability() {
        return capabilityView;
    }

    @Override
    public long getStoredEnergyLong() {
        return core.getEnergyStored();
    }

    @Override
    public long getMaxEnergyLong() {
        return core.getCapacity();
    }

    @Override
    public long receiveEnergyFromTurbine(long maxAmount) {
        return core.addEnergyFromReactor(maxAmount);
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
